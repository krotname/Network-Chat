package dev.krotname.networkchat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.krotname.networkchat.network.ChatConnection;
import dev.krotname.networkchat.network.ChatServer;
import dev.krotname.networkchat.network.ChatServerConfig;
import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.MessageType;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

class NetworkChatIntegrationTest {

  @Test
  void serverBroadcastsMessagesBetweenClients() throws Exception {
    int port = randomFreePort();
    try (ChatServer server = new ChatServer(port)) {
      server.start();
      server.awaitStarted();

      try (TestClient alice = new TestClient("alice", port);
          TestClient bob = new TestClient("bob", port)) {
        alice.connect();
        bob.connect();

        assertTrue(server.getConnectedUsers().contains("alice"));
        assertTrue(server.getConnectedUsers().contains("bob"));

        alice.sendText("Привет всем");
        awaitTextMessage(bob, "alice", "Привет всем");
      }
    }
  }

  @Test
  void serverRejectsDuplicateUserName() throws Exception {
    int port = randomFreePort();
    try (ChatServer server = new ChatServer(port)) {
      server.start();
      server.awaitStarted();

      try (TestClient alice = new TestClient("alice", port);
          ChatConnection duplicate = new ChatConnection(new Socket("127.0.0.1", port))) {
        alice.connect();
        consumeNameRequest(duplicate);
        duplicate.send(ChatMessage.withData(MessageType.USER_NAME, "alice", null));
        ChatMessage response;
        do {
          response = duplicate.receive();
        } while (response.type() != MessageType.ERROR
            && response.type() != MessageType.NAME_ACCEPTED);
        assertEquals(MessageType.ERROR, response.type());
      }
    }
  }

  @Test
  void serverRejectsInvalidUserName() throws Exception {
    int port = randomFreePort();
    try (ChatServer server = new ChatServer(port)) {
      server.start();
      server.awaitStarted();

      try (TestClient alice = new TestClient("alice", port);
          ChatConnection invalid = new ChatConnection(new Socket("127.0.0.1", port))) {
        alice.connect();
        consumeNameRequest(invalid);
        invalid.send(ChatMessage.withData(MessageType.USER_NAME, "!!", null));
        ChatMessage response;
        do {
          response = invalid.receive();
        } while (response.type() != MessageType.ERROR
            && response.type() != MessageType.NAME_ACCEPTED);
        assertEquals(MessageType.ERROR, response.type());
      }
    }
  }

  @Test
  void serverRemovesUserWhenConnectionCloses() throws Exception {
    int port = randomFreePort();
    try (ChatServer server = new ChatServer(port)) {
      server.start();
      server.awaitStarted();

      try (TestClient alice = new TestClient("alice", port);
          TestClient bob = new TestClient("bob", port)) {
        alice.connect();
        bob.connect();

        awaitProtocolEvent(alice, MessageType.USER_ADDED, "bob");

        bob.disconnect();
        awaitProtocolEvent(alice, MessageType.USER_REMOVED, "bob");
      }
    }
  }

  @Test
  void serverRejectsConnectionsOverConfiguredLimit() throws Exception {
    int port = randomFreePort();
    ChatServerConfig config =
        new ChatServerConfig(port, 1, Duration.ofSeconds(2), Duration.ofSeconds(5));
    try (ChatServer server = new ChatServer(config)) {
      server.start();
      server.awaitStarted();

      try (TestClient alice = new TestClient("alice", port)) {
        alice.connect();
        Socket rejectedSocket = new Socket("127.0.0.1", port);
        rejectedSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(2));
        try (ChatConnection rejected = new ChatConnection(rejectedSocket)) {
          ChatMessage response = rejected.receive();
          assertEquals(MessageType.ERROR, response.type());
          assertTrue(response.data().contains("busy"));
        }
      }
    }
  }

  @Test
  void serverClosesClientThatDoesNotCompleteHandshake() throws Exception {
    int port = randomFreePort();
    ChatServerConfig config =
        new ChatServerConfig(port, 2, Duration.ofMillis(200), Duration.ofSeconds(5));
    try (ChatServer server = new ChatServer(config)) {
      server.start();
      server.awaitStarted();

      Socket socket = new Socket("127.0.0.1", port);
      socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(2));
      try (ChatConnection silent = new ChatConnection(socket)) {
        assertEquals(MessageType.NAME_REQUEST, silent.receive().type());
        assertThrows(IOException.class, silent::receive);
      }
    }
  }

  private static void consumeNameRequest(ChatConnection connection) throws IOException {
    ChatMessage request;
    do {
      request = connection.receive();
    } while (request.type() != MessageType.NAME_REQUEST
        && request.type() != MessageType.NAME_ACCEPTED);
  }

  private static void awaitTextMessage(
      TestClient client, String expectedSender, String expectedData) {
    Awaitility.await("wait for broadcast")
        .pollInterval(Duration.ofMillis(200))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertTrue(containsTextMessage(client, expectedSender, expectedData)));
  }

  private static boolean containsTextMessage(
      TestClient client, String expectedSender, String expectedData) {
    ChatMessage message;
    while ((message = client.drainQueue()) != null) {
      if (MessageType.TEXT.equals(message.type())
          && expectedSender.equals(message.sender())
          && expectedData.equals(message.data())) {
        return true;
      }
    }
    return false;
  }

  private static void awaitProtocolEvent(
      TestClient client, MessageType expectedType, String expectedData) {
    Awaitility.await("wait for protocol event")
        .pollInterval(Duration.ofMillis(200))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertTrue(containsInQueue(client, expectedType, expectedData)));
  }

  private static boolean containsInQueue(
      TestClient client, MessageType expectedType, String expectedData) {
    ChatMessage message;
    while ((message = client.drainQueue()) != null) {
      if (message.type() == expectedType
          && message.data() != null
          && message.data().contains(expectedData)) {
        return true;
      }
    }
    return false;
  }

  private static int randomFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException ex) {
      return 1500;
    }
  }

  private static final class TestClient implements AutoCloseable {
    private final String userName;
    private final int port;
    private final LinkedBlockingQueue<ChatMessage> messages = new LinkedBlockingQueue<>();
    private ChatConnection connection;
    private Thread reader;

    private TestClient(String userName, int port) {
      this.userName = userName;
      this.port = port;
    }

    void connect() throws Exception {
      Socket socket = new Socket("127.0.0.1", port);
      connection = new ChatConnection(socket);
      ChatMessage request;
      do {
        request = connection.receive();
      } while (request.type() != MessageType.NAME_REQUEST);
      connection.send(ChatMessage.withData(MessageType.USER_NAME, userName, null));
      ChatMessage accepted = connection.receive();
      if (accepted.type() != MessageType.NAME_ACCEPTED) {
        throw new IllegalStateException("Expected NAME_ACCEPTED");
      }

      reader =
          new Thread(
              () -> {
                while (!Thread.currentThread().isInterrupted()) {
                  try {
                    messages.offer(connection.receive());
                  } catch (IOException ex) {
                    break;
                  }
                }
              });
      reader.setDaemon(true);
      reader.start();
    }

    void sendText(String text) throws Exception {
      connection.send(ChatMessage.text(text, userName));
    }

    ChatMessage drainQueue() {
      return messages.poll();
    }

    void disconnect() {
      close();
    }

    @Override
    public void close() {
      if (reader != null) {
        reader.interrupt();
      }
      try {
        if (connection != null) {
          connection.close();
        }
      } catch (IOException ignored) {
      }
      if (reader != null) {
        try {
          reader.join(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
