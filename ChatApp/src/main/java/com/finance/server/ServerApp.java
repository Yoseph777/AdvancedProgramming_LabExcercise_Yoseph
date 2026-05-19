package com.finance.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerApp extends Application {
    private static final int PORT = 5050;
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static TextArea chatArea;
    private PrintWriter serverWriter;
    @Override
    public void start(Stage stage) {
        startServer();
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        chatArea = new TextArea();
        chatArea.setEditable(false);
        VBox.setVgrow(chatArea, Priority.ALWAYS);
        TextField input = new TextField();
        input.setPromptText("Server message...");
        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(e -> {
            String text = input.getText().trim();
            if (text.isEmpty()) return;
            String msg = "SERVER: " + text;
            appendMessage(msg);
            broadcast(msg);
            saveMessage("SERVER", text);
            input.clear();});

        input.setOnAction(e -> sendBtn.fire());
        HBox bottom = new HBox(10, input, sendBtn);
        HBox.setHgrow(input, Priority.ALWAYS);
        root.getChildren().addAll(new Label("Server Chat"), chatArea, bottom);
        Scene scene = new Scene(root, 700, 500);
        stage.setScene(scene);
        stage.setTitle("Finance Server");
        stage.show();
    }

    private void startServer() {
        Thread serverThread = new Thread(() -> {
            try {
                initializeDatabase();
                ServerSocket serverSocket = new ServerSocket(PORT);
                appendMessage("[Server started on port " + PORT + "]");
                while (true) {
                    Socket socket = serverSocket.accept();
                    appendMessage("[Client connected]");
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);new Thread(handler).start();
                }
            } catch (Exception e) {
                appendMessage("[Server Error] " + e.getMessage());
                e.printStackTrace();
            }
        });
        serverThread.start();
    }
    private static void initializeDatabase()
            throws SQLException {
        try (
                Connection conn =
                        DatabaseManager.getConnection();
                Statement st =
                        conn.createStatement()
        ) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS CHAT_MESSAGE (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "sender VARCHAR(100)," +
                            "message TEXT," +
                            "timestamp BIGINT" +
                            ")"
            );
        }
    }
    private static void saveMessage(
            String sender,
            String message
    ) {
        try (
                Connection conn =
                        DatabaseManager.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(
                                "INSERT INTO CHAT_MESSAGE " +
                                        "(sender, message, timestamp) " +
                                        "VALUES (?, ?, ?)"
                        )
        ) {
            ps.setString(1, sender);
            ps.setString(2, message);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(String msg) {
        for (ClientHandler client : clients) {
            client.send(msg);
        }
    }

    private static void appendMessage(String msg) {
        Platform.runLater(() -> {chatArea.appendText(msg + "\n");});
    }
    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    appendMessage(line);
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        saveMessage(parts[0], parts[1]);
                    }
                    broadcast(line);
                }
            } catch (Exception e) {
                appendMessage("[Client disconnected]");
            } finally {
                clients.remove(this);
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }
        public void send(String msg) {
            out.println(msg);
        }
    }
    public static void main(String[] args) {launch(args);}
}