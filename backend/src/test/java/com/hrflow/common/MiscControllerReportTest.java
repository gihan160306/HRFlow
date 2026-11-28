package com.hrflow.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class MiscControllerReportTest {

    @Test
    void csvTextEscapesCommasQuotesNewlinesAndGuardsFormulaInjection() {
        assertEquals("Phòng Nội dung", MiscController.csvText("Phòng Nội dung"));
        assertEquals("\"Content, Design\"", MiscController.csvText("Content, Design"));
        assertEquals("\"Say \"\"Hello\"\"\"", MiscController.csvText("Say \"Hello\""));
        assertEquals("\"Line1\nLine2\"", MiscController.csvText("Line1\nLine2"));

        assertEquals("'=SUM(A1:A2)", MiscController.csvText("=SUM(A1:A2)"));
        assertEquals("'+1+1", MiscController.csvText("+1+1"));
        assertEquals("'-cmd|'/C calc'!A0", MiscController.csvText("-cmd|'/C calc'!A0"));
        assertEquals("'@SUM(1+1)", MiscController.csvText("@SUM(1+1)"));
        assertEquals("'\t=1+1", MiscController.csvText("\t=1+1"));
        assertEquals("\"'\r=1+1\"", MiscController.csvText("\r=1+1"));
        assertEquals("\"'  =HYPERLINK(\"\"http://evil\"\")\"", MiscController.csvText("  =HYPERLINK(\"http://evil\")"));
        assertEquals("", MiscController.csvText(null));
    }

    @Test
    void csvNumberFormatsExactVndWithoutCurrencySymbolOrRounding() {
        assertEquals("1265633780", MiscController.csvNumber(new BigDecimal("1265633780")));
        assertEquals("1265633780", MiscController.csvNumber(new BigDecimal("1265633780.00")));
        assertEquals("2.5", MiscController.csvNumber(new BigDecimal("2.50")));
        assertEquals("0", MiscController.csvNumber(BigDecimal.ZERO));
        assertFalse(MiscController.csvNumber(new BigDecimal("1000000")).contains("đ"));
        assertFalse(MiscController.csvNumber(new BigDecimal("1000000")).contains(","));
    }

    @Test
    void utf8BomCsvPrependsExactBomBytesAndPreservesVietnamese() {
        byte[] bytes = MiscController.utf8BomCsv("Phòng ban,Số lượng\nNội dung,10\n");
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                new byte[] {bytes[0], bytes[1], bytes[2]});
        String decoded = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertEquals("Phòng ban,Số lượng\nNội dung,10\n", decoded);
    }
}