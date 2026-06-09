package dev.krotname.networkchat.client;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Bot client that responds to simple date/time commands. */
public final class BotChatClient extends ChatClient {
  private final Clock clock;
  private final String botUserName;

  public BotChatClient() {
    this(Clock.systemDefaultZone());
  }

  public BotChatClient(Clock clock) {
    this.clock = clock;
    this.botUserName = "date_bot_" + ThreadLocalRandom.current().nextInt(1000);
  }

  public static void main(String[] args) {
    new BotChatClient().run();
  }

  @Override
  public String getServerAddress() {
    return "localhost";
  }

  @Override
  public int getServerPort() {
    return 1500;
  }

  @Override
  public String getUserName() {
    return botUserName;
  }

  @Override
  protected boolean shouldSendTextFromConsole() {
    return false;
  }

  @Override
  protected SocketThread getSocketThread() {
    return new BotSocketThread();
  }

  private static final Map<String, DateTimeFormatter> COMMANDS = createCommands();

  private static Map<String, DateTimeFormatter> createCommands() {
    Map<String, DateTimeFormatter> map = new HashMap<>();
    map.put("дата", DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    map.put("день", DateTimeFormatter.ofPattern("d"));
    map.put("месяц", DateTimeFormatter.ofPattern("MMMM"));
    map.put("год", DateTimeFormatter.ofPattern("yyyy"));
    map.put("время", DateTimeFormatter.ofPattern("HH:mm:ss"));
    map.put("час", DateTimeFormatter.ofPattern("H"));
    map.put("минуты", DateTimeFormatter.ofPattern("m"));
    map.put("секунды", DateTimeFormatter.ofPattern("s"));
    return Collections.unmodifiableMap(map);
  }

  private void answerCommand(String sender, String command) {
    DateTimeFormatter formatter = COMMANDS.get(command);
    if (formatter == null) {
      return;
    }
    String answer = LocalDateTime.now(clock).format(formatter);
    sendTextMessage(String.format("Информация для %s: %s", sender, answer));
  }

  private final class BotSocketThread extends SocketThread {
    private static final String GREETING =
        "Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.";

    @Override
    protected void clientHandshake() throws IOException {
      super.clientHandshake();
      sendTextMessage(GREETING);
    }

    @Override
    protected void processIncomingMessage(String message) {
      if (message == null || !message.contains(": ")) {
        return;
      }
      String[] chunks = message.split(": ", 2);
      if (chunks.length != 2) {
        return;
      }
      String sender = chunks[0];
      String command = chunks[1].trim().toLowerCase();
      answerCommand(sender, command);
    }

    @Override
    protected void clientMainLoop() throws IOException {
      super.clientMainLoop();
    }
  }
}
