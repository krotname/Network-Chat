package dev.krotname.networkchat.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ChatServerConfigTest {

  @Test
  void defaultAndPortSpecificConfigAreValid() {
    ChatServerConfig defaults = ChatServerConfig.defaultConfig();
    ChatServerConfig configuredPort = ChatServerConfig.ofPort(0);

    assertEquals(ChatServerConfig.DEFAULT_PORT, defaults.port());
    assertEquals(ChatServerConfig.DEFAULT_MAX_CLIENTS, defaults.maxClients());
    assertEquals(0, configuredPort.port());
    assertEquals(ChatServerConfig.DEFAULT_MAX_CLIENTS, configuredPort.maxClients());
  }

  @Test
  void convertsSocketTimeoutsForServerUse() {
    ChatServerConfig disabledTimeouts = new ChatServerConfig(1500, 1, Duration.ZERO, Duration.ZERO);
    ChatServerConfig roundedTimeouts =
        new ChatServerConfig(1500, 1, Duration.ofNanos(1), Duration.ofNanos(1));

    assertEquals(0, disabledTimeouts.handshakeTimeoutMillis());
    assertEquals(0, disabledTimeouts.readTimeoutMillis());
    assertEquals(1, roundedTimeouts.handshakeTimeoutMillis());
    assertEquals(1, roundedTimeouts.readTimeoutMillis());
  }

  @Test
  void rejectsInvalidLimitsAndTimeouts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ChatServerConfig(-1, 1, Duration.ZERO, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ChatServerConfig(65_536, 1, Duration.ZERO, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ChatServerConfig(1500, 0, Duration.ZERO, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ChatServerConfig(1500, 1, Duration.ofMillis(-1), Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ChatServerConfig(1500, 1, Duration.ZERO, Duration.ofMillis(-1)));
    assertThrows(
        NullPointerException.class, () -> new ChatServerConfig(1500, 1, null, Duration.ZERO));
    assertThrows(
        NullPointerException.class, () -> new ChatServerConfig(1500, 1, Duration.ZERO, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ChatServerConfig(
                1500, 1, Duration.ofMillis((long) Integer.MAX_VALUE + 1L), Duration.ZERO));
  }

  @Test
  void legacyServerConstructorKeepsConfigAccessible() {
    ChatServer server = new ChatServer(0);

    assertEquals(0, server.getPort());
    assertEquals(0, server.getConfig().port());
    assertFalse(server.isRunning());
    server.close();
  }
}
