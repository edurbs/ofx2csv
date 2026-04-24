package com.mcs.ofx2csv.controller;

import com.mcs.ofx2csv.model.TransactionRow;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(ApplicationExtension.class)
class MainControllerTest {

    @TempDir
    Path tempDir;

    private MainController controller;
    private TextField dirField;

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        dirField = (TextField) root.lookup("#dirField");

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void ui_shows_labels_and_buttons(FxRobot robot) {
        assertNotNull(robot.lookup("#dropZone").query(), "Drop zone should exist");
        assertNotNull(robot.lookup("#dirField").query(), "Directory field should exist");
        assertNotNull(robot.lookup("#convertButton").query(), "Convert button should exist");
        assertNotNull(robot.lookup("#previewTable").query(), "Preview table should exist");

        assertTrue(dirField.getText().isEmpty(), "dirField should be empty initially");
    }

    @Test
    void convert_without_files_shows_warning(FxRobot robot) {
        // Click Converter with no files or directory
        robot.clickOn("#convertButton");

        assertNotNull(controller.getLastAlertMessage());
        assertTrue(controller.getLastAlertMessage().contains("Selecione pelo menos um arquivo OFX"));
    }

    @Test
    void convert_without_directory_shows_warning(FxRobot robot) {
        // Set files (using interact to ensure FX thread processes the update)
        Path fakeFile = Path.of("/nonexistent.ofx");
        robot.interact(() -> controller.setSelectedFiles(List.of(fakeFile.toFile())));
        robot.interact(() -> controller.setOutputDir(""));
        // Wait for async Platform.runLater to execute
        robot.sleep(100);

        robot.clickOn("#convertButton");

        assertNotNull(controller.getLastAlertMessage());
        assertTrue(controller.getLastAlertMessage().contains("Selecione um diretório de destino válido"));
    }

    @Test
    void selecting_files_populates_preview_table(FxRobot robot) throws Exception {
        Path sampleOfx = Path.of("/home/eduardo/Projetos/mcs/ofx2csv/Extrato5981117086.OFX.ofx");
        assumeTrue(Files.exists(sampleOfx), "Sample OFX file not found, skipping");

        robot.interact(() -> controller.setSelectedFiles(List.of(sampleOfx.toFile())));
        robot.sleep(200); // Wait for async preview parsing

        robot.interact(() -> {
            @SuppressWarnings("unchecked")
            TableView<TransactionRow> table = (TableView<TransactionRow>) robot.lookup("#previewTable").query();
            assertTrue(table.getItems().size() > 0,
                    "Preview table should have rows after file selection");
        });
    }

    @Test
    void full_e2e_conversion_from_ofx_to_excel(FxRobot robot) throws Exception {
        Path sampleOfx = Path.of("/home/eduardo/Projetos/mcs/ofx2csv/Extrato5981117086.OFX.ofx");
        assumeTrue(Files.exists(sampleOfx), "Sample OFX file not found, skipping E2E test");

        robot.interact(() -> controller.setSelectedFiles(List.of(sampleOfx.toFile())));
        robot.interact(() -> controller.setOutputDir(tempDir.toString()));
        robot.sleep(200);

        assertFalse(dirField.getText().isBlank());

        // Click Converter
        robot.clickOn("#convertButton");

        // Wait for background conversion to complete
        waitForConversion(robot, 5000);

        // Verify success feedback
        assertNotNull(controller.getLastAlertMessage());
        assertTrue(controller.getLastAlertMessage().contains("convertido"));

        // Verify Excel file was created
        Path outputFile = tempDir.resolve("Extrato5981117086.OFX.xlsx");
        assertTrue(Files.exists(outputFile), "Excel file should exist after conversion");

        // Verify Excel content
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(outputFile.toFile()))) {
            Sheet sheet = workbook.getSheet("Extrato");
            Row header = sheet.getRow(0);
            assertEquals("Data Balancete", header.getCell(0).getStringCellValue());
            assertEquals("Historico", header.getCell(1).getStringCellValue());
            assertEquals("CREDITO", header.getCell(2).getStringCellValue());
            assertEquals("DEBITO", header.getCell(3).getStringCellValue());
            assertEquals("SOMA", header.getCell(4).getStringCellValue());

            assertTrue(sheet.getPhysicalNumberOfRows() > 1, "Should have data rows");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                double credito = row.getCell(2).getNumericCellValue();
                double debito = row.getCell(3).getNumericCellValue();
                double soma = row.getCell(4).getNumericCellValue();

                if (debito != 0 && credito != 0) {
                    fail("Row " + i + ": both DEBITO and CREDITO are non-zero");
                }
                assertEquals(soma, debito + credito, 0.001,
                        "Row " + i + ": SOMA should equal DEBITO + CREDITO");
            }
        }
    }

    private void waitForConversion(FxRobot robot, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String msg = controller.getLastAlertMessage();
            if (msg != null && (msg.contains("convertido") || msg.contains("erro"))) {
                return;
            }
            robot.sleep(100);
        }
        fail("Conversion did not complete within " + timeoutMs + "ms");
    }
}
