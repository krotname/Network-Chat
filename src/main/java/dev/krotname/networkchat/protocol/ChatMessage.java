package dev.krotname.networkchat.protocol;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable message object used by both client and server. */
public record ChatMessage(
    MessageType type, String data, String sender, long timestamp, String messageId) {
  public static final int MAX_DATA_LENGTH = 2048;

  public ChatMessage {
    Objects.requireNonNull(type, "type");
    if (data != null && data.length() > MAX_DATA_LENGTH) {
      throw new IllegalArgumentException("Message data is too long");
    }
  }

  public static ChatMessage withData(MessageType type, String data, String sender) {
    return new ChatMessage(
        type, data, sender, Instant.now().toEpochMilli(), UUID.randomUUID().toString());
  }

  public static ChatMessage text(String data, String sender) {
    return withData(MessageType.TEXT, data, sender);
  }

  public static ChatMessage text(String data) {
    return text(data, null);
  }

  public static ChatMessage fromUser(String data, String userName) {
    return withData(MessageType.TEXT, data, userName);
  }
}
