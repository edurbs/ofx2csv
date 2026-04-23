package com.mcs.ofx2csv.controller;

import com.mcs.ofx2csv.model.TransactionRow;
import com.mcs.ofx2csv.service.ExcelWriter;
import com.mcs.ofx2csv.service.OfxParser;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
    private TextField fileField;
    private TextField dirField;

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        fileField = (TextField) root.lookup("#fileField");
        dirField = (TextField) root.lookup("#dirField");

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void ui_shows_labels_and_buttons(FxRobot robot) {
        assertTrue(fileField.getText().isEmpty(), "fileField should be empty initially");
        assertTrue(dirField.getText().isEmpty(), "dirField should be empty initially");
    }

    @Test
    void convert_without_files_shows_warning(FxRobot robot) {
        // No files selected, no directory -- click Converter
        robot.clickOn("#convertButton");

        assertNotNull(controller.getLastAlertMessage());
        assertTrue(controller.getLastAlertMessage().contains("Selecione pelo menos um arquivo OFX"));
    }

    @Test
    void convert_without_directory_shows_warning(FxRobot robot) {
        // Set files but no directory
        controller.setSelectedFiles(List.of(Path.of("/nonexistent.ofx").toFile()));

        robot.clickOn("#convertButton");

        assertNotNull(controller.getLastAlertMessage());
        assertTrue(controller.getLastAlertMessage().contains("Selecione um diretório de destino válido"));
    }

    @Test
    void full_e2e_conversion_from_ofx_to_excel(FxRobot robot) throws Exception {
        Path sampleOfx = Path.of("/home/eduardo/Projetos/mcs/ofx2csv/Extrato5981117086.OFX.ofx");
        assumeTrue(Files.exists(sampleOfx), "Sample OFX file not found, skipping E2E test");

        // Inject files and output directory programmatically
        controller.setSelectedFiles(List.of(sampleOfx.toFile()));
        controller.setOutputDir(tempDir.toString());

        // Verify text fields updated
        assertFalse(fileField.getText().isBlank());
        assertFalse(dirField.getText().isBlank());

        // Click Converter
        robot.clickOn("#convertButton");

        // Verify success alert
        assertNotNull(controller.getLastAlertMessage());
        assertTrue(controller.getLastAlertMessage().contains("convertido"));

        // Verify Excel file was created
        Path outputFile = tempDir.resolve("Extrato5981117086.OFX.xlsx");
        assertTrue(Files.exists(outputFile), "Excel file should exist after conversion");

        // Verify Excel content -- read back and assert columns and data
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(outputFile.toFile()))) {
            Sheet sheet = workbook.getSheet("Extrato");
            Row header = sheet.getRow(0);
            assertEquals("Data Balancete", header.getCell(0).getStringCellValue());
            assertEquals("Historico", header.getCell(1).getStringCellValue());
            assertEquals("CREDITO", header.getCell(2).getStringCellValue());
            assertEquals("DEBITO", header.getCell(3).getStringCellValue());
            assertEquals("SOMA", header.getCell(4).getStringCellValue());

            // Verify at least one data row exists
            assertTrue(sheet.getPhysicalNumberOfRows() > 1, "Should have data rows");

            // Verify each data row: exactly one of debito/credito is non-zero
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
}
