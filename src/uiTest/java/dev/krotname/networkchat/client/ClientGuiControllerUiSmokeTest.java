package dev.krotname.networkchat.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import dev.krotname.networkchat.client.gui.ChatWindow;
import dev.krotname.networkchat.client.gui.ConnectionSettingsStore;
import dev.krotname.networkchat.protocol.ChatMessage;
import dev.krotname.networkchat.protocol.MessageType;
import java.awt.GraphicsEnvironment;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ClientGuiControllerUiSmokeTest {

  @Test
  void chatWindowBuildsAndRendersState() throws Exception {
    assumeFalse(GraphicsEnvironment.isHeadless());
    Preferences preferences =
        Preferences.userRoot().node("dev.krotname.networkchat.ui.tests." + UUID.randomUUID());
    ClientGuiController controller =
        new ClientGuiController(false, new ConnectionSettingsStore(preferences));
    ChatWindow window = controller.getViewForTesting();

    try {
      controller
          .getModel()
          .addTextMessage(
              new ChatMessage(
                  MessageType.ROOM_TEXT,
                  "hello",
                  "alice",
                  1L,
                  "id-1",
                  ChatMessage.PROTOCOL_VERSION,
                  "general",
                  null),
              "alice");
      controller
          .getModel()
          .addTextMessage(ChatMessage.privateText("secret", "bob", "alice"), "alice");
      controller.getModel().addServiceMessage("Пользователь bob в чате");
      controller.getModel().addUser("alice");
      controller.getModel().addUser("bob");
      controller.getModel().joinRoom("general");
      controller.getModel().addRoom("dev");
      SwingUtilities.invokeAndWait(
          () -> {
            window.refreshMessages();
            window.refreshUsers();
            window.refreshRooms("general");
            window.notifyConnectionStatusChanged(true, "alice", null, 2);
          });
      assertTrue(window.getMessagesText().contains("Вы: hello"));
      assertTrue(window.getMessagesText().contains("лично -> alice"));
      assertTrue(window.getMessagesText().contains("* Пользователь bob в чате"));
      assertTrue(window.getUsersText().contains("alice"));
      assertTrue(window.getRoomCount() >= 2);
      assertTrue(window.getSelectedRoom().contains("general"));
      assertTrue(window.getStatusText().contains("Участников: 2"));
      assertFalse(window.isConnectionPanelVisible());

      SwingUtilities.invokeAndWait(
          () -> window.notifyConnectionStatusChanged(false, "alice", "Connection lost", 2));
      assertFalse(window.isMessageInputEditable());
      assertTrue(window.isReconnectButtonEnabled());
      assertTrue(window.isConnectionPanelVisible());

      controller.clearTimeline();
      assertTrue(window.getMessagesText().isBlank());
    } finally {
      SwingUtilities.invokeAndWait(controller::dispose);
      preferences.removeNode();
    }
  }
}
