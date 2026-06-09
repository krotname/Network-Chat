package dev.krotname.networkchat.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/** Serializes and deserializes chat messages to a single-line JSON format. */
public final class ChatProtocol {
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  private ChatProtocol() {}

  public static String encode(ChatMessage message) throws IOException {
    try {
      return MAPPER.writeValueAsString(message);
    } catch (JsonProcessingException ex) {
      throw new IOException("Failed to encode protocol message", ex);
    }
  }

  /**
   * Decodes one JSON line into a validated protocol message. Unknown or malformed frames are
   * rejected with IOException to keep protocol failures explicit.
   */
  public static ChatMessage decode(String line) throws IOException {
    try {
      return MAPPER.readValue(line, ChatMessage.class);
    } catch (JsonProcessingException ex) {
      throw new IOException("Failed to decode protocol message", ex);
    }
  }
}
