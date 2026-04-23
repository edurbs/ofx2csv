package com.mcs.ofx2csv.service;

import com.mcs.ofx2csv.model.TransactionRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelWriterTest {

    @TempDir
    Path tempDir;

    private final ExcelWriter writer = new ExcelWriter();

    @Test
    void filtersOutZeroAmountRows() throws Exception {
        List<TransactionRow> rows = List.of(
                TransactionRow.fromOfx(LocalDate.of(2024, 3, 15), "JOAO", "PIX", -150.00),
                TransactionRow.fromOfx(LocalDate.of(2024, 3, 15), "BALANCE", "Marker", 0.0),
                TransactionRow.fromOfx(LocalDate.of(2024, 3, 16), "MARIA", "Transfer", 500.50)
        );

        writer.write(rows, tempDir, "test_zero_filter");

        Path outputFile = tempDir.resolve("test_zero_filter.xlsx");
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(outputFile.toFile()))) {
            Sheet sheet = workbook.getSheet("Extrato");
            // Header + 2 data rows (zero-amount row filtered out)
            assertEquals(3, sheet.getPhysicalNumberOfRows(), "Should have header + 2 data rows");
        }
    }

    @Test
    void writesExcelFileWithCorrectColumns() throws Exception {
        List<TransactionRow> rows = List.of(
                TransactionRow.fromOfx(LocalDate.of(2024, 3, 15), "JOAO", "PIX", -150.00),
                TransactionRow.fromOfx(LocalDate.of(2024, 3, 16), "MARIA", "Transfer", 500.50)
        );

        writer.write(rows, tempDir, "test_output");

        Path outputFile = tempDir.resolve("test_output.xlsx");
        assertTrue(Files.exists(outputFile), "Excel file should be created");

        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(outputFile.toFile()))) {
            Sheet sheet = workbook.getSheet("Extrato");

            // Verify header
            Row header = sheet.getRow(0);
            assertEquals("Data Balancete", header.getCell(0).getStringCellValue());
            assertEquals("Historico", header.getCell(1).getStringCellValue());
            assertEquals("CREDITO", header.getCell(2).getStringCellValue());
            assertEquals("DEBITO", header.getCell(3).getStringCellValue());
            assertEquals("SOMA", header.getCell(4).getStringCellValue());

            // Verify first data row (negative amount -> debito)
            Row row1 = sheet.getRow(1);
            assertEquals("15/03/2024", row1.getCell(0).getStringCellValue());
            assertEquals("JOAO - PIX", row1.getCell(1).getStringCellValue());
            assertEquals(0.0, row1.getCell(2).getNumericCellValue(), 0.001);
            assertEquals(150.0, row1.getCell(3).getNumericCellValue(), 0.001);
            assertEquals(150.0, row1.getCell(4).getNumericCellValue(), 0.001);

            // Verify second data row (positive amount -> credito)
            Row row2 = sheet.getRow(2);
            assertEquals("16/03/2024", row2.getCell(0).getStringCellValue());
            assertEquals("MARIA - Transfer", row2.getCell(1).getStringCellValue());
            assertEquals(500.5, row2.getCell(2).getNumericCellValue(), 0.001);
            assertEquals(0.0, row2.getCell(3).getNumericCellValue(), 0.001);
            assertEquals(500.5, row2.getCell(4).getNumericCellValue(), 0.001);
        }
    }
}
