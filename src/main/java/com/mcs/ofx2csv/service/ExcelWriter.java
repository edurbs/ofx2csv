package com.mcs.ofx2csv.service;

import com.mcs.ofx2csv.model.TransactionRow;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
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
            "Data Balancete", "Historico", "CREDITO", "DEBITO"
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

            // Currency style for CREDITO, DEBITO, SOMA columns
            DataFormat fmt = workbook.createDataFormat();
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(fmt.getFormat("_-\"R$\" * #,##0.00_-;_-\"R$\" * (#,##0.00)_-;_-\"R$\" * \"-\"??_-;_-@_-"));

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(HEADERS[i]);
            }

            // Data rows (skip zero-amount transactions)
            List<TransactionRow> filtered = rows.stream()
                    .filter(tr -> tr.debito() != 0 || tr.credito() != 0)
                    .toList();
            int rowIdx = 1;
            for (TransactionRow tr : filtered) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tr.date().format(DATE_FORMAT));
                row.createCell(1).setCellValue(tr.historico());
                var cCred = row.createCell(2);
                cCred.setCellValue(tr.credito());
                cCred.setCellStyle(currencyStyle);
                var cDeb = row.createCell(3);
                cDeb.setCellValue(tr.debito());
                cDeb.setCellStyle(currencyStyle);
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
