package com.engine.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ChatServer server;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket clientSocket, ChatServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try (
                clientSocket;
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))
        ) {
            out = new PrintWriter(clientSocket.getOutputStream(), true, StandardCharsets.UTF_8);

            // Обработка подключения
            out.println("Добро пожаловать в чат! Введите ваше имя:");
            clientName = in.readLine();

            if (clientName == null || clientName.isBlank()) {
                clientName = "Аноним_" + clientSocket.getPort();
            }

            server.addClient(out);
            System.out.println(clientName + " подключился: " + clientSocket.getRemoteSocketAddress());
            out.println("✅ Вы вошли в чат как " + clientName + "! (для выхода закройте окно или введите /exit)");
            server.broadcast("📢 " + clientName + " присоединился к чату!", out);

            // Цикл чтения сообщений
            String message;
            while ((message = in.readLine()) != null) {
                if ("/exit".equalsIgnoreCase(message.trim())) {
                    break;
                }
                server.broadcast(clientName + ": " + message, out);
            }

        } catch (IOException e) {
            System.err.println("Обрыв связи с клиентом " + clientName);
        } finally {
            // Graceful disconnect
            if (out != null) {
                server.removeClient(out);
            }
            if (clientName != null) {
                System.out.println(clientName + " отключился.");
                server.broadcast("📢 " + clientName + " покинул чат.", null);
            }
        }
    }
}