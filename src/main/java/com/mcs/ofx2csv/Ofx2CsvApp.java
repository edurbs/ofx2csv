package com.mcs.ofx2csv;

import com.mcs.ofx2csv.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class Ofx2CsvApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();

        // Handle files passed via command line (Windows file association / drag-drop)
        List<String> args = getParameters().getRaw();
        List<File> filesFromArgs = args.stream()
                .map(File::new)
                .filter(File::exists)
                .filter(f -> f.getName().toLowerCase().endsWith(".ofx"))
                .toList();
        if (!filesFromArgs.isEmpty()) {
            controller.setSelectedFiles(filesFromArgs);
        }

        stage.setTitle("Conversor OFX \u2192 Excel");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setWidth(750);
        stage.setHeight(700);
        stage.setResizable(false);
        stage.centerOnScreen();

        stage.setOnCloseRequest(event -> {
            if (controller.isConverting()) {
                event.consume();
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
