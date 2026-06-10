package dev.krotname.networkchat.client;

import dev.krotname.networkchat.client.gui.ChatWindow;
import dev.krotname.networkchat.client.gui.ClientGuiModel;
import dev.krotname.networkchat.protocol.ChatMessage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** GUI-specific client controller. */
public final class ClientGuiController extends ChatClient {
  private final ClientGuiModel model = new ClientGuiModel();
  private final ChatWindow view;

  public static void main(String[] args) {
    new ClientGuiController().run();
  }

  public ClientGuiController() {
    this(true);
  }

  public ClientGuiController(boolean visibleWindow) {
    view = new ChatWindow(this, visibleWindow);
  }

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "GUI model is intentionally mutable for controller-view synchronization.")
  public ClientGuiModel getModel() {
    return model;
  }

  @Override
  public String getServerAddress() {
    return view.getServerAddress();
  }

  @Override
  public int getServerPort() {
    return view.getServerPort();
  }

  @Override
  public String getUserName() {
    return view.getUserName();
  }

  @Override
  protected boolean shouldSendTextFromConsole() {
    return false;
  }

  @Override
  protected SocketThread getSocketThread() {
    return new GuiSocketThread();
  }

  public void dispose() {
    view.dispose();
  }

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Inner thread intentionally uses outer controller for UI callbacks.")
  public final class GuiSocketThread extends SocketThread {
    @Override
    protected void processIncomingMessage(ChatMessage message) {
      model.setNewMessage(formatTextMessage(message));
      view.refreshMessages();
    }

    @Override
    protected void informAboutAddingNewUser(String userName) {
      model.addUser(userName);
      view.refreshUsers();
    }

    @Override
    protected void informAboutDeletingNewUser(String userName) {
      model.deleteUser(userName);
      view.refreshUsers();
    }

    @Override
    protected void onConnectionStatusChanged(boolean clientConnected) {
      view.notifyConnectionStatusChanged(clientConnected);
    }
  }
}
