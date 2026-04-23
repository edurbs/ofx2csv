package com.mcs.ofx2csv.controller;

import com.mcs.ofx2csv.model.TransactionRow;
import com.mcs.ofx2csv.service.ExcelWriter;
import com.mcs.ofx2csv.service.OfxParser;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML
    private TextField fileField;

    @FXML
    private TextField dirField;

    private final List<File> selectedFiles = new ArrayList<>();
    private final OfxParser ofxParser = new OfxParser();
    private final ExcelWriter excelWriter = new ExcelWriter();

    /**
     * Sets the output directory path (used by tests to bypass DirectoryChooser).
     */
    public void setOutputDir(String path) {
        dirField.setText(path);
    }

    /**
     * Sets the selected OFX files (used by tests to bypass FileChooser).
     */
    public void setSelectedFiles(List<File> files) {
        selectedFiles.clear();
        selectedFiles.addAll(files);
        fileField.setText(String.join("; ", selectedFiles.stream()
                .map(File::getName).toList()));
    }

    /**
     * Returns the last alert message (used by tests to verify conversion result).
     */
    public String getLastAlertMessage() {
        return lastAlertMessage;
    }

    private String lastAlertMessage;

    @FXML
    private void pickFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar arquivos OFX");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos OFX", "*.ofx", "*.OFX"));
        List<File> files = chooser.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            selectedFiles.clear();
            selectedFiles.addAll(files);
            fileField.setText(String.join("; ", selectedFiles.stream()
                    .map(File::getName).toList()));
        }
    }

    @FXML
    private void pickDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecionar pasta de destino");
        File dir = chooser.showDialog(null);
        if (dir != null) {
            dirField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void convert() {
        if (selectedFiles.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Nenhum arquivo selecionado",
                    "Selecione pelo menos um arquivo OFX.");
            return;
        }

        String dirPath = dirField.getText();
        if (dirPath == null || dirPath.isBlank() || !Files.isDirectory(Path.of(dirPath))) {
            showAlert(Alert.AlertType.WARNING, "Diretório inválido",
                    "Selecione um diretório de destino válido.");
            return;
        }

        Path outputDir = Path.of(dirPath);
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (File file : selectedFiles) {
            try {
                List<TransactionRow> rows = ofxParser.parse(file.toPath());
                String baseName = removeExtension(file.getName());
                excelWriter.write(rows, outputDir, baseName);
                successCount++;
            } catch (Exception e) {
                errors.add(file.getName() + ": " + e.getMessage());
            }
        }

        if (errors.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Conversão concluída",
                    successCount + " arquivo(s) convertido(s) com sucesso.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Erros na conversão",
                    successCount + " arquivo(s) convertido(s).\n" +
                            errors.size() + " erro(s):\n" + String.join("\n", errors));
        }
    }

    private String removeExtension(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        lastAlertMessage = message;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
