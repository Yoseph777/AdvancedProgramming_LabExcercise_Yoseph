package university;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Teacher extends Application {

    private Server.UniversityService service;
    private int    myId;
    private String myName;
    private static final String ROLE = "TEACHER";
    private int    lastMessageId = 0;
    private Thread socketThread;
    private TextArea chatArea;
    private TextField chatInput;

    @Override
    public void start(Stage stage) {
        connectRMI();
        stage.setTitle("AASTU – Teacher Portal");
        stage.setScene(buildLoginScene(stage));
        stage.setWidth(520);
        stage.setHeight(680);
        stage.show();
    }

    private void connectRMI() {
        try {
            service = (Server.UniversityService) Naming.lookup("rmi://localhost/UniversityService");
        } catch (Exception e) {
            showError("Cannot reach server.\nMake sure Server.java is running.\n" + e.getMessage());
        }
    }

    private Scene buildLoginScene(Stage stage) {
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #1e2a3a;");

        Label title = new Label("Teacher Login");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("AASTU Registry & Chat System");
        subtitle.setTextFill(Color.LIGHTGRAY);

        TextField idField   = styledField("Teacher ID");
        TextField nameField = styledField("Full Name");

        Button loginBtn = new Button("Login");
        loginBtn.setPrefWidth(200);
        loginBtn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white;" +
                "-fx-font-size:14; -fx-background-radius:6;");

        Label status = new Label();
        status.setTextFill(Color.SALMON);

        loginBtn.setOnAction(e -> {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String name = nameField.getText().trim();
                if (name.isEmpty()) { status.setText("Name cannot be empty."); return; }

                boolean ok = service.login(ROLE, id, name);
                if (ok) {
                    myId   = id;
                    myName = name;
                    stage.setScene(buildMainScene(stage));
                    startSocketListener();
                } else {
                    status.setText("Invalid credentials. Register first.");
                }
            } catch (NumberFormatException nfe) {
                status.setText("ID must be a number.");
            } catch (Exception ex) {
                status.setText("Login error: " + ex.getMessage());
            }
        });

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #4a90d9;");

        Label hint = new Label("First time? Register your teacher record via the Registry tab\n" +
                "after logging in with a new ID and name.");
        hint.setTextFill(Color.GRAY);
        hint.setFont(Font.font("Arial", 11));
        hint.setTextAlignment(TextAlignment.CENTER);
        hint.setWrapText(true);

        Button registerBtn = new Button("Register as New Teacher");
        registerBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a90d9;" +
                "-fx-border-color: #4a90d9; -fx-border-radius:6;" +
                "-fx-background-radius:6;");
        registerBtn.setPrefWidth(200);
        registerBtn.setOnAction(e -> {
            try {
                int id       = Integer.parseInt(idField.getText().trim());
                String name  = nameField.getText().trim();
                String dept  = showInputDialog("Department (for registration):");
                if (dept == null || dept.isEmpty()) return;
                service.addTeacher(id, name, dept);
                status.setTextFill(Color.LIGHTGREEN);
                status.setText("Registered! You can now log in.");
            } catch (NumberFormatException nfe) {
                status.setTextFill(Color.SALMON); status.setText("ID must be a number.");
            } catch (Exception ex) {
                status.setTextFill(Color.SALMON); status.setText("Error: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(title, subtitle, new Separator(),
                idField, nameField, loginBtn, registerBtn, status, sep, hint);
        return new Scene(root);
    }

    private Scene buildMainScene(Stage stage) {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab registryTab = new Tab("📋  Registry", buildRegistryPane(stage));
        Tab chatTab     = new Tab("💬  Chat",     buildChatPane(stage));

        tabs.getTabs().addAll(registryTab, chatTab);

        chatTab.setOnSelectionChanged(e -> {
            if (chatTab.isSelected()) loadChatHistory();
        });

        BorderPane root = new BorderPane(tabs);
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: #1e2a3a;");
        topBar.setPadding(new Insets(8, 14, 8, 14));
        Label who = new Label("Logged in as: " + myName + "  [TEACHER]");
        who.setTextFill(Color.WHITE);
        Button logout = new Button("Logout");
        logout.setStyle("-fx-background-color: #c0392b; -fx-text-fill:white; -fx-background-radius:4;");
        logout.setOnAction(e -> { stopSocketListener(); stage.setScene(buildLoginScene(stage)); });
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(who, spacer, logout);
        root.setTop(topBar);

        return new Scene(root, 520, 680);
    }

    private Pane buildRegistryPane(Stage stage) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(18));

        TitledPane addPane = new TitledPane();
        addPane.setText("Add / Update Teacher Record");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8); form.setPadding(new Insets(10));

        TextField idF   = new TextField(); idF.setPromptText("ID (number)");
        TextField nameF = new TextField(); nameF.setPromptText("Full Name");
        TextField deptF = new TextField(); deptF.setPromptText("Department");

        form.addRow(0, new Label("ID:"),   idF);
        form.addRow(1, new Label("Name:"), nameF);
        form.addRow(2, new Label("Dept:"), deptF);

        Button addBtn = new Button("Save Teacher");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill:white; -fx-background-radius:4;");
        form.add(addBtn, 1, 3);

        Label feedback = new Label(); form.add(feedback, 0, 4, 2, 1);

        addBtn.setOnAction(e -> {
            try {
                int id     = Integer.parseInt(idF.getText().trim());
                String name = nameF.getText().trim();
                String dept = deptF.getText().trim();
                service.addTeacher(id, name, dept);
                feedback.setTextFill(Color.GREEN);
                feedback.setText("Teacher saved successfully.");
                idF.clear(); nameF.clear(); deptF.clear();
            } catch (NumberFormatException nfe) {
                feedback.setTextFill(Color.RED); feedback.setText("ID must be a number.");
            } catch (Exception ex) {
                feedback.setTextFill(Color.RED); feedback.setText("Error: " + ex.getMessage());
            }
        });

        addPane.setContent(form);

        TextArea display = new TextArea();
        display.setEditable(false);
        display.setPrefHeight(260);
        display.setStyle("-fx-font-family: monospace;");

        HBox viewBtns = new HBox(10);
        Button showTeachers = new Button("List All Teachers");
        Button showStudents = new Button("List All Students");
        showTeachers.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-background-radius:4;");
        showStudents.setStyle("-fx-background-color:#8e44ad;-fx-text-fill:white;-fx-background-radius:4;");

        showTeachers.setOnAction(e -> {
            try { display.setText("── Teachers ──\n" + service.listTeachers()); }
            catch (Exception ex) { display.setText("Error: " + ex.getMessage()); }
        });
        showStudents.setOnAction(e -> {
            try { display.setText("── Students ──\n" + service.listStudents()); }
            catch (Exception ex) { display.setText("Error: " + ex.getMessage()); }
        });

        viewBtns.getChildren().addAll(showTeachers, showStudents);
        root.getChildren().addAll(addPane, new Separator(), viewBtns, display);
        return root;
    }

    private Pane buildChatPane(Stage stage) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(14));

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(460);
        chatArea.setStyle("-fx-font-family: monospace; -fx-font-size:12;");
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        chatInput = new TextField();
        chatInput.setPromptText("Type a message…");

        Button sendBtn    = new Button("Send");
        Button fileBtn    = new Button("📎 File");
        Button refreshBtn = new Button("🔄");

        sendBtn.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-background-radius:4;");
        fileBtn.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-background-radius:4;");
        refreshBtn.setStyle("-fx-background-radius:4;");

        HBox inputRow = new HBox(8, chatInput, sendBtn, fileBtn, refreshBtn);
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        sendBtn.setOnAction(e -> sendText());
        chatInput.setOnAction(e -> sendText());
        refreshBtn.setOnAction(e -> loadChatHistory());

        fileBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select File to Send");
            File f = fc.showOpenDialog(stage);
            if (f == null) return;
            try {
                byte[] data = readFileBytes(f);
                ChatMessage msg = new ChatMessage(ROLE, myName, f.getName(), data);
                service.sendMessage(msg);
                loadChatHistory();
            } catch (Exception ex) {
                showError("Failed to send file: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(
                new Label("Chat  –  Teachers & Students"),
                chatArea,
                inputRow
        );
        return root;
    }

    private void sendText() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        try {
            ChatMessage msg = new ChatMessage(ROLE, myName, text);
            service.sendMessage(msg);
            chatInput.clear();
            loadChatHistory();
        } catch (Exception ex) {
            showError("Send failed: " + ex.getMessage());
        }
    }

    private void loadChatHistory() {
        if (service == null) return;
        try {
            List<ChatMessage> msgs = service.getMessages(0);
            StringBuilder sb = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            for (ChatMessage m : msgs) {
                String time = sdf.format(new Date(m.timestamp));
                if (m.type == ChatMessage.Type.FILE) {
                    sb.append(String.format("[%s] [%s] %s sent a file: %s%n",
                            time, m.senderRole, m.senderName, m.content));
                } else {
                    sb.append(String.format("[%s] [%s] %s: %s%n",
                            time, m.senderRole, m.senderName, m.content));
                }
                lastMessageId = Math.max(lastMessageId, m.id);
            }
            chatArea.setText(sb.toString());
            chatArea.setScrollTop(Double.MAX_VALUE);
        } catch (Exception ex) {
            chatArea.appendText("\n[Error loading messages: " + ex.getMessage() + "]\n");
        }
    }

    private void startSocketListener() {
        socketThread = new Thread(() -> {
            try (Socket s = new Socket("localhost", 5000);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("NEW_MSG:")) {
                        Platform.runLater(this::loadChatHistory);
                    }
                }
            } catch (Exception ignored) {}
        });
        socketThread.setDaemon(true);
        socketThread.start();
    }

    private void stopSocketListener() {
        if (socketThread != null) socketThread.interrupt();
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(260);
        tf.setStyle("-fx-background-color:#2c3e50; -fx-text-fill:white;" +
                "-fx-prompt-text-fill: #aaa; -fx-background-radius:4;" +
                "-fx-padding:8;");
        return tf;
    }

    private byte[] readFileBytes(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return fis.readAllBytes();
        }
    }

    private void showError(String msg) {
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, msg).showAndWait());
    }

    private String showInputDialog(String prompt) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setHeaderText(prompt);
        dlg.setTitle("Input");
        return dlg.showAndWait().orElse(null);
    }

    @Override
    public void stop() { stopSocketListener(); }

    public static void main(String[] args) { launch(args); }
}
