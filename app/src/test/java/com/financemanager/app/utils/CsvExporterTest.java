package com.financemanager.app.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** CSV output must stay parseable when values contain separators or quotes. */
public class CsvExporterTest {

    @Test
    public void leavesPlainValuesAlone() {
        assertEquals("Coffee", CsvExporter.escape("Coffee"));
        assertEquals("", CsvExporter.escape(null));
    }

    @Test
    public void quotesSeparatorsAndNewlines() {
        assertEquals("\"Lunch, coffee\"", CsvExporter.escape("Lunch, coffee"));
        assertEquals("\"line 1\nline 2\"", CsvExporter.escape("line 1\nline 2"));
        assertEquals("\"line 1\r\nline 2\"", CsvExporter.escape("line 1\r\nline 2"));
    }

    @Test
    public void doublesEmbeddedQuotes() {
        assertEquals("\"He said \"\"hi\"\"\"", CsvExporter.escape("He said \"hi\""));
    }
}
