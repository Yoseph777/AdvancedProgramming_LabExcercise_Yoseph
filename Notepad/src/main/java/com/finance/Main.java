package com.finance;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class Main extends Application {
    private File currentFile = null;
    private String savedContent = "";

    @Override
    public void start(Stage stage) {
        TextArea textArea = new TextArea();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Supported Files",
                        "*.txt",
                        "*.pdf"
                ),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        MenuItem newItem = new MenuItem("New");
        MenuItem open = new MenuItem("Open...");
        MenuItem save = new MenuItem("Save");
        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(newItem, open, new SeparatorMenuItem(), save);
        MenuBar menuBar = new MenuBar(fileMenu);

        newItem.setOnAction(e -> {
            if (confirmSaveIfChanged(stage, textArea)) {
                textArea.clear();
                currentFile = null;
                savedContent = "";
                updateTitle(stage);
            }
        });

        open.setOnAction(e -> {
            if (confirmSaveIfChanged(stage, textArea)) {
                File file = fileChooser.showOpenDialog(stage);
                if (file != null) {
                    try {
                        String fileName = file.getName().toLowerCase();
                        String content;
                        if (fileName.endsWith(".pdf")) {
                            PDDocument document = Loader.loadPDF(file);
                            PDFTextStripper stripper = new PDFTextStripper();
                            content = stripper.getText(document);
                            document.close();
                        } else {
                            content = Files.readString(file.toPath());
                        }
                        textArea.setText(content);
                        savedContent = content;
                        currentFile = file;
                        updateTitle(stage);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        showAlert("Error Reading File");
                    }
                }
            }
        });

        save.setOnAction(e -> {
            if (currentFile != null) {
                saveToFile(currentFile, textArea.getText());
            } else {
                saveAsFunc(stage, textArea);
            }
        });

        stage.setOnCloseRequest(e -> {
            if (!confirmSaveIfChanged(stage, textArea)) {
                e.consume();
            }
        });

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(textArea);

        stage.setScene(new Scene(root, 800, 500));
        updateTitle(stage);
        stage.show();
    }

    private boolean confirmSaveIfChanged(Stage stage, TextArea textArea) {
        String currentContent = textArea.getText();
        if (!currentContent.equals(savedContent)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes.");
            alert.setContentText("Do you want to save your changes before proceeding?");

            ButtonType buttonSave = new ButtonType("Save");
            ButtonType buttonDontSave = new ButtonType("Don't Save");
            ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonSave, buttonDontSave, buttonCancel);
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == buttonSave) {
                    if (currentFile != null) {
                        saveToFile(currentFile, currentContent);
                        return true;
                    } else {
                        return saveAsFunc(stage, textArea);
                    }
                } else if (result.get() == buttonDontSave) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean saveAsFunc(Stage stage, TextArea textArea) {
        FileChooser fc = new FileChooser();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            saveToFile(file, textArea.getText());
            currentFile = file;
            updateTitle(stage);
            return true;
        }
        return false;
    }

    private void saveToFile(File file, String content) {
        try {
            Files.writeString(file.toPath(), content);
            savedContent = content;
        } catch (Exception ex) {
            showAlert("Error Saving File");
        }
    }

    private void updateTitle(Stage stage) {
        String name = (currentFile == null) ? "Yoseph's" : currentFile.getName();
        stage.setTitle(name + " Notepad");
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    public static void main(String[] args) { launch(); }
}