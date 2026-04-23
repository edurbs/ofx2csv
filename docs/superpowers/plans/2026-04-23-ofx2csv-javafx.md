# OFX to Excel Converter — JavaFX 25 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite the Python PyQt5 OFX-to-Excel converter as a JavaFX 25 desktop app with DEBITO/CREDITO/SOMA columns instead of Valor/Saldo.

**Architecture:** Simple MVC — JavaFX FXML for the view, service classes for OFX parsing (OFX4J) and Excel writing (Apache POI), a record for transaction rows. Non-modular Gradle project to keep setup simple.

**Tech Stack:** Java 21+, JavaFX 25 (controls + fxml), OFX4J 1.39, Apache POI 5.3.0, TestFX 4.0.18 (E2E UI tests), Gradle

**Spec:** `../ofx2csvPython/SPEC.md` — complete reverse-engineering spec of the Python app

---

### Task 1: Gradle Project Scaffold

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`

- [ ] **Step 1: Create `settings.gradle`**

```groovy
rootProject.name = 'ofx2csv'
```

- [ ] **Step 2: Create `build.gradle`**

```groovy
plugins {
    id 'application'
    id 'java'
}

group = 'com.mcs'
version = '1.0'

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass = 'com.mcs.ofx2csv.Ofx2CsvApp'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.openjfx:javafx-controls:25'
    implementation 'org.openjfx:javafx-fxml:25'

    implementation 'com.webcohesion.ofx4j:ofx4j:1.39'
    implementation 'org.apache.poi:poi-ooxml:5.3.0'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'

    // TestFX for E2E UI testing
    testImplementation 'org.testfx:testfx-core:4.0.18'
    testImplementation 'org.testfx:testfx-junit5:4.0.18'
    testImplementation 'org.assertj:assertj-core:3.26.3'
}

test {
    useJUnitPlatform()

    // Required for TestFX headless UI tests
    if (System.getenv('CI') != null || System.getProperty('java.awt.headless') != null) {
        jvmArgs += [
                '--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED',
                '--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED'
        ]
    }
}
```

- [ ] **Step 3: Verify build resolves**

Run: `cd /home/eduardo/Projetos/mcs/ofx2csv && ./gradlew dependencies --configuration compileClasspath 2>&1 | head -40`
Expected: Dependencies resolve without errors. If Gradle wrapper doesn't exist, run `gradle wrapper` first.

- [ ] **Step 4: Initialize git and commit**

```bash
cd /home/eduardo/Projetos/mcs/ofx2csv
git init
echo "build/\n.gradle/\n*.class\n*.xlsx\n*.~lock*" > .gitignore
git add build.gradle settings.gradle .gitignore
git commit -m "chore: scaffold Gradle project with JavaFX 25, OFX4J, Apache POI"
```

---

### Task 2: TransactionRow Model

**Files:**
- Create: `src/main/java/com/mcs/ofx2csv/model/TransactionRow.java`
- Create: `src/test/java/com/mcs/ofx2csv/model/TransactionRowTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/main/java/com/mcs/ofx2csv/model/TransactionRow.java`:

```java
package com.mcs.ofx2csv.model;

import java.time.LocalDate;

/**
 * Represents a single row in the output Excel file.
 *
 * @param date       Transaction date (DD/MM/YYYY in output)
 * @param historico  Combined NAME + MEMO from OFX
 * @param debito     Negative amount (money out), or 0
 * @param credito    Positive amount (money in), or 0
 * @param soma       debito + credito
 */
public record TransactionRow(LocalDate date, String historico, double debito, double credito, double soma) {

    /**
     * Factory method that splits the OFX transaction amount into debito/credito.
     * Negative amounts go to debito, positive to credito. The other column is zero.
     */
    public static TransactionRow fromOfx(LocalDate date, String name, String memo, double amount) {
        String historico = buildHistorico(name, memo);
        double debito = amount < 0 ? amount : 0;
        double credito = amount > 0 ? amount : 0;
        double soma = debito + credito;
        return new TransactionRow(date, historico, debito, credito, soma);
    }

    private static String buildHistorico(String name, String memo) {
        if (name == null || name.isBlank()) {
            return memo != null ? memo : "";
        }
        if (memo == null || memo.isBlank()) {
            return name;
        }
        return name + " - " + memo;
    }
}
```

Create `src/test/java/com/mcs/ofx2csv/model/TransactionRowTest.java`:

```java
package com.mcs.ofx2csv.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransactionRowTest {

    @Test
    void negativeAmountGoesToDebito() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "Payee", "Memo text", -150.00);

        assertEquals(-150.00, row.debito(), 0.001);
        assertEquals(0.0, row.credito(), 0.001);
        assertEquals(-150.00, row.soma(), 0.001);
    }

    @Test
    void positiveAmountGoesToCredito() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "Payee", "Memo text", 500.50);

        assertEquals(0.0, row.debito(), 0.001);
        assertEquals(500.50, row.credito(), 0.001);
        assertEquals(500.50, row.soma(), 0.001);
    }

    @Test
    void zeroAmountGivesBothZero() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "Payee", "Memo", 0.0);

        assertEquals(0.0, row.debito(), 0.001);
        assertEquals(0.0, row.credito(), 0.001);
        assertEquals(0.0, row.soma(), 0.001);
    }

    @Test
    void historicoCombinesNameAndMemo() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "JOAO SILVA", "PIX transfer", -100.0);

        assertEquals("JOAO SILVA - PIX transfer", row.historico());
    }

    @Test
    void historicoMemoOnlyWhenNameIsEmpty() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), "", "PIX - RECEBIDO", 200.0);

        assertEquals("PIX - RECEBIDO", row.historico());
    }

    @Test
    void historicoMemoOnlyWhenNameIsNull() {
        TransactionRow row = TransactionRow.fromOfx(
                LocalDate.of(2024, 3, 15), null, "PIX - RECEBIDO", 200.0);

        assertEquals("PIX - RECEBIDO", row.historico());
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew test --tests "com.mcs.ofx2csv.model.TransactionRowTest" -i`
Expected: All 6 tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/mcs/ofx2csv/model/ src/test/java/com/mcs/ofx2csv/model/
git commit -m "feat: add TransactionRow model with debito/credito/soma splitting"
```

---

### Task 3: OFX Parser Service

**Files:**
- Create: `src/main/java/com/mcs/ofx2csv/service/OfxParser.java`
- Create: `src/test/java/com/mcs/ofx2csv/service/OfxParserTest.java`

- [ ] **Step 1: Write the OFX parser implementation**

Create `src/main/java/com/mcs/ofx2csv/service/OfxParser.java`:

```java
package com.mcs.ofx2csv.service;

import com.mcs.ofx2csv.model.TransactionRow;
import com.webcohesion.ofx4j.domain.data.MessageSetType;
import com.webcohesion.ofx4j.domain.data.ResponseEnvelope;
import com.webcohesion.ofx4j.domain.data.banking.BankStatementResponse;
import com.webcohesion.ofx4j.domain.data.banking.BankStatementResponseTransaction;
import com.webcohesion.ofx4j.domain.data.banking.BankingResponseMessageSet;
import com.webcohesion.ofx4j.domain.data.common.Transaction;
import com.webcohesion.ofx4j.domain.data.common.TransactionList;
import com.webcohesion.ofx4j.io.AggregateUnmarshaller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses Brazilian bank OFX files into TransactionRow lists using OFX4J.
 * Handles empty FITID tag sanitization and encoding via byte-level processing.
 */
public class OfxParser {

    // Matches <FITID>...</FITID> where content is whitespace-only (empty)
    private static final Pattern EMPTY_FITID = Pattern.compile(
            "<FITID>\\s*</FITID>", Pattern.CASE_INSENSITIVE);

    /**
     * Parses an OFX file into a list of TransactionRows.
     *
     * @param filePath path to the .ofx file
     * @return list of transaction rows, never null
     * @throws IOException if the file cannot be read or parsed
     */
    public List<TransactionRow> parse(Path filePath) throws IOException {
        byte[] raw = Files.readAllBytes(filePath);
        byte[] sanitized = sanitizeEmptyFitid(raw);
        return unmarshal(new ByteArrayInputStream(sanitized));
    }

    /**
     * Replaces empty FITID tags with <FITID>NONE</FITID> at the byte level.
     * Some Brazilian banks (Banco do Brasil) export balance markers with empty FITID.
     */
    byte[] sanitizeEmptyFitid(byte[] raw) {
        String content = new String(raw, StandardCharsets.ISO_8859_1);
        String sanitized = EMPTY_FITID.matcher(content).replaceAll("<FITID>NONE</FITID>");
        return sanitized.getBytes(StandardCharsets.ISO_8859_1);
    }

    private List<TransactionRow> unmarshal(ByteArrayInputStream input) throws IOException {
        AggregateUnmarshaller<ResponseEnvelope> unmarshaller =
                new AggregateUnmarshaller<>(ResponseEnvelope.class);
        ResponseEnvelope envelope = unmarshaller.unmarshal(input);

        BankingResponseMessageSet banking = (BankingResponseMessageSet)
                envelope.getMessageSet(MessageSetType.banking);
        if (banking == null || banking.getStatementResponses() == null
                || banking.getStatementResponses().isEmpty()) {
            return List.of();
        }

        BankStatementResponseTransaction txn = banking.getStatementResponses().get(0);
        BankStatementResponse statement = txn.getMessage();
        if (statement == null || statement.getTransactionList() == null) {
            return List.of();
        }

        TransactionList txnList = statement.getTransactionList();
        if (txnList.getTransactions() == null) {
            return List.of();
        }

        List<TransactionRow> rows = new ArrayList<>();
        for (Object obj : txnList.getTransactions()) {
            Transaction t = (Transaction) obj;
            LocalDate date = t.getDatePosted().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            rows.add(TransactionRow.fromOfx(date, t.getName(), t.getMemo(), t.getAmount()));
        }
        return rows;
    }
}
```

- [ ] **Step 2: Write the test**

Create `src/test/java/com/mcs/ofx2csv/service/OfxParserTest.java`:

```java
package com.mcs.ofx2csv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.mcs.ofx2csv.model.TransactionRow;

import static org.junit.jupiter.api.Assertions.*;

class OfxParserTest {

    @TempDir
    Path tempDir;

    private final OfxParser parser = new OfxParser();

    @Test
    void sanitizeEmptyFitidReplacesEmptyTag() {
        byte[] input = "<FITID></FITID>".getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = parser.sanitizeEmptyFitid(input);
        assertEquals("<FITID>NONE</FITID>", new String(result, StandardCharsets.ISO_8859_1));
    }

    @Test
    void sanitizeEmptyFitidReplacesWhitespaceOnlyTag() {
        byte[] input = "<FITID>  \n  </FITID>".getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = parser.sanitizeEmptyFitid(input);
        assertEquals("<FITID>NONE</FITID>", new String(result, StandardCharsets.ISO_8859_1));
    }

    @Test
    void sanitizeEmptyFitidPreservesNonEmptyTag() {
        byte[] input = "<FITID>12345</FITID>".getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = parser.sanitizeEmptyFitid(input);
        assertEquals("<FITID>12345</FITID>", new String(result, StandardCharsets.ISO_8859_1));
    }

    @Test
    void parseRealOfxFile() throws IOException {
        Path ofxFile = Path.of("/home/eduardo/Projetos/mcs/ofx2csv/Extrato5981117086.OFX.ofx");
        if (!Files.exists(ofxFile)) {
            return; // skip if sample file not present
        }

        List<TransactionRow> rows = parser.parse(ofxFile);

        assertFalse(rows.isEmpty(), "Should parse transactions from sample OFX file");
        TransactionRow first = rows.getFirst();
        assertNotNull(first.date());
        assertFalse(first.historico().isBlank());
    }
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew test --tests "com.mcs.ofx2csv.service.OfxParserTest" -i`
Expected: All 4 tests PASS. If the sample OFX parsing fails, debug the OFX4J navigation chain — the key thing to verify is the exact class structure in the 1.39 jar.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mcs/ofx2csv/service/ src/test/java/com/mcs/ofx2csv/service/
git commit -m "feat: add OfxParser service with FITID sanitization"
```

---

### Task 4: Excel Writer Service

**Files:**
- Create: `src/main/java/com/mcs/ofx2csv/service/ExcelWriter.java`
- Create: `src/test/java/com/mcs/ofx2csv/service/ExcelWriterTest.java`

- [ ] **Step 1: Write the Excel writer implementation**

Create `src/main/java/com/mcs/ofx2csv/service/ExcelWriter.java`:

```java
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
```

- [ ] **Step 2: Write the test**

Create `src/test/java/com/mcs/ofx2csv/service/ExcelWriterTest.java`:

```java
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
            assertEquals("DEBITO", header.getCell(2).getStringCellValue());
            assertEquals("CREDITO", header.getCell(3).getStringCellValue());
            assertEquals("SOMA", header.getCell(4).getStringCellValue());

            // Verify first data row (negative amount → debito)
            Row row1 = sheet.getRow(1);
            assertEquals("15/03/2024", row1.getCell(0).getStringCellValue());
            assertEquals("JOAO - PIX", row1.getCell(1).getStringCellValue());
            assertEquals(-150.0, row1.getCell(2).getNumericCellValue(), 0.001);
            assertEquals(0.0, row1.getCell(3).getNumericCellValue(), 0.001);
            assertEquals(-150.0, row1.getCell(4).getNumericCellValue(), 0.001);

            // Verify second data row (positive amount → credito)
            Row row2 = sheet.getRow(2);
            assertEquals("16/03/2024", row2.getCell(0).getStringCellValue());
            assertEquals("MARIA - Transfer", row2.getCell(1).getStringCellValue());
            assertEquals(0.0, row2.getCell(2).getNumericCellValue(), 0.001);
            assertEquals(500.5, row2.getCell(3).getNumericCellValue(), 0.001);
            assertEquals(500.5, row2.getCell(4).getNumericCellValue(), 0.001);
        }
    }
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew test --tests "com.mcs.ofx2csv.service.ExcelWriterTest" -i`
Expected: All 1 test PASS — verifies columns, header, date format, debito/credito splitting.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mcs/ofx2csv/service/ExcelWriter.java src/test/java/com/mcs/ofx2csv/service/ExcelWriterTest.java
git commit -m "feat: add ExcelWriter service with Apache POI"
```

---

### Task 5: FXML Layout + Main Controller

**Files:**
- Create: `src/main/resources/main.fxml`
- Create: `src/main/java/com/mcs/ofx2csv/Ofx2CsvApp.java`
- Create: `src/main/java/com/mcs/ofx2csv/controller/MainController.java`

- [ ] **Step 1: Create the FXML layout**

Create `src/main/resources/main.fxml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.layout.VBox?>

<VBox spacing="10" xmlns="http://javafx.com/javafx"
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.mcs.ofx2csv.controller.MainController"
      alignment="CENTER_LEFT">
    <padding>
        <Insets top="20" right="20" bottom="20" left="20"/>
    </padding>

    <Label text="Arquivo a converter"/>
    <TextField fx:id="fileField" editable="false" prefWidth="400"/>
    <Button text="..." onAction="#pickFiles"/>

    <Label text="Salvar em..."/>
    <TextField fx:id="dirField" editable="false" prefWidth="400"/>
    <Button text="..." onAction="#pickDirectory"/>

    <Button text="Converter" onAction="#convert" prefWidth="200"/>
</VBox>
```

- [ ] **Step 2: Create the Application entry point**

Create `src/main/java/com/mcs/ofx2csv/Ofx2CsvApp.java`:

```java
package com.mcs.ofx2csv;

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
        stage.setTitle("Conversor OFX - Excel");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

- [ ] **Step 3: Create the FXML controller**

Create `src/main/java/com/mcs/ofx2csv/controller/MainController.java`:

```java
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
import java.io.IOException;
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
```

- [ ] **Step 4: Build the full project**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL. If there are module access issues with FXML, we may need to add `--add-opens` JVM args or switch to non-modular.

- [ ] **Step 5: Run the application**

Run: `./gradlew run`
Expected: JavaFX window opens with title "Conversor OFX - Excel", showing the file picker, directory picker, and Convert button.

- [ ] **Step 6: Test end-to-end with sample OFX file**

1. Click first "..." — select `Extrato5981117086.OFX.ofx`
2. Click second "..." — select an output directory
3. Click "Converter"
4. Open the generated .xlsx file and verify:
   - 5 columns: Data Balancete, Historico, DEBITO, CREDITO, SOMA
   - Negative amounts in DEBITO, positive in CREDITO
   - Dates in DD/MM/YYYY

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/mcs/ofx2csv/ src/main/java/com/mcs/ofx2csv/controller/ src/main/resources/
git commit -m "feat: add JavaFX FXML UI with file pickers and convert button"
```

---

### Task 6: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update CLAUDE.md with verified build/test/run commands and actual project structure**

Read `CLAUDE.md` and update it with the actual verified build commands, class paths, and any adjustments discovered during implementation (e.g., JVM args needed for FXML, exact dependency versions that resolved).

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md with verified project structure"
```

---

### Task 7: TestFX E2E UI Tests

**Files:**
- Create: `src/test/java/com/mcs/ofx2csv/controller/MainControllerTest.java`

**Design decision:** `FileChooser` and `DirectoryChooser` open OS-native dialogs that TestFX cannot drive. The controller exposes `setSelectedFiles()` and `setOutputDir()` methods to inject paths programmatically in tests. The test verifies the UI state (text fields update correctly) and the full conversion pipeline (OFX file → Excel output).

- [ ] **Step 1: Write the TestFX E2E test**

Create `src/test/java/com/mcs/ofx2csv/controller/MainControllerTest.java`:

```java
package com.mcs.ofx2csv.controller;

import com.mcs.ofx2csv.Ofx2CsvApp;
import com.mcs.ofx2csv.model.TransactionRow;
import com.mcs.ofx2csv.service.ExcelWriter;
import com.mcs.ofx2csv.service.OfxParser;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import org.testfx.assertions.api.Assertions;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.File;
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
    private Button convertButton;

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        fileField = (TextField) root.lookup("#fileField");
        dirField = (TextField) root.lookup("#dirField");
        convertButton = (Button) root.lookup(".button");

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void ui_shows_labels_and_buttons(FxRobot robot) {
        Assertions.assertThat(robot.lookup(".label").queryLabeled()).hasText("Arquivo a converter");
        Assertions.assertThat(convertButton).hasText("Converter");
    }

    @Test
    void convert_without_files_shows_warning(FxRobot robot) {
        // No files selected, no directory — click Converter
        robot.clickOn(convertButton);

        assertNotNull(controller.getLastAlertMessage());
        assertTrue(controller.getLastAlertMessage().contains("Nenhum arquivo selecionado"));
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
        robot.clickOn(convertButton);

        // Verify success alert
        assertNotNull(controller.getLastAlertMessage());
        assertTrue(controller.getLastAlertMessage().contains("convertido"));

        // Verify Excel file was created
        Path outputFile = tempDir.resolve("Extrato5981117086.OFX.xlsx");
        assertTrue(Files.exists(outputFile), "Excel file should exist after conversion");

        // Verify Excel content — read back and assert columns and data
        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(outputFile.toFile()))) {
            Sheet sheet = workbook.getSheet("Extrato");
            Row header = sheet.getRow(0);
            assertEquals("Data Balancete", header.getCell(0).getStringCellValue());
            assertEquals("Historico", header.getCell(1).getStringCellValue());
            assertEquals("DEBITO", header.getCell(2).getStringCellValue());
            assertEquals("CREDITO", header.getCell(3).getStringCellValue());
            assertEquals("SOMA", header.getCell(4).getStringCellValue());

            // Verify at least one data row exists
            assertTrue(sheet.getPhysicalNumberOfRows() > 1, "Should have data rows");

            // Verify each data row: exactly one of debito/credito is non-zero
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                double debito = row.getCell(2).getNumericCellValue();
                double credito = row.getCell(3).getNumericCellValue();
                double soma = row.getCell(4).getNumericCellValue();

                // XOR: exactly one should be non-zero (or both zero for zero-amount txns)
                if (debito != 0 && credito != 0) {
                    fail("Row " + i + ": both DEBITO and CREDITO are non-zero");
                }
                assertEquals(soma, debito + credito, 0.001,
                        "Row " + i + ": SOMA should equal DEBITO + CREDITO");
            }
        }
    }
}
```

- [ ] **Step 2: Run the TestFX tests**

Run: `./gradlew test --tests "com.mcs.ofx2csv.controller.MainControllerTest" -i`
Expected: All 3 tests PASS. The E2E test may need `--add-opens` JVM args or `java.awt.headless=true` if running without a display.

If tests fail with `java.awt.HeadlessException`, add to `build.gradle` test block:
```groovy
jvmArgs += '-Djava.awt.headless=true'
jvmArgs += '--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED'
jvmArgs += '--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED'
```

If tests fail with `IllegalStateException: No suitable driver found`, the CI environment needs a virtual display (Xvfb). For local development, tests should run fine with a display.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/mcs/ofx2csv/controller/MainControllerTest.java
git commit -m "test: add TestFX E2E tests for UI and full conversion pipeline"
```

---

## Verification Checklist

- [ ] `./gradlew build` succeeds
- [ ] `./gradlew test` — all tests pass (TransactionRow, OfxParser, ExcelWriter, MainController)
- [ ] `./gradlew run` — GUI opens
- [ ] TestFX E2E test passes: UI loads with labels, converter button works, full OFX→Excel pipeline succeeds
- [ ] Sample OFX file converts successfully to .xlsx with correct columns
- [ ] Negative amounts appear in DEBITO column, positive in CREDITO
- [ ] SOMA column equals DEBITO + CREDITO per row
- [ ] Dates formatted DD/MM/YYYY
- [ ] Historico combines NAME + MEMO with " - " separator
