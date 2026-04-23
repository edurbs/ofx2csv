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
