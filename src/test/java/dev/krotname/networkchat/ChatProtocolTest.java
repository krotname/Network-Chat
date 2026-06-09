package dev.krotname.networkchat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.ChatProtocol;
import dev.krotname.networkchat.protocol.MessageType;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ChatProtocolTest {

  @Test
  void roundTripTextMessage() throws IOException {
    ChatMessage message = ChatMessage.withData(MessageType.TEXT, "hello", "alice");
    String json = ChatProtocol.encode(message);
    ChatMessage restored = ChatProtocol.decode(json);
    assertEquals(message, restored);
  }

  @Test
  void rejectsOverlongPayload() {
    String overlong = "a".repeat(ChatMessage.MAX_DATA_LENGTH + 1);
    assertThrows(
        IllegalArgumentException.class,
        () -> ChatMessage.withData(MessageType.TEXT, overlong, "alice"));
  }

  @Test
  void decodeRejectsInvalidFrame() {
    assertThrows(IOException.class, () -> ChatProtocol.decode("{\"broken\":true}"));
  }
}
