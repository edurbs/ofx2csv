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
import com.webcohesion.ofx4j.io.OFXParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
        byte[] corrected = transcodeUtf8ToLatin1(sanitized);
        try {
            return unmarshal(new ByteArrayInputStream(corrected));
        } catch (OFXParseException e) {
            throw new IOException("Failed to parse OFX file: " + filePath, e);
        }
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

    /**
     * Detects if the OFX content is actually UTF-8 and transcodes to ISO-8859-1.
     * Brazilian banks commonly declare ENCODING:USASCII / CHARSET:1252 but
     * encode the content in UTF-8. OFX4J uses the declared charset to decode,
     * so we transcode UTF-8 multi-byte sequences to single-byte Latin-1 equivalents.
     */
    byte[] transcodeUtf8ToLatin1(byte[] ofxBytes) {
        String asUtf8 = new String(ofxBytes, StandardCharsets.UTF_8);
        byte[] roundTripped = asUtf8.getBytes(StandardCharsets.UTF_8);
        if (java.util.Arrays.equals(ofxBytes, roundTripped)) {
            return asUtf8.getBytes(StandardCharsets.ISO_8859_1);
        }
        return ofxBytes;
    }

    private List<TransactionRow> unmarshal(ByteArrayInputStream input) throws IOException, OFXParseException {
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
                    .atZone(ZoneOffset.UTC).toLocalDate();
            rows.add(TransactionRow.fromOfx(date, t.getName(), t.getMemo(), t.getAmount()));
        }
        return rows;
    }
}
