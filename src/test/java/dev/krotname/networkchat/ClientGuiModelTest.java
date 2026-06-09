package dev.krotname.networkchat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.krotname.networkchat.client.gui.ClientGuiModel;
import org.junit.jupiter.api.Test;

class ClientGuiModelTest {

  @Test
  void tracksUsersAndMessages() {
    ClientGuiModel model = new ClientGuiModel();
    model.addUser("alice");
    model.addUser("bob");
    model.setNewMessage("hello");

    assertTrue(model.getAllUserNames().contains("alice"));
    assertEquals("hello", model.getNewMessage());
    assertEquals(2, model.getAllUserNames().size());

    model.deleteUser("alice");
    assertEquals(1, model.getAllUserNames().size());
    model.deleteUser("unknown");
  }
}
