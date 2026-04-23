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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
    void transcodeUtf8ToLatin1ConvertsAccentedChars() {
        // "Fácil" in UTF-8: 46 C3 A1 63 69 6C → transcode to ISO-8859-1: 46 E1 63 69 6C
        String content = "OFXHEADER:100\n<FITID>NONE</FITID>\nFácil";
        byte[] input = content.getBytes(StandardCharsets.UTF_8);
        byte[] result = parser.transcodeUtf8ToLatin1(input);
        // Decode result as ISO-8859-1 (same as OFX4J would) — should get correct "Fácil"
        String decoded = new String(result, StandardCharsets.ISO_8859_1);
        assertTrue(decoded.contains("Fácil"), "Should transcode UTF-8 accented chars to Latin-1");
    }

    @Test
    void transcodeUtf8ToLatin1PreservesNonUtf8() {
        // Byte 0xFE is valid in ISO-8859-1 but not valid UTF-8
        byte[] input = "OFXHEADER:100\n".getBytes(StandardCharsets.UTF_8);
        byte[] withInvalid = new byte[input.length + 1];
        System.arraycopy(input, 0, withInvalid, 0, input.length);
        withInvalid[input.length] = (byte) 0xFE;
        byte[] result = parser.transcodeUtf8ToLatin1(withInvalid);
        assertArrayEquals(withInvalid, result, "Should return original bytes when not valid UTF-8");
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
