package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class BotClient extends Client {

    public static void main(String[] args) {
        new BotClient().run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        int x = (int) (Math.random() * 100);
        return "date_bot_" + x;
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            BotClient.this.sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            if (message == null) return;
            String[] parts = message.split(": ");
            if (parts.length != 2) return;
            HashMap<String, String> map = new HashMap<>();
            map.put("дата", "d.MM.YYYY");
            map.put("день", "d");
            map.put("месяц", "MMMM");
            map.put("год", "YYYY");
            map.put("время", "H:mm:ss");
            map.put("час", "H");
            map.put("минуты", "m");
            map.put("секунды", "s");
            String pattern = map.get(parts[1]);
            if (pattern == null) return;
            String answer = new SimpleDateFormat(pattern).format(Calendar.getInstance().getTime());
            sendTextMessage(String.format("Информация для %s: %s", parts[0], answer));
        }
    }
}
