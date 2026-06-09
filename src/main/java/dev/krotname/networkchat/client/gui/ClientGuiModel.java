package dev.krotname.networkchat.client.gui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** GUI state for users and latest chat message. */
public final class ClientGuiModel {
  private final Set<String> allUserNames = new HashSet<>();
  private String newMessage = "";

  /** Returns a defensive copy to prevent external mutation by UI tests or clients. */
  public synchronized Set<String> getAllUserNames() {
    return Collections.unmodifiableSet(new HashSet<>(allUserNames));
  }

  public synchronized String getNewMessage() {
    return newMessage;
  }

  public synchronized void setNewMessage(String newMessage) {
    this.newMessage = newMessage;
  }

  public synchronized void addUser(String newUserName) {
    allUserNames.add(newUserName);
  }

  public synchronized void deleteUser(String userName) {
    allUserNames.remove(userName);
  }
}
