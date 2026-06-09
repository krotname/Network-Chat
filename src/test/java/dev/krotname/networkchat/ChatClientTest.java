package dev.krotname.networkchat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.krotname.networkchat.client.ChatClient;
import org.junit.jupiter.api.Test;

final class ChatClientTest {

  @Test
  void runSignalsConnectionWithoutConsolePath() {
    TestClient client = new TestClient();
    client.run();
    assertTrue(client.isConnected());
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
}
