/*
 * Copyright (c) 2021. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        ConsoleHelper.writeMessage("Введите порт");


        int port = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Сервер чата поднят на порту " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        ConsoleHelper.writeMessage("");


    }

    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> e : connectionMap.entrySet()
        ) {
            try {
                e.getValue().send(message);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public void run() {
            ConsoleHelper.writeMessage("Установлено новое соединение: " + socket.getRemoteSocketAddress().toString());
            String userName = "";
            try (Connection connection = new Connection(socket)) {
                userName = this.serverHandshake(connection);
                connectionMap.put(userName, connection);
                this.notifyUsers(connection, userName);
                Server.sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                this.serverMainLoop(connection, userName);

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("произошла ошибка при обмене данными с удаленным адресом" + socket.getRemoteSocketAddress().toString());
            }
            if (!userName.isEmpty()) {
                connectionMap.remove(userName);
                Server.sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }

            ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто: " + socket.getRemoteSocketAddress().toString());

        }


        private Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {

            String data = "";
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message receive = connection.receive();
                data = receive.getData();
                if (data != null && !data.equals("") && !connectionMap.containsKey(data)) {
                    connectionMap.put(data, connection);
                    connection.send(new Message(MessageType.NAME_ACCEPTED));
                    break;
                }
            }
            return data;
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {

            for (Map.Entry<String, Connection> e : connectionMap.entrySet()
            ) {
                String name = e.getKey();
                if (!userName.equals(name)) {
                    connection.send(new Message(MessageType.USER_ADDED, name));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {

                Message receive = connection.receive();

                if (receive.getType() == MessageType.TEXT) {
                    String broadCastMessage = userName + ": " + receive.getData();
                    Server.sendBroadcastMessage(new Message(MessageType.TEXT, broadCastMessage));
                } else {
                    ConsoleHelper.writeMessage("Cообщение - не текст");
                }

            }
        }

    }
}
