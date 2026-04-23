package com.mcs.ofx2csv.service;

import com.mcs.ofx2csv.model.TransactionRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes TransactionRow lists to .xlsx files using Apache POI.
 */
public class ExcelWriter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] HEADERS = {
            "Data Balancete", "Historico", "DEBITO", "CREDITO", "SOMA"
    };

    /**
     * Writes transaction rows to an Excel file.
     *
     * @param rows         transaction data to write
     * @param outputDir    directory to write the file into
     * @param baseFileName filename without extension (e.g., "Extrato5981117086.OFX")
     * @throws IOException if writing fails
     */
    public void write(List<TransactionRow> rows, Path outputDir, String baseFileName) throws IOException {
        Path outputFile = outputDir.resolve(baseFileName + ".xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Extrato");

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(HEADERS[i]);
            }

            // Data rows
            int rowIdx = 1;
            for (TransactionRow tr : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tr.date().format(DATE_FORMAT));
                row.createCell(1).setCellValue(tr.historico());
                row.createCell(2).setCellValue(tr.debito());
                row.createCell(3).setCellValue(tr.credito());
                row.createCell(4).setCellValue(tr.soma());
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
                workbook.write(fos);
            }
        }
    }
}
