package com.mcs.ofx2csv;

import com.mcs.ofx2csv.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Ofx2CsvApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();

        stage.setTitle("Conversor OFX \u2192 Excel");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setWidth(750);
        stage.setHeight(550);
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
