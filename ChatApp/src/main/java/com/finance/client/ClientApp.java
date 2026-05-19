package com.finance.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ClientApp extends Application {
    private static final String HOST = "localhost";
    private static final int PORT = 5050;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private TextArea chatArea;
    private TextField inputField;
    private String username;

    @Override
    public void start(Stage stage) {
        username = askUsername();
        connect();
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        chatArea = new TextArea();
        chatArea.setEditable(false);
        VBox.setVgrow(chatArea, Priority.ALWAYS);
        inputField = new TextField();
        inputField.setPromptText("Type message...");
        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
        HBox bottom = new HBox(10, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        root.getChildren().addAll(new Label("Client Chat"), chatArea, bottom);
        Scene scene = new Scene(root, 700, 500);
        stage.setScene(scene);
        stage.setTitle("Finance Client - " + username);
        stage.show();
        listen();
    }

    private void connect() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            showError("Cannot connect to server");
        }
    }

    private void listen() {
        Thread t = new Thread(() -> {
            try {
                String line;
                while (
                        (line = in.readLine())
                                != null
                ) {
                    appendMessage(line);
                }
            } catch (Exception e) {
                appendMessage(
                        "[Disconnected]"
                );
            }
        });
        t.setDaemon(true);
        t.start();
    }
    private void sendMessage() {
        String text =
                inputField.getText().trim();
        if (text.isEmpty()) return;
        String msg = username + ": " + text;
        out.println(msg);
        inputField.clear();
    }
    private void appendMessage(String msg) {
        Platform.runLater(() -> {
            chatArea.appendText(msg + "\n");
        });
    }

    private String askUsername() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Enter username");
        return dialog.showAndWait().orElse("Anonymous");
    }
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.showAndWait();
    }
    @Override
    public void stop() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }
    public static void main(String[] args) {launch(args);}
}