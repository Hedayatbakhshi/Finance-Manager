package com.financemanager.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.financemanager.app.models.TransactionType;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

public class CsvImportParserTest {

    @Test
    public void parsesQuotedFieldsCommasAndEmbeddedNewlines() {
        String csv = "Date,Title,Description\r\n"
                + "2026-01-02,\"Coffee, milk\",\"Line one\nLine two\"\r\n"
                + "2026-01-03,\"He said \"\"hi\"\"\",\r\n";
        List<List<String>> rows = CsvImportParser.parse(csv);

        assertEquals(3, rows.size());
        assertEquals("Coffee, milk", rows.get(1).get(1));
        assertEquals("Line one\nLine two", rows.get(1).get(2));
        assertEquals("He said \"hi\"", rows.get(2).get(1));
        assertEquals(3, rows.get(2).size());
    }

    @Test
    public void stripsByteOrderMarkAndIgnoresTrailingNewline() {
        List<List<String>> rows = CsvImportParser.parse(
                "\uFEFFDate,Title\n2026-01-02,Rent\n");
        assertEquals(2, rows.size());
        assertEquals("Date", rows.get(0).get(0));
    }

    @Test
    public void handlesEmptyInput() {
        assertTrue(CsvImportParser.parse("").isEmpty());
        assertTrue(CsvImportParser.parse(null).isEmpty());
    }

    @Test
    public void findsHeadersRegardlessOfCaseAndSpacing() {
        List<String> header = List.of(" date ", "Title", "To Account", "AMOUNT");
        assertEquals(0, CsvImportParser.indexOf(header, "date"));
        assertEquals(1, CsvImportParser.indexOf(header, "title"));
        assertEquals(2, CsvImportParser.indexOf(header, "to account"));
        assertEquals(-1, CsvImportParser.indexOf(header, "notes"));
    }

    @Test
    public void parsesExportedTypeLabels() {
        assertEquals(TransactionType.INCOME, CsvImportParser.parseType("Income"));
        assertEquals(TransactionType.EXPENSE, CsvImportParser.parseType(" EXPENSE "));
        assertEquals(TransactionType.TRANSFER, CsvImportParser.parseType("Transfer"));
        assertNull(CsvImportParser.parseType("Bonus"));
        assertNull(CsvImportParser.parseType(""));
    }

    @Test
    public void parsesPositiveAmountsOnly() {
        assertEquals(Long.valueOf(123450), CsvImportParser.parseAmount("1,234.50"));
        assertEquals(Long.valueOf(500), CsvImportParser.parseAmount("5"));
        assertNull(CsvImportParser.parseAmount("0"));
        assertNull(CsvImportParser.parseAmount("-12.00"));
        assertNull(CsvImportParser.parseAmount("not money"));
    }

    @Test
    public void parsesSupportedDateFormats() {
        long expected = DateUtils.startOfDay(LocalDate.of(2026, 3, 14));
        assertEquals(expected, CsvImportParser.parseDate("2026-03-14"));
        assertEquals(expected, CsvImportParser.parseDate("2026/03/14"));
        assertEquals(expected, CsvImportParser.parseDate("14/03/2026"));
        assertEquals(0, CsvImportParser.parseDate("14 March 2026"));
        assertEquals(0, CsvImportParser.parseDate(""));
    }
}
