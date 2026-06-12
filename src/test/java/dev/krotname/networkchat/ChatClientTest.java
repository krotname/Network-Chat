package dev.krotname.networkchat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.krotname.networkchat.client.ChatClient;
import dev.krotname.networkchat.network.ChatConnection;
import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.MessageType;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

final class ChatClientTest {

  @Test
  void runSignalsConnectionWithoutConsolePath() {
    TestClient client = new TestClient();
    client.run();
    assertTrue(client.isConnected());
  }

  @Test
  void runResolvesConnectionSettingsBeforeUserName() {
    OrderedClient client = new OrderedClient();
    client.run();
    assertEquals(List.of("address", "port", "name"), client.calls());
  }

  @Test
  void consoleExitClosesReaderLoopWithoutConnectionError() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      Future<?> server = executor.submit(() -> acceptHandshakeAndWaitForClientClose(serverSocket));
      FastExitClient client = new FastExitClient(serverSocket.getLocalPort());

      client.run();

      Awaitility.await()
          .atMost(Duration.ofSeconds(2))
          .untilAsserted(() -> assertFalse(client.isConnected()));
      server.get(3, TimeUnit.SECONDS);
      assertNull(client.getLastConnectionError());
    } finally {
      executor.shutdownNow();
    }
  }

  private static void acceptHandshakeAndWaitForClientClose(ServerSocket serverSocket) {
    try (Socket socket = serverSocket.accept();
        ChatConnection connection = new ChatConnection(socket)) {
      connection.send(ChatMessage.withData(MessageType.NAME_REQUEST, null, null));
      assertEquals(MessageType.USER_NAME, connection.receive().type());
      connection.send(ChatMessage.withData(MessageType.NAME_ACCEPTED, null, null));
      try {
        connection.receive();
      } catch (IOException expected) {
        return;
      }
      throw new AssertionError("Expected client to close the socket");
    } catch (IOException ex) {
      throw new AssertionError(ex);
    }
  }

  private static final class TestClient extends ChatClient {
    private static final String serverAddress = "localhost";
    private static final int serverPort = 1500;
    private static final String userName = "tester";

    @Override
    public String getServerAddress() {
      return serverAddress;
    }

    @Override
    public int getServerPort() {
      return serverPort;
    }

    @Override
    public String getUserName() {
      return userName;
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
      return false;
    }

    @Override
    protected SocketThread getSocketThread() {
      return new SocketThread() {
        @Override
        public void run() {
          notifyConnectionStatusChanged(true);
        }
      };
    }

    boolean isConnected() {
      return clientConnected;
    }
  }

  private static final class OrderedClient extends ChatClient {
    private final List<String> calls = new ArrayList<>();

    @Override
    public String getServerAddress() {
      calls.add("address");
      return "localhost";
    }

    @Override
    public int getServerPort() {
      calls.add("port");
      return 1500;
    }

    @Override
    public String getUserName() {
      calls.add("name");
      return "tester";
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
      return false;
    }

    @Override
    protected SocketThread getSocketThread() {
      return new SocketThread() {
        @Override
        public void run() {
          notifyConnectionStatusChanged(true);
        }
      };
    }

    List<String> calls() {
      return calls;
    }
  }

  private static final class FastExitClient extends ChatClient {
    private final int port;

    private FastExitClient(int port) {
      this.port = port;
    }

    @Override
    public String getServerAddress() {
      return "127.0.0.1";
    }

    @Override
    public int getServerPort() {
      return port;
    }

    @Override
    public String getUserName() {
      return "fast_exit";
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
      return true;
    }

    @Override
    protected String readInputLine() {
      return "exit";
    }

    @Override
    protected SocketThread getSocketThread() {
      return new SocketThread() {};
    }

    boolean isConnected() {
      return clientConnected;
    }
  }
}
