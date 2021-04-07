package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;

    public static void main(String[] args) {
        new Client().run();
    }

    private volatile boolean clientConnected = false;

    public void run() {
        SocketThread s = getSocketThread();
        s.setDaemon(true);
        s.start();

        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
        String input = "";

        while (clientConnected) {
            input = ConsoleHelper.readString();
            if (input.equals("exit")) {
                break;
            }
            if (shouldSendTextFromConsole()) {
                sendTextMessage(input);
            }
        }
    }

    protected String getServerAddress() {
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            e.printStackTrace();
            clientConnected = false;
        }
    }

    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message) {
            System.out.println(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            System.out.println("Участник с именем " + userName + " присоединился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName) {
            System.out.println("Участник с именем " + userName + " покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {

            while (true) {
                Message receive = connection.receive();
                if (receive == null ||receive.getType() == null) throw new IOException("Unexpected MessageType");
                if (receive.getType().equals(MessageType.NAME_REQUEST)) {
                    String userName = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, userName));
                    //break;
                } else if (receive.getType().equals(MessageType.NAME_ACCEPTED)) {
                    notifyConnectionStatusChanged(true);
                    break;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
           while (true) {
                Message receive = connection.receive();

                if (receive == null ||receive.getType() == null) throw new IOException("Unexpected MessageType");

                if (receive.getType().equals(MessageType.USER_ADDED)) {
                    informAboutAddingNewUser(receive.getData());

                } else if (receive.getType().equals(MessageType.USER_REMOVED)) {
                    informAboutDeletingNewUser(receive.getData());

                } else if (receive.getType().equals(MessageType.TEXT)) {
                    processIncomingMessage(receive.getData());
                }
                else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }
        public void run(){
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();
            try (Socket socket = new Socket(serverAddress, serverPort)) {
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                notifyConnectionStatusChanged(false);
            }
        }

    }
}
