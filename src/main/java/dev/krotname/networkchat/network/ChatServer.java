package dev.krotname.networkchat.network;

import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.MessageType;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Chat server that authenticates users, keeps sessions, and broadcasts messages. */
public final class ChatServer implements AutoCloseable {
  private static final Logger LOG = Logger.getLogger(ChatServer.class.getName());
  private static final int DEFAULT_PORT = 1500;
  private static final int MIN_USER_NAME_LENGTH = 3;
  private static final int MAX_USER_NAME_LENGTH = 64;

  private final int port;
  private final Map<String, ChatConnection> sessions = new ConcurrentHashMap<>();
  private final Object sessionsMonitor = new Object();
  private final AtomicInteger activeClients = new AtomicInteger();
  private final ExecutorService clientExecutor =
      Executors.newCachedThreadPool(
          r -> {
            Thread t = new Thread(r, "chat-client-handler");
            t.setDaemon(true);
            return t;
          });
  private final ExecutorService acceptorExecutor =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "chat-acceptor");
            t.setDaemon(true);
            return t;
          });
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final CountDownLatch startSignal = new CountDownLatch(1);
  private volatile ServerSocket serverSocket;

  public ChatServer(int port) {
    this.port = port;
  }

  public static void main(String[] args) throws Exception {
    int port = parsePort(args);
    try (ChatServer server = new ChatServer(port)) {
      server.start();
      server.awaitStarted();
      Thread.currentThread().join();
    }
  }

  private static int parsePort(String[] args) {
    if (args.length >= 2 && "--port".equalsIgnoreCase(args[0])) {
      return Integer.parseInt(args[1]);
    }
    if (args.length == 1) {
      return Integer.parseInt(args[0]);
    }
    return DEFAULT_PORT;
  }

  public void start() throws IOException {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    serverSocket = new ServerSocket(port);
    acceptorExecutor.execute(this::acceptLoop);
    startSignal.countDown();
  }

  public void awaitStarted() {
    try {
      startSignal.await();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void acceptLoop() {
    while (running.get()) {
      try {
        Socket socket = serverSocket.accept();
        clientExecutor.execute(() -> handleClient(socket));
      } catch (IOException ex) {
        if (running.get()) {
          LOG.log(Level.WARNING, "Accept failed", ex);
        }
      }
    }
  }

  /**
   * Processes a single client socket from acceptance to logout and ensures active-client metrics
   * are always accurate through finally block cleanup.
   */
  private void handleClient(Socket socket) {
    String userName = null;
    boolean active = false;
    try (socket;
        ChatConnection connection = new ChatConnection(socket)) {
      userName = serverHandshake(connection);
      sendUsersListToNewClient(connection, userName);
      activeClients.incrementAndGet();
      active = true;
      broadcast(ChatMessage.withData(MessageType.USER_ADDED, userName, null), connection);
      serverMainLoop(connection, userName);
    } catch (IOException | RuntimeException ex) {
      LOG.log(Level.FINE, "Client session ended", ex);
    } finally {
      if (active) {
        activeClients.decrementAndGet();
      }
      if (userName != null) {
        removeSession(userName);
      }
    }
  }

  /**
   * Keep requesting the name until a valid and unique value is provided by the client.
   *
   * <p>Reservation occurs atomically to avoid the duplicate-name race while handshaking.
   */
  private String serverHandshake(ChatConnection connection) throws IOException {
    while (true) {
      connection.send(ChatMessage.withData(MessageType.NAME_REQUEST, null, null));
      ChatMessage request = connection.receive();
      if (request.type() != MessageType.USER_NAME
          || request.data() == null
          || request.data().isBlank()) {
        connection.send(ChatMessage.withData(MessageType.ERROR, "Expected USER_NAME frame", null));
        continue;
      }
      String candidate = request.data().trim();
      if (!isNameValid(candidate)) {
        connection.send(ChatMessage.withData(MessageType.ERROR, "Invalid user name", null));
        continue;
      }
      synchronized (sessionsMonitor) {
        if (sessions.containsKey(candidate)) {
          connection.send(
              ChatMessage.withData(MessageType.ERROR, "User name already in use", null));
          continue;
        }
        sessions.put(candidate, connection);
        connection.send(ChatMessage.withData(MessageType.NAME_ACCEPTED, null, null));
        return candidate;
      }
    }
  }

  private boolean isNameValid(String userName) {
    return userName.length() >= MIN_USER_NAME_LENGTH
        && userName.length() <= MAX_USER_NAME_LENGTH
        && userName.matches("[\\p{L}\\p{N}_-]+");
  }

  /** Sends all existing users to the new client so UI state is correct right after connect. */
  private void sendUsersListToNewClient(ChatConnection connection, String currentUser)
      throws IOException {
    for (String userName : sessions.keySet()) {
      if (currentUser.equals(userName)) {
        continue;
      }
      connection.send(ChatMessage.withData(MessageType.USER_ADDED, userName, null));
    }
  }

  /** Read messages from one client and broadcast normalized text messages to all peers. */
  private void serverMainLoop(ChatConnection connection, String userName) throws IOException {
    while (running.get()) {
      ChatMessage message = connection.receive();
      if (message.type() == MessageType.TEXT) {
        String text = message.data() == null ? "" : message.data();
        broadcast(ChatMessage.text(String.format("%s: %s", userName, text), userName), connection);
        continue;
      }
      if (message.type() == MessageType.NAME_ACCEPTED
          || message.type() == MessageType.USER_ADDED
          || message.type() == MessageType.USER_REMOVED
          || message.type() == MessageType.ERROR) {
        continue;
      }
      throw new IOException("Unsupported message type: " + message.type());
    }
  }

  /**
   * Broadcasts to every connected client, excluding the message source. Failed deliveries are
   * treated as dropped clients and cleaned up immediately.
   */
  private void broadcast(ChatMessage message, ChatConnection exceptConnection) throws IOException {
    for (Map.Entry<String, ChatConnection> entry : sessions.entrySet()) {
      if (entry.getValue() == exceptConnection) {
        continue;
      }
      try {
        ChatConnection recipient = entry.getValue();
        if (recipient != null) {
          recipient.send(message);
        }
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Failed sending to " + entry.getKey(), ex);
        removeSession(entry.getKey());
      }
    }
  }

  private void removeSession(String userName) {
    ChatConnection removed = sessions.remove(userName);
    if (removed == null) {
      return;
    }
    try {
      removed.close();
    } catch (IOException ex) {
      LOG.log(Level.FINE, "Error closing connection", ex);
    }
    try {
      broadcast(ChatMessage.withData(MessageType.USER_REMOVED, userName, null), null);
    } catch (IOException ex) {
      LOG.log(Level.FINE, "Unable to broadcast USER_REMOVED", ex);
    }
  }

  @Override
  public void close() {
    running.set(false);
    try {
      if (serverSocket != null) {
        serverSocket.close();
      }
    } catch (IOException ex) {
      LOG.log(Level.FINE, "Error while closing server socket", ex);
    }
    for (String userName : new HashSet<>(sessions.keySet())) {
      removeSession(userName);
    }
    clientExecutor.shutdownNow();
    acceptorExecutor.shutdownNow();
  }

  public Set<String> getConnectedUsers() {
    return Collections.unmodifiableSet(sessions.keySet());
  }

  public int getPort() {
    return port;
  }

  public boolean isRunning() {
    return running.get();
  }

  public int getActiveClients() {
    return activeClients.get();
  }
}
