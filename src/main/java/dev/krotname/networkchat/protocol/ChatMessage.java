package dev.krotname.networkchat.protocol;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable message object used by both client and server.
 *
 * <p>For text messages, {@code data} contains only the raw chat text and {@code sender} contains
 * the user name. Room and private routing is carried in dedicated metadata fields.
 */
public record ChatMessage(
    MessageType type,
    String data,
    String sender,
    long timestamp,
    String messageId,
    int protocolVersion,
    String room,
    String recipient) {
  public static final int PROTOCOL_VERSION = 1;
  public static final int MAX_DATA_LENGTH = 2048;
  public static final String GENERAL_ROOM = "general";

  public ChatMessage {
    Objects.requireNonNull(type, "type");
    if (data != null && data.length() > MAX_DATA_LENGTH) {
      throw new IllegalArgumentException("Message data is too long");
    }
    if (requiresTextData(type) && (data == null || data.isBlank())) {
      throw new IllegalArgumentException("Text message data is required");
    }
    if ((type == MessageType.ROOM_JOIN || type == MessageType.ROOM_LEAVE)
        && (room == null || room.isBlank())) {
      throw new IllegalArgumentException("Room name is required");
    }
    if (type == MessageType.PRIVATE_TEXT && (recipient == null || recipient.isBlank())) {
      throw new IllegalArgumentException("Private message recipient is required");
    }
    if (room != null) {
      room = room.trim();
    }
    if (recipient != null) {
      recipient = recipient.trim();
    }
  }

  public ChatMessage(
      MessageType type, String data, String sender, long timestamp, String messageId) {
    this(type, data, sender, timestamp, messageId, PROTOCOL_VERSION, null, null);
  }

  public static ChatMessage withData(MessageType type, String data, String sender) {
    return new ChatMessage(
        type,
        data,
        sender,
        Instant.now().toEpochMilli(),
        UUID.randomUUID().toString(),
        PROTOCOL_VERSION,
        null,
        null);
  }

  public static ChatMessage text(String data, String sender) {
    return roomText(data, sender, GENERAL_ROOM);
  }

  public static ChatMessage roomText(String data, String sender, String room) {
    return new ChatMessage(
        MessageType.ROOM_TEXT,
        data,
        sender,
        Instant.now().toEpochMilli(),
        UUID.randomUUID().toString(),
        PROTOCOL_VERSION,
        room,
        null);
  }

  public static ChatMessage privateText(String data, String sender, String recipient) {
    return new ChatMessage(
        MessageType.PRIVATE_TEXT,
        data,
        sender,
        Instant.now().toEpochMilli(),
        UUID.randomUUID().toString(),
        PROTOCOL_VERSION,
        null,
        recipient);
  }

  public static ChatMessage roomJoin(String room) {
    return withRoom(MessageType.ROOM_JOIN, room);
  }

  public static ChatMessage roomLeave(String room) {
    return withRoom(MessageType.ROOM_LEAVE, room);
  }

  public static ChatMessage roomAdded(String room) {
    return withRoom(MessageType.ROOM_ADDED, room);
  }

  public static ChatMessage roomJoined(String room) {
    return withRoom(MessageType.ROOM_JOINED, room);
  }

  public static ChatMessage roomLeft(String room) {
    return withRoom(MessageType.ROOM_LEFT, room);
  }

  public static ChatMessage text(String data) {
    return text(data, null);
  }

  public static ChatMessage fromUser(String data, String userName) {
    return withData(MessageType.TEXT, data, userName);
  }

  public ChatMessage withSender(String newSender) {
    return new ChatMessage(
        type, data, newSender, timestamp, messageId, protocolVersion, room, recipient);
  }

  public ChatMessage inRoom(String newRoom) {
    return new ChatMessage(
        type, data, sender, timestamp, messageId, protocolVersion, newRoom, recipient);
  }

  private static ChatMessage withRoom(MessageType type, String room) {
    return new ChatMessage(
        type,
        room,
        null,
        Instant.now().toEpochMilli(),
        UUID.randomUUID().toString(),
        PROTOCOL_VERSION,
        room,
        null);
  }

  private static boolean requiresTextData(MessageType type) {
    return type == MessageType.TEXT
        || type == MessageType.ROOM_TEXT
        || type == MessageType.PRIVATE_TEXT;
  }
}
