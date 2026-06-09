package dev.krotname.networkchat.client;

import dev.krotname.networkchat.util.ConsoleInput;
import java.io.IOException;

public final class ConsoleChatClient extends ChatClient {
  public static void main(String[] args) {
    new ConsoleChatClient().run();
  }

  @Override
  public String getServerAddress() throws IOException {
    return ConsoleInput.readString("Введите адрес сервера: ");
  }

  @Override
  public int getServerPort() throws IOException {
    return ConsoleInput.readInt("Введите порт сервера: ");
  }

  @Override
  public String getUserName() throws IOException {
    return ConsoleInput.readString("Введите ник: ");
  }

  @Override
  protected boolean shouldSendTextFromConsole() {
    return true;
  }

  @Override
  protected SocketThread getSocketThread() {
    return new ConsoleSocketThread();
  }

  private final class ConsoleSocketThread extends SocketThread {
    @Override
    protected void processIncomingMessage(String message) {
      System.out.println(message);
    }

    @Override
    protected void informAboutAddingNewUser(String userName) {
      System.out.printf("Участник с именем %s присоединился к чату.%n", userName);
    }

    @Override
    protected void informAboutDeletingNewUser(String userName) {
      System.out.printf("Участник с именем %s вышел из чата.%n", userName);
    }

    @Override
    protected void notifyConnectionStatusChanged(boolean clientConnected) {
      super.notifyConnectionStatusChanged(clientConnected);
      if (clientConnected) {
        System.out.println("Соединение с сервером установлено");
      } else {
        System.out.println("Соединение с сервером не установлено");
      }
    }
  }
}
