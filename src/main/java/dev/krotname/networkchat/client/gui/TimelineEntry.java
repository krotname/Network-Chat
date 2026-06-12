package dev.krotname.networkchat.client.gui;

import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.MessageType;
import java.time.Instant;
import java.util.Objects;

/** One rendered event in the local GUI timeline. */
public record TimelineEntry(
    Type type,
    String messageId,
    String sender,
    String text,
    long timestamp,
    boolean ownMessage,
    String room,
    String recipient) {
  public TimelineEntry {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(text, "text");
  }

  public static TimelineEntry text(ChatMessage message, boolean ownMessage) {
    if (message.type() != MessageType.TEXT
        && message.type() != MessageType.ROOM_TEXT
        && message.type() != MessageType.PRIVATE_TEXT) {
      throw new IllegalArgumentException("Timeline text entry requires text message");
    }
    return new TimelineEntry(
        Type.TEXT,
        message.messageId(),
        message.sender(),
        message.data(),
        message.timestamp(),
        ownMessage,
        message.room(),
        message.recipient());
  }

  public static TimelineEntry service(String text) {
    return new TimelineEntry(
        Type.SERVICE, null, null, text, Instant.now().toEpochMilli(), false, null, null);
  }

  public enum Type {
    TEXT,
    SERVICE
  }
}
