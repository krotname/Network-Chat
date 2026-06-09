package dev.krotname.networkchat.client.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import dev.krotname.networkchat.client.ClientGuiController;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ChatWindowUiSmokeTest {

  @Test
  void chatWindowBuildsAndRendersState() throws Exception {
    assumeFalse(GraphicsEnvironment.isHeadless());
    ClientGuiController controller = new ClientGuiController(false);
    ChatWindow window = new ChatWindow(controller, false);

    try {
      controller.getModel().setNewMessage("alice: hello");
      controller.getModel().addUser("alice");
      SwingUtilities.invokeAndWait(
          () -> {
            window.refreshMessages();
            window.refreshUsers();
          });
      assertTrue(window.getMessagesText().contains("alice: hello"));
      assertTrue(window.getUsersText().contains("alice"));
    } finally {
      SwingUtilities.invokeAndWait(window::dispose);
      controller.dispose();
    }
  }
}
