package com.engine.chat.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatSwingClient extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;

    public ChatSwingClient() {
        super("Java TCP Chat");

        clientName = JOptionPane.showInputDialog(
                this,
                "Введите ваше имя для входа в чат:",
                "Авторизация",
                JOptionPane.PLAIN_MESSAGE
        );

        if (clientName == null || clientName.trim().isEmpty()) {
            System.exit(0);
        }

        initGUI();
        connectToServer();
    }

    private void initGUI() {
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sendButton = new JButton("Отправить");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            String serverPrompt = in.readLine();
            chatArea.append(serverPrompt + "\n");

            // Авто-отправка имени
            out.println(clientName);

            // Фоновый поток для прослушивания сервера
            new Thread(this::listenForMessages).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Сервер недоступен: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String finalMessage = message;
                SwingUtilities.invokeLater(() -> chatArea.append(finalMessage + "\n"));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> chatArea.append("Соединение с сервером потеряно.\n"));
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && out != null) {
            if (text.equalsIgnoreCase("/exit")) {
                disconnect();
            } else {
                out.println(text);
                chatArea.append("Вы: " + text + "\n");
                inputField.setText("");
            }
        }
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.println("/exit");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new ChatSwingClient().setVisible(true);
        });
    }
}