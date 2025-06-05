package com.univocity.parsers.issues.github;

import com.univocity.parsers.csv.*;
import org.testng.annotations.*;

import java.io.*;
import java.util.*;

import static org.testng.Assert.*;


/**
 * From: <a href="https://github.com/univocity/univocity-parsers/issues/438">GitHub438</a>
 *
 * @author Univocity Software Pty Ltd - <a href="mailto:parsers@univocity.com">parsers@univocity.com</a>
 */
public class Github_438 {
	@Test
	public void testSuperLongHeader() {
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.getFormat().setLineSeparator("\n");
		StringBuilder sb = new StringBuilder();
        sb.append("a".repeat(1025));
		settings.setHeaders(sb.toString());
		settings.getFormat().setLineSeparator("\n");
		StringWriter out = new StringWriter();

		CsvWriter writer = new CsvWriter(out, settings);
		writer.writeHeaders();
		List<String> row = new ArrayList<>();
		row.add("value 1");
		row.add("value 2");
		writer.writeRow(row);
		writer.close();

		assertEquals(out.toString(), sb + "\nvalue 1,value 2\n");
	}
}