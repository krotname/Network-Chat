package dev.krotname.networkchat.network;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/** Runtime limits and socket timeouts for {@link ChatServer}. */
public record ChatServerConfig(
    int port,
    int maxClients,
    Duration handshakeTimeout,
    Duration readTimeout,
    Path historyFile,
    int historyLimit,
    int historyReplayLimit,
    Path accountFile,
    TlsServerConfig tls) {
  public static final int DEFAULT_PORT = 1500;
  public static final int DEFAULT_MAX_CLIENTS = 100;
  public static final Duration DEFAULT_HANDSHAKE_TIMEOUT = Duration.ofSeconds(10);
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofMinutes(5);
  public static final int DEFAULT_HISTORY_LIMIT = 10_000;
  public static final int DEFAULT_HISTORY_REPLAY_LIMIT = 50;

  public ChatServerConfig(
      int port, int maxClients, Duration handshakeTimeout, Duration readTimeout) {
    this(
        port,
        maxClients,
        handshakeTimeout,
        readTimeout,
        null,
        DEFAULT_HISTORY_LIMIT,
        DEFAULT_HISTORY_REPLAY_LIMIT,
        null,
        TlsServerConfig.disabled());
  }

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
    if (historyLimit < 1) {
      throw new IllegalArgumentException("History limit must be positive");
    }
    if (historyReplayLimit < 0) {
      throw new IllegalArgumentException("History replay limit must not be negative");
    }
    Objects.requireNonNull(tls, "tls");
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
    return new ChatServerConfig(
        newPort,
        maxClients,
        handshakeTimeout,
        readTimeout,
        historyFile,
        historyLimit,
        historyReplayLimit,
        accountFile,
        tls);
  }

  public ChatServerConfig withHistory(Path newHistoryFile) {
    return new ChatServerConfig(
        port,
        maxClients,
        handshakeTimeout,
        readTimeout,
        newHistoryFile,
        historyLimit,
        historyReplayLimit,
        accountFile,
        tls);
  }

  public ChatServerConfig withHistory(Path newHistoryFile, int newLimit, int newReplayLimit) {
    return new ChatServerConfig(
        port,
        maxClients,
        handshakeTimeout,
        readTimeout,
        newHistoryFile,
        newLimit,
        newReplayLimit,
        accountFile,
        tls);
  }

  public ChatServerConfig withAccounts(Path newAccountFile) {
    return new ChatServerConfig(
        port,
        maxClients,
        handshakeTimeout,
        readTimeout,
        historyFile,
        historyLimit,
        historyReplayLimit,
        newAccountFile,
        tls);
  }

  public ChatServerConfig withTls(TlsServerConfig newTls) {
    return new ChatServerConfig(
        port,
        maxClients,
        handshakeTimeout,
        readTimeout,
        historyFile,
        historyLimit,
        historyReplayLimit,
        accountFile,
        newTls);
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
