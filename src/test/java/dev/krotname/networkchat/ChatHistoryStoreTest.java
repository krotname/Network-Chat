package dev.krotname.networkchat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.krotname.networkchat.network.ChatHistoryStore;
import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.ChatProtocol;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChatHistoryStoreTest {

  @TempDir private Path tempDir;

  @Test
  void persistsAndRotatesRoomMessages() {
    Path historyFile = tempDir.resolve("history.jsonl");
    ChatHistoryStore store = ChatHistoryStore.open(historyFile, 2);

    store.save(ChatMessage.roomText("one", "alice", "general"));
    store.save(ChatMessage.roomText("two", "alice", "general"));
    store.save(ChatMessage.roomText("three", "alice", "general"));

    ChatHistoryStore reloaded = ChatHistoryStore.open(historyFile, 2);

    assertEquals(2, reloaded.recentRoomMessages("general", 10).size());
    assertEquals("two", reloaded.recentRoomMessages("general", 10).getFirst().data());
    assertEquals("three", reloaded.recentRoomMessages("general", 10).getLast().data());
  }

  @Test
  void ignoresCorruptLinesOnStartup() throws Exception {
    Path historyFile = tempDir.resolve("history.jsonl");
    ChatMessage valid = ChatMessage.roomText("valid", "alice", "general");
    Files.writeString(
        historyFile, "not-json\n" + ChatProtocol.encode(valid) + "\n", StandardCharsets.UTF_8);

    ChatHistoryStore store = ChatHistoryStore.open(historyFile, 10);

    assertEquals(1, store.corruptRecordCount());
    assertEquals(1, store.recentRoomMessages("general", 10).size());
    assertTrue(store.knownRooms().contains("general"));
  }

  @Test
  void migratesLegacyTextHistoryToCurrentRoomFrames() throws Exception {
    Path historyFile = tempDir.resolve("legacy-history.jsonl");
    Files.writeString(
        historyFile,
        """
        {"type":"TEXT","data":"legacy","sender":"alice","timestamp":1,"messageId":"old"}
        """
            .strip(),
        StandardCharsets.UTF_8);

    ChatHistoryStore store = ChatHistoryStore.open(historyFile, 10);
    ChatMessage migrated = store.recentRoomMessages("general", 10).getFirst();

    assertEquals("legacy", migrated.data());
    assertEquals(ChatMessage.PROTOCOL_VERSION, migrated.protocolVersion());
    assertEquals("general", migrated.room());
  }
}
