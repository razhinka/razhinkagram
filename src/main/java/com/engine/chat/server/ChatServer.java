package com.engine.chat.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int PORT = 8080;

    // Потокобезопасная коллекция для рассылки сообщений
    private final Set<PrintWriter> clientWriters = ConcurrentHashMap.newKeySet();

    public void start() {
        System.out.println("Чат-сервер запущен на порту " + PORT);

        // Используем виртуальные потоки Java 21 для обработки соединений
        try (ServerSocket serverSocket = new ServerSocket(PORT);
             ExecutorService virtualThreads = Executors.newVirtualThreadPerTaskExecutor()) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                virtualThreads.execute(new ClientHandler(clientSocket, this));
            }
        } catch (IOException e) {
            System.err.println("Критическая ошибка сервера: " + e.getMessage());
        }
    }

    public void broadcast(String message, PrintWriter senderWriter) {
        for (PrintWriter writer : clientWriters) {
            if (writer != senderWriter) {
                writer.println(message);
            }
        }
    }

    public void addClient(PrintWriter writer) {
        clientWriters.add(writer);
    }

    public void removeClient(PrintWriter writer) {
        clientWriters.remove(writer);
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}