# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaFX 25 desktop application that converts Brazilian bank OFX statement files (.ofx) into Excel spreadsheets (.xlsx). This is a rewrite of an existing Python/PyQt5 application whose source lives in `../ofx2csvPython/`. The Python source includes a comprehensive spec at `../ofx2csvPython/SPEC.md`.

## Source Reference

- **Python source**: `../ofx2csvPython/Conversor de OFX/` — the application being rewritten
- **Full reverse-engineering spec**: `../ofx2csvPython/SPEC.md` — read this first for complete behavioral details
- **Sample OFX file**: `Extrato5981117086.OFX.ofx` (Banco do Brasil format)
- **Expected output reference**: `Extrato5981117086.OFX ok.xlsx`

## Build & Run

```bash
# Build (from project root)
./gradlew build

# Run the application
./gradlew run

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.mcs.ofx2csv.controller.MainControllerTest"
```

## Architecture

### Core Conversion Pipeline

```
GUI (JavaFX) → Service Layer → OFX Parser → Transaction Processor → Excel Writer
```

### OFX Parsing — Critical Compatibility Rules

1. **Empty FITID sanitization**: Brazilian banks (notably Banco do Brasil) export balance marker transactions with empty `<FITID></FITID>` tags. The OFX spec requires non-empty FITID. The parser fixes this with regex on raw bytes (replace empty FITID content with `NONE`) before passing to OFX4J's `AggregateUnmarshaller`. See `OfxParser.java`.

2. **OFX4J library**: Uses `AggregateUnmarshaller` with `NanoXMLOFXReader` to parse OFX streams. Navigation: `ResponseEnvelope` → `BankingResponseMessageSet` → `BankStatementResponseTransaction` → `BankStatementResponse` → `TransactionList` → `Transaction`.

3. **Encoding**: OFX files declare encoding in headers (`ENCODING:UTF-8`, etc.). Files must be read as raw bytes — never as text — so OFX4J can decode correctly from the declared encoding.

### Excel Output Columns

| Column | Source | Format |
|--------|--------|--------|
| Data Balancete | DTPOSTED | DD/MM/YYYY |
| Historico | NAME + " - " + MEMO | String (NAME omitted if empty) |
| DEBITO | TRNAMT if < 0, else 0 | Decimal |
| CREDITO | TRNAMT if > 0, else 0 | Decimal |
| SOMA | DEBITO + CREDITO | Decimal |

Output filename: `{destination}/{original_filename_without_extension}.xlsx`

Note: This differs from the Python app which uses "Valor" and "Saldo" columns. The Java version splits amounts into debit/credit.

### Data Model

```
TransactionRow (record)
  ├── date      (LocalDate)
  ├── historico (String)
  ├── debito    (double)
  ├── credito   (double)
  └── soma      (double)

TransactionRow.fromOfx(date, name, memo, amount) — factory method that:
  - Combines name + memo into historico
  - Splits amount into debito/credito
  - Computes soma = debito + credito
```

## Domain Notes

- Application is Brazilian-locale: Portuguese column names, DD/MM/YYYY dates
- Only the first account in multi-account OFX files is processed
- No CSV output — only .xlsx despite the project name
- UI tests use TestFX with headless mode; controller exposes `setSelectedFiles()`, `setOutputDir()`, and `getLastAlertMessage()` for test injection
