package dev.krotname.networkchat.network;

import java.time.Duration;
import java.util.Objects;

/** Runtime limits and socket timeouts for {@link ChatServer}. */
public record ChatServerConfig(
    int port, int maxClients, Duration handshakeTimeout, Duration readTimeout) {
  public static final int DEFAULT_PORT = 1500;
  public static final int DEFAULT_MAX_CLIENTS = 100;
  public static final Duration DEFAULT_HANDSHAKE_TIMEOUT = Duration.ofSeconds(10);
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofMinutes(5);

  public ChatServerConfig {
    if (port < 0 || port > 65_535) {
      throw new IllegalArgumentException("Port must be in range 0..65535");
    }
    if (maxClients < 1) {
      throw new IllegalArgumentException("Max clients must be positive");
    }
    Objects.requireNonNull(handshakeTimeout, "handshakeTimeout");
    Objects.requireNonNull(readTimeout, "readTimeout");
    if (handshakeTimeout.isNegative()) {
      throw new IllegalArgumentException("Handshake timeout must not be negative");
    }
    if (readTimeout.isNegative()) {
      throw new IllegalArgumentException("Read timeout must not be negative");
    }
    validateSocketTimeout(handshakeTimeout, "handshakeTimeout");
    validateSocketTimeout(readTimeout, "readTimeout");
  }

  public static ChatServerConfig defaultConfig() {
    return new ChatServerConfig(
        DEFAULT_PORT, DEFAULT_MAX_CLIENTS, DEFAULT_HANDSHAKE_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  public static ChatServerConfig ofPort(int port) {
    return defaultConfig().withPort(port);
  }

  public ChatServerConfig withPort(int newPort) {
    return new ChatServerConfig(newPort, maxClients, handshakeTimeout, readTimeout);
  }

  int handshakeTimeoutMillis() {
    return toSocketTimeoutMillis(handshakeTimeout);
  }

  int readTimeoutMillis() {
    return toSocketTimeoutMillis(readTimeout);
  }

  private static void validateSocketTimeout(Duration timeout, String fieldName) {
    toSocketTimeoutMillis(timeout, fieldName);
  }

  private static int toSocketTimeoutMillis(Duration timeout) {
    return toSocketTimeoutMillis(timeout, "timeout");
  }

  private static int toSocketTimeoutMillis(Duration timeout, String fieldName) {
    if (timeout.isZero()) {
      return 0;
    }
    long millis = timeout.toMillis();
    if (millis <= 0) {
      return 1;
    }
    if (millis > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(fieldName + " is too large for socket timeout");
    }
    return (int) millis;
  }
}
