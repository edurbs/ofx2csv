package com.mcs.ofx2csv.controller;

import com.mcs.ofx2csv.model.TransactionRow;
import com.mcs.ofx2csv.service.ExcelWriter;
import com.mcs.ofx2csv.service.OfxParser;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // Zone 1: Drop Zone
    @FXML private StackPane dropZone;
    @FXML private Label dropZoneIcon;
    @FXML private Label dropZoneLabel;
    @FXML private HBox fileListContainer;
    @FXML private Label fileCountLabel;

    // Zone 2: Destination
    @FXML private TextField dirField;

    // Zone 3: Preview Table
    @FXML private TableView<TransactionRow> previewTable;
    @FXML private TableColumn<TransactionRow, String> colDate;
    @FXML private TableColumn<TransactionRow, String> colHistorico;
    @FXML private TableColumn<TransactionRow, Number> colDebito;
    @FXML private TableColumn<TransactionRow, Number> colCredito;
    @FXML private TableColumn<TransactionRow, Number> colSoma;
    @FXML private Label transactionCountLabel;
    @FXML private ChoiceBox<File> fileSelector;

    // Zone 4: Convert + Feedback
    @FXML private Button convertButton;
    @FXML private VBox feedbackBar;
    @FXML private Label feedbackIcon;
    @FXML private Label feedbackMessage;
    @FXML private Hyperlink feedbackAction;
    @FXML private Button feedbackDismiss;
    @FXML private TextArea feedbackDetails;

    private final List<File> selectedFiles = new ArrayList<>();
    private final OfxParser ofxParser = new OfxParser();
    private final ExcelWriter excelWriter = new ExcelWriter();
    private String lastAlertMessage;
    private volatile boolean converting = false;
    private Path lastFeedbackDir;
    private PauseTransition autoHideTimer;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private void initialize() {
        setupTableColumns();
        setupDragAndDrop();
        setupAutoHide();
        setupKeyboardShortcuts();
        setupFileSelector();
        dirField.textProperty().addListener(obs -> updateConvertButtonState());
        updateConvertButtonState();
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().date().format(DATE_FORMATTER)));

        colHistorico.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().historico()));

        colDebito.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().debito()));
        colDebito.setCellFactory(col -> new NumericCell());

        colCredito.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().credito()));
        colCredito.setCellFactory(col -> new NumericCell());

        colSoma.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().soma()));
        colSoma.setCellFactory(col -> new NumericCell());
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragEntered(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                dropZone.getStyleClass().add("drop-zone-active");
            }
            event.consume();
        });

        dropZone.setOnDragExited(event -> {
            dropZone.getStyleClass().remove("drop-zone-active");
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                List<File> ofxFiles = db.getFiles().stream()
                        .filter(f -> f.getName().toLowerCase().endsWith(".ofx"))
                        .toList();

                if (ofxFiles.isEmpty()) {
                    dropZoneLabel.setText("Apenas arquivos .ofx são aceitos");
                    dropZoneLabel.setStyle("-fx-text-fill: #EF4444;");
                    PauseTransition resetLabel = new PauseTransition(Duration.seconds(2));
                    resetLabel.setOnFinished(e -> {
                        dropZoneLabel.setText("Arraste seus arquivos OFX aqui ou clique para selecionar");
                        dropZoneLabel.setStyle("-fx-text-fill: #6B7280;");
                    });
                    resetLabel.play();
                } else {
                    selectedFiles.clear();
                    selectedFiles.addAll(ofxFiles);
                    updateFileListUI();
                    if (dirField.getText().isBlank()) {
                        setDefaultOutputDir();
                    }
                    updatePreviewTable();
                    updateConvertButtonState();
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void setupAutoHide() {
        autoHideTimer = new PauseTransition(Duration.seconds(10));
        autoHideTimer.setOnFinished(e -> hideFeedback());
    }

    private void setupKeyboardShortcuts() {
        dropZone.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown() && event.getCode() == KeyCode.O) {
                        pickFiles();
                        event.consume();
                    } else if (event.isControlDown() && event.getCode() == KeyCode.S) {
                        pickDirectory();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ENTER && !convertButton.isDisabled()) {
                        convert();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        ((Stage) newScene.getWindow()).close();
                        event.consume();
                    } else if (event.isControlDown() && event.getCode() == KeyCode.W) {
                        ((Stage) newScene.getWindow()).close();
                        event.consume();
                    } else if (event.isControlDown() && event.getCode() == KeyCode.L) {
                        selectedFiles.clear();
                        updateFileListUI();
                        updatePreviewTable();
                        dirField.setText("");
                        updateConvertButtonState();
                        hideFeedback();
                        event.consume();
                    }
                });
            }
        });
    }

    private void setupFileSelector() {
        fileSelector.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(File file) {
                return file != null ? file.getName() : "";
            }

            @Override
            public File fromString(String string) {
                return selectedFiles.stream()
                        .filter(f -> f.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        fileSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                previewFile(newVal);
            }
        });
    }

    // --- Public API (test-friendly) ---

    public void setOutputDir(String path) {
        dirField.setText(path);
    }

    public void setSelectedFiles(List<File> files) {
        selectedFiles.clear();
        selectedFiles.addAll(files);
        runIfFxThread(() -> {
            updateFileListUI();
            if (dirField.getText().isBlank()) {
                setDefaultOutputDir();
            }
            updatePreviewTable();
            updateConvertButtonState();
        });
    }

    public String getLastAlertMessage() {
        if (feedbackBar != null && feedbackBar.isVisible()
                && feedbackMessage.getText() != null
                && !feedbackMessage.getText().isBlank()) {
            return feedbackMessage.getText();
        }
        return lastAlertMessage;
    }

    public boolean isConverting() {
        return converting;
    }

    // --- User Actions ---

    @FXML
    private void openGitHub() {
        try {
            new ProcessBuilder("xdg-open", "https://github.com/edurbs").start();
        } catch (Exception ignored) {
            if (Desktop.isDesktopSupported()) {
                try { Desktop.getDesktop().browse(java.net.URI.create("https://github.com/edurbs")); } catch (Exception e) { /* ignore */ }
            }
        }
    }

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
            updateFileListUI();
            if (dirField.getText().isBlank()) {
                setDefaultOutputDir();
            }
            updatePreviewTable();
            updateConvertButtonState();
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

        final Path outputDir = Path.of(dirPath);
        final List<File> files = new ArrayList<>(selectedFiles);

        convertButton.setDisable(true);
        convertButton.setText("Convertendo...");
        converting = true;

        javafx.concurrent.Task<Void> conversionTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                List<String> errors = new ArrayList<>();
                int successCount = 0;

                for (File file : files) {
                    try {
                        List<TransactionRow> rows = ofxParser.parse(file.toPath());
                        String baseName = removeExtension(file.getName());
                        excelWriter.write(rows, outputDir, baseName);
                        successCount++;
                    } catch (Throwable t) {
                        errors.add(file.getName() + ": " + t.getClass().getSimpleName() + " - " + t.getMessage());
                    }
                }

                if (errors.isEmpty()) {
                    updateMessage("success", "success",
                            successCount + " de " + files.size() + " arquivos convertidos com sucesso",
                            null, outputDir);
                } else {
                    updateMessage("error", "error",
                            successCount + " de " + files.size() + " convertido(s), "
                                    + errors.size() + " erro(s)",
                            String.join("\n", errors), null);
                }
                return null;
            }

            private void updateMessage(String type, String icon, String message,
                                       String details, Path dir) {
                javafx.application.Platform.runLater(() -> {
                    showFeedback(type, message, details);
                    if ("success".equals(type) && dir != null) {
                        feedbackAction.setVisible(true);
                        feedbackAction.setManaged(true);
                        lastFeedbackDir = dir;
                    }
                    converting = false;
                    updateConvertButtonState();
                });
            }
        };

        conversionTask.setOnFailed(event -> {
            Throwable t = event.getSource().getException();
            String msg = t != null ? t.getClass().getSimpleName() + ": " + t.getMessage() : "Erro desconhecido";
            javafx.application.Platform.runLater(() -> {
                showFeedback("error", "Erro na convers\u00e3o", msg);
                converting = false;
                updateConvertButtonState();
            });
        });

        new Thread(conversionTask).start();
    }

    // --- Feedback Bar ---

    @FXML
    private void hideFeedback() {
        feedbackBar.setVisible(false);
        feedbackBar.setManaged(false);
        feedbackAction.setVisible(false);
        feedbackAction.setManaged(false);
        feedbackDetails.setVisible(false);
        feedbackDetails.setManaged(false);
        autoHideTimer.stop();
    }

    @FXML
    private void openOutputFolder() {
        if (lastFeedbackDir == null) return;

        boolean opened = false;
        String os = System.getProperty("os.name").toLowerCase();

        try {
            String path = lastFeedbackDir.toAbsolutePath().toString();
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", path).start();
                opened = true;
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", path).start();
                opened = true;
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                new ProcessBuilder("xdg-open", path).start();
                opened = true;
            }
        } catch (Exception ignored) {}

        // Fallback to Desktop API
        if (!opened && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(lastFeedbackDir.toUri());
            } catch (Exception ignored) {}
        }
    }

    private void showFeedback(String type, String message) {
        showFeedback(type, message, null);
    }

    private void showFeedback(String type, String message, String details) {
        autoHideTimer.stop();

        feedbackBar.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackBar.getStyleClass().add("feedback-" + type);

        feedbackIcon.setText("success".equals(type) ? "\u2713" : "\u2717");
        feedbackMessage.setText(message);

        if (details != null && !details.isBlank()) {
            feedbackDetails.setText(details);
            feedbackDetails.setVisible(true);
            feedbackDetails.setManaged(true);
        } else {
            feedbackDetails.setVisible(false);
            feedbackDetails.setManaged(false);
        }

        feedbackBar.setVisible(true);
        feedbackBar.setManaged(true);
        autoHideTimer.playFromStart();
    }

    // --- UI State Helpers ---

    private void updateFileListUI() {
        fileListContainer.getChildren().clear();

        if (selectedFiles.isEmpty()) {
            fileListContainer.setVisible(false);
            fileListContainer.setManaged(false);
            fileCountLabel.setVisible(false);
            fileCountLabel.setManaged(false);
            dropZoneIcon.setVisible(true);
            dropZoneIcon.setManaged(true);
            dropZoneLabel.setText("Arraste seus arquivos OFX aqui ou clique para selecionar");
            dropZoneLabel.setStyle("-fx-text-fill: #6B7280;");
            return;
        }

        // Hide icon, show file chips
        dropZoneIcon.setVisible(false);
        dropZoneIcon.setManaged(false);
        dropZoneLabel.setText(selectedFiles.size() + " arquivo(s) selecionado(s)");
        dropZoneLabel.setStyle("-fx-text-fill: #374151; -fx-font-weight: bold; -fx-font-size: 13px;");

        for (int i = 0; i < selectedFiles.size(); i++) {
            File file = selectedFiles.get(i);
            HBox chip = new HBox();
            chip.getStyleClass().add("file-chip");

            Label nameLabel = new Label(file.getName());
            nameLabel.getStyleClass().add("chip-label");

            Button removeBtn = new Button("\u00d7");
            removeBtn.getStyleClass().add("chip-remove");
            final int index = i;
            removeBtn.setOnAction(e -> {
                selectedFiles.remove(index);
                updateFileListUI();
                if (selectedFiles.isEmpty()) {
                    dirField.setText("");
                }
                updatePreviewTable();
                updateConvertButtonState();
            });

            chip.getChildren().addAll(nameLabel, removeBtn);
            fileListContainer.getChildren().add(chip);
        }

        fileListContainer.setVisible(true);
        fileListContainer.setManaged(true);

        fileCountLabel.setVisible(true);
        fileCountLabel.setManaged(true);
    }

    private void setDefaultOutputDir() {
        if (!selectedFiles.isEmpty()) {
            File parent = selectedFiles.getFirst().getParentFile();
            if (parent != null) {
                dirField.setText(parent.getAbsolutePath());
            }
        }
    }

    private void updateConvertButtonState() {
        boolean hasFiles = !selectedFiles.isEmpty();
        boolean hasDir = dirField.getText() != null && !dirField.getText().isBlank()
                && Files.isDirectory(Path.of(dirField.getText()));

        if (converting) {
            convertButton.setDisable(true);
            convertButton.setText("Convertendo...");
        } else if (hasFiles && hasDir) {
            convertButton.setDisable(false);
            convertButton.setText("Converter (" + selectedFiles.size() + " arquivo" +
                    (selectedFiles.size() == 1 ? "" : "s") + ")");
        } else {
            convertButton.setDisable(false);
            convertButton.setText("Converter");
        }
    }

    private void updatePreviewTable() {
        previewTable.getItems().clear();
        if (selectedFiles.isEmpty()) {
            transactionCountLabel.setText("");
            fileSelector.getItems().clear();
            fileSelector.setVisible(false);
            fileSelector.setManaged(false);
            return;
        }

        // Update file selector
        if (selectedFiles.size() > 1) {
            fileSelector.getItems().setAll(selectedFiles);
            fileSelector.getSelectionModel().selectFirst();
            fileSelector.setVisible(true);
            fileSelector.setManaged(true);
            // previewFile() is called by the selection listener above
        } else {
            fileSelector.getItems().clear();
            fileSelector.setVisible(false);
            fileSelector.setManaged(false);
            previewFile(selectedFiles.getFirst());
        }
    }

    private void previewFile(File file) {
        previewTable.getItems().clear();
        try {
            List<TransactionRow> rows = ofxParser.parse(file.toPath());
            previewTable.getItems().setAll(rows);
            transactionCountLabel.setText(rows.size() + " transa\u00e7\u00e3o" +
                    (rows.size() == 1 ? "" : "\u00f5es"));
            transactionCountLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        } catch (Throwable t) {
            transactionCountLabel.setText("Erro: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            transactionCountLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
        }
    }

    // --- Utilities ---

    private String removeExtension(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
    }

    private void runIfFxThread(Runnable action) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            action.run();
        } else {
            javafx.application.Platform.runLater(action);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        lastAlertMessage = message;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    // --- Custom Cell ---

    private static class NumericCell extends TableCell<TransactionRow, Number> {
        @Override
        protected void updateItem(Number value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setText("");
            } else {
                setText(String.format("%,.2f", value.doubleValue()));
            }
        }
    }
}
