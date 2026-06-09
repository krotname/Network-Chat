package dev.krotname.networkchat.client;

import dev.krotname.networkchat.network.ChatConnection;
import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.MessageType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/** Template client with reusable protocol loop and overridable UI/input behavior. */
public abstract class ChatClient {
  private static final Logger LOG = Logger.getLogger(ChatClient.class.getName());
  private static final String EXIT_COMMAND = "exit";
  private static final BufferedReader CONSOLE_READER =
      new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

  protected volatile boolean clientConnected;
  protected ChatConnection connection;
  private volatile String resolvedUserName;
  private final CountDownLatch connectionEstablishedLatch = new CountDownLatch(1);

  public abstract String getServerAddress() throws IOException;

  public abstract int getServerPort() throws IOException;

  public abstract String getUserName() throws IOException;

  protected abstract boolean shouldSendTextFromConsole();

  protected abstract SocketThread getSocketThread();

  /**
   * Coordinates one client session: resolve credentials, execute the socket thread, wait for the
   * handshake result, then optionally read local input.
   */
  public void run() {
    try {
      String requestedUserName = getUserName();
      if (requestedUserName == null || requestedUserName.isBlank()) {
        throw new IOException("User name is required");
      }
      resolvedUserName = requestedUserName.trim();
    } catch (IOException e) {
      LOG.warning("Unable to determine user name");
      return;
    }
    SocketThread socketThread = getSocketThread();
    socketThread.start();
    try {
      connectionEstablishedLatch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    if (!clientConnected) {
      return;
    }
    if (shouldSendTextFromConsole()) {
      while (clientConnected) {
        String text = readInputLine();
        if (text == null || EXIT_COMMAND.equalsIgnoreCase(text)) {
          break;
        }
        sendTextMessage(text);
      }
    }
    closeConnection();
  }

  protected String readInputLine() {
    try {
      String line = CONSOLE_READER.readLine();
      return line == null ? null : line.trim();
    } catch (Exception ex) {
      LOG.warning("Failed reading console line");
      return null;
    }
  }

  public void sendTextMessage(String text) {
    if (text == null || text.isBlank() || connection == null) {
      return;
    }
    try {
      connection.send(ChatMessage.text(text, resolvedUserName));
    } catch (IOException ex) {
      LOG.warning("Failed to send chat message");
      setConnectionStatus(false);
    }
  }

  private void closeConnection() {
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (IOException ex) {
      LOG.fine("Error closing client connection");
    }
    connection = null;
  }

  private void setConnectionStatus(boolean connected) {
    this.clientConnected = connected;
    if (connectionEstablishedLatch.getCount() > 0) {
      connectionEstablishedLatch.countDown();
    }
  }

  protected class SocketThread extends Thread {
    /**
     * Connection loop with explicit protocol handshake to enforce username registration before
     * entering the main message stream.
     */
    @Override
    public void run() {
      try {
        String serverAddress = getServerAddress();
        int serverPort = getServerPort();
        try (var socket = new Socket(serverAddress, serverPort)) {
          connection = new ChatConnection(socket);
          clientHandshake();
          clientMainLoop();
        }
      } catch (Exception ex) {
        LOG.warning("Connection error: " + ex.getMessage());
        notifyConnectionStatusChanged(false);
      }
    }

    /**
     * Default client handshake: wait for server NAME_REQUEST and respond with user name, failing
     * fast on protocol violations.
     */
    protected void clientHandshake() throws IOException {
      // Handshake follows the protocol contract:
      // server sends NAME_REQUEST, client answers with USER_NAME, server returns NAME_ACCEPTED.
      while (true) {
        ChatMessage message = connection.receive();
        if (message.type() == MessageType.NAME_REQUEST) {
          connection.send(ChatMessage.withData(MessageType.USER_NAME, resolvedUserName, null));
        } else if (message.type() == MessageType.NAME_ACCEPTED) {
          notifyConnectionStatusChanged(true);
          return;
        } else if (message.type() == MessageType.ERROR) {
          throw new IOException("Server error: " + message.data());
        } else {
          throw new IOException("Unexpected message type: " + message.type());
        }
      }
    }

    /**
     * Dispatches normalized protocol events to handlers. Unexpected frames are treated as protocol
     * errors and close the connection.
     */
    protected void clientMainLoop() throws IOException {
      while (true) {
        ChatMessage message = connection.receive();
        MessageType type = message.type();
        switch (type) {
          case USER_ADDED -> informAboutAddingNewUser(message.data());
          case USER_REMOVED -> informAboutDeletingNewUser(message.data());
          case TEXT -> processIncomingMessage(message.data());
          case ERROR -> LOG.warning("Server error: " + message.data());
          default -> throw new IOException("Unexpected message type: " + type);
        }
      }
    }

    protected void processIncomingMessage(String message) {
      System.out.println(message);
    }

    protected void informAboutAddingNewUser(String userName) {
      System.out.printf("Участник добавлен: %s%n", userName);
    }

    protected void informAboutDeletingNewUser(String userName) {
      System.out.printf("Участник вышел: %s%n", userName);
    }

    protected void notifyConnectionStatusChanged(boolean clientConnected) {
      setConnectionStatus(clientConnected);
    }
  }
}
