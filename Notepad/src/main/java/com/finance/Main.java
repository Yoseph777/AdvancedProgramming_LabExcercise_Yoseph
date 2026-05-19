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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class Main extends Application {
    private File currentFile = null;

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
        MenuItem saveAs = new MenuItem("Save As...");

        Menu fileMenu = new Menu("File");
        Menu editMenu = new Menu("Edit");
        fileMenu.getItems().addAll(newItem, open, new SeparatorMenuItem(), save, saveAs);
        MenuBar menuBar = new MenuBar(fileMenu);

        newItem.setOnAction(e -> {
            textArea.clear();
            currentFile = null;
            updateTitle(stage);
        });

        open.setOnAction(e -> {

            File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                try {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".pdf")) {
                        PDDocument document = Loader.loadPDF(file);
                        PDFTextStripper stripper = new PDFTextStripper();
                        String content = stripper.getText(document);
                        textArea.setText(content);
                        document.close();
                    } else {
                        String content = Files.readString(file.toPath());
                        textArea.setText(content);
                    }
                    currentFile = file;
                    updateTitle(stage);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showAlert("Error Reading File");
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

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(textArea);

        stage.setScene(new Scene(root, 800, 500));
        updateTitle(stage);
        stage.show();
    }

    private void saveAsFunc(Stage stage, TextArea textArea) {
        FileChooser fc = new FileChooser();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            saveToFile(file, textArea.getText());
            currentFile = file;
            updateTitle(stage);
        }
    }

    private void saveToFile(File file, String content) {
        try {
            Files.writeString(file.toPath(), content);
        } catch (Exception ex) { showAlert("Error Saving File"); }
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