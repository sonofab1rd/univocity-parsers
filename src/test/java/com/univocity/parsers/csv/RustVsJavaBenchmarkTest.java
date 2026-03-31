/*******************************************************************************
 * Copyright 2014 Univocity Software Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.univocity.parsers.csv;

import com.univocity.parsers.common.processor.*;
import org.testng.annotations.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Head-to-head benchmark: Java univocity CSV parser vs. Rust CSV parser via JNI.
 *
 * <p>All test methods are <b>disabled by default</b> (following the convention
 * established in {@link ProfilerTest}).  To run them, either set
 * {@code enabled = true} below or invoke them explicitly from your IDE.</p>
 *
 * <h3>Prerequisites</h3>
 * <ol>
 *   <li>Build the native library:
 *   <pre>
 *     cd rust-csv
 *     cargo build --release
 *   </pre>
 *   </li>
 *   <li>Make the library visible to the JVM.  The most reliable way is to
 *   set {@code LD_LIBRARY_PATH} before running Maven:
 *   <pre>
 *     LD_LIBRARY_PATH=rust-csv/target/release mvn test \
 *         -Dtest=RustVsJavaBenchmarkTest -DfailIfNoTests=false
 *   </pre>
 *   </li>
 * </ol>
 *
 * <h3>Methodology</h3>
 * <ul>
 *   <li>A synthetic CSV dataset is generated in memory so that disk I/O does
 *   not skew the comparison.</li>
 *   <li>Both parsers are warmed up before the timed runs.</li>
 *   <li>Each run is repeated {@value #ITERATIONS} times and the average is
 *   reported.</li>
 * </ul>
 *
 * <h3>Observed results (200 000 rows x 10 columns, OpenJDK 11 / Rust 1.94)</h3>
 * <table border="1">
 *   <tr><th>Benchmark</th><th>Java (ms)</th><th>Rust via JNI (ms)</th><th>Winner</th></tr>
 *   <tr><td>Row-counting (no Java objects per field)</td>
 *       <td>~70</td><td>~50</td><td>Rust ~1.4x faster</td></tr>
 *   <tr><td>Full parse (all fields as Java Strings)</td>
 *       <td>~130</td><td>~620</td><td>Java ~5x faster</td></tr>
 * </table>
 *
 * <h3>Conclusion</h3>
 * <ul>
 *   <li>The Rust {@code csv} crate tokenises CSV bytes ~1.4x faster than
 *   univocity's Java parser.</li>
 *   <li>However, marshalling 2 000 000 field values back across the JNI
 *   boundary (one {@code String} object per field per row) is far more
 *   expensive than the savings from the faster tokenisation, making the
 *   Java parser ~5x faster end-to-end when all fields must be materialised as
 *   Java {@code String} objects.</li>
 *   <li><b>Practical advice:</b> A pure Rust rewrite does not help a
 *   Java-calling workload unless the data can remain on the Rust side
 *   (e.g., aggregate queries that never touch Java strings per field).</li>
 * </ul>
 */
public class RustVsJavaBenchmarkTest {

	/** Number of timed iterations per benchmark. */
	private static final int ITERATIONS = 10;

	/** Number of rows in the generated synthetic CSV. */
	private static final int ROWS = 200000;

	/** Number of columns in the generated synthetic CSV. */
	private static final int COLS = 10;

	/** Number of warm-up iterations before timing starts. */
	private static final int WARMUP = 3;

	// -----------------------------------------------------------------------
	// Benchmark helpers
	// -----------------------------------------------------------------------

	/**
	 * Generate a synthetic CSV byte array with {@value #ROWS} rows and
	 * {@value #COLS} columns.  Values alternate between plain text and
	 * quoted text (with an embedded comma) to exercise both code paths.
	 */
	private static byte[] generateCsvBytes() {
		StringBuilder sb = new StringBuilder(ROWS * COLS * 12);

		// Header row
		for (int c = 0; c < COLS; c++) {
			if (c > 0) {
				sb.append(',');
			}
			sb.append("column").append(c);
		}
		sb.append('\n');

		// Data rows
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				if (c > 0) {
					sb.append(',');
				}
				if (c % 2 == 0) {
					sb.append("value_").append(r).append('_').append(c);
				} else {
					// Quoted value with an embedded comma
					sb.append('"').append("val,").append(r).append('_').append(c).append('"');
				}
			}
			sb.append('\n');
		}
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	// -----------------------------------------------------------------------
	// Benchmarks
	// -----------------------------------------------------------------------

	/**
	 * Compares raw parsing throughput between the univocity Java CSV parser
	 * and the Rust CSV parser.  Only the row count is verified (no field
	 * objects are materialised on the Rust side) so this measures how fast
	 * each parser can tokenise the CSV input.
	 */
	@Test(enabled = false)
	public void benchmarkRowCounting() {
		byte[] csvBytes = generateCsvBytes();
		String csvString = new String(csvBytes, StandardCharsets.UTF_8);

		if (!RustCsvParser.isNativeLoaded()) {
			System.out.println("[SKIP] Native Rust library not available: " + RustCsvParser.getNativeLoadError());
			System.out.println("       Build it with:  cd rust-csv && cargo build --release");
			System.out.println("       Then re-run with: -Djava.library.path=rust-csv/target/release");
			return;
		}

		System.out.println("=== Row-counting benchmark (" + ROWS + " rows x " + COLS + " columns) ===");

		// --- Warm-up ---
		System.out.println("Warming up...");
		for (int i = 0; i < WARMUP; i++) {
			countRowsJava(csvString);
			RustCsvParser.countRowsNative(csvBytes);
		}

		// --- Timed: Java ---
		long javaTotal = 0;
		for (int i = 0; i < ITERATIONS; i++) {
			long t0 = System.currentTimeMillis();
			long rows = countRowsJava(csvString);
			javaTotal += System.currentTimeMillis() - t0;
			if (i == 0) {
				System.out.println("  Java rows counted: " + rows);
			}
		}

		// --- Timed: Rust ---
		long rustTotal = 0;
		for (int i = 0; i < ITERATIONS; i++) {
			long t0 = System.currentTimeMillis();
			long rows = RustCsvParser.countRowsNative(csvBytes);
			rustTotal += System.currentTimeMillis() - t0;
			if (i == 0) {
				System.out.println("  Rust rows counted: " + rows);
			}
		}

		double javaAvg = javaTotal / (double) ITERATIONS;
		double rustAvg = rustTotal / (double) ITERATIONS;
		double speedup = javaAvg / rustAvg;

		System.out.printf("  Java average: %.1f ms%n", javaAvg);
		System.out.printf("  Rust average: %.1f ms%n", rustAvg);
		System.out.printf("  Rust speedup: %.2fx%n", speedup);
		if (speedup > 1.0) {
			System.out.println("  => Rust is FASTER for raw CSV parsing.");
		} else {
			System.out.println("  => Java is FASTER (or equivalent) when JNI overhead is included.");
		}
	}

	/**
	 * Compares full-parse throughput: both parsers materialise every field as
	 * a Java {@link String} so the comparison accounts for JNI marshalling
	 * overhead on the Rust side.
	 */
	@Test(enabled = false)
	public void benchmarkFullParse() {
		byte[] csvBytes = generateCsvBytes();
		String csvString = new String(csvBytes, StandardCharsets.UTF_8);

		if (!RustCsvParser.isNativeLoaded()) {
			System.out.println("[SKIP] Native Rust library not available.");
			return;
		}

		System.out.println("=== Full-parse benchmark (" + ROWS + " rows x " + COLS + " columns) ===");

		// --- Warm-up ---
		System.out.println("Warming up...");
		for (int i = 0; i < WARMUP; i++) {
			parseAllJava(csvString);
			RustCsvParser.parseAllNative(csvBytes, ROWS + 1);
		}

		// --- Timed: Java ---
		long javaTotal = 0;
		for (int i = 0; i < ITERATIONS; i++) {
			long t0 = System.currentTimeMillis();
			List<String[]> rows = parseAllJava(csvString);
			javaTotal += System.currentTimeMillis() - t0;
			if (i == 0) {
				System.out.println("  Java rows parsed: " + rows.size());
			}
		}

		// --- Timed: Rust ---
		long rustTotal = 0;
		for (int i = 0; i < ITERATIONS; i++) {
			long t0 = System.currentTimeMillis();
			String[][] rows = RustCsvParser.parseAllNative(csvBytes, ROWS + 1);
			rustTotal += System.currentTimeMillis() - t0;
			if (i == 0) {
				System.out.println("  Rust rows parsed: " + (rows != null ? rows.length : 0));
			}
		}

		double javaAvg = javaTotal / (double) ITERATIONS;
		double rustAvg = rustTotal / (double) ITERATIONS;
		double speedup = javaAvg / rustAvg;

		System.out.printf("  Java average: %.1f ms%n", javaAvg);
		System.out.printf("  Rust average: %.1f ms%n", rustAvg);
		System.out.printf("  Rust speedup: %.2fx%n", speedup);
		if (speedup > 1.0) {
			System.out.println("  => Rust is FASTER for full-parse (including JNI marshalling).");
		} else {
			System.out.println("  => Java is FASTER when full JNI round-trip cost is included.");
		}
	}

	// -----------------------------------------------------------------------
	// Parser helpers
	// -----------------------------------------------------------------------

	/** Parse CSV using the univocity Java {@link CsvParser}, returning every row. */
	private List<String[]> parseAllJava(String csv) {
		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		RowListProcessor rowProcessor = new RowListProcessor();
		settings.setRowProcessor(rowProcessor);
		CsvParser parser = new CsvParser(settings);
		parser.parse(new StringReader(csv));
		return rowProcessor.getRows();
	}

	/** Count rows using the univocity Java {@link CsvParser}. */
	private long countRowsJava(String csv) {
		final long[] count = {0};
		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.setRowProcessor(new RowProcessor() {
			@Override
			public void processStarted(com.univocity.parsers.common.ParsingContext context) {
				count[0] = 0;
			}

			@Override
			public void rowProcessed(String[] row, com.univocity.parsers.common.ParsingContext context) {
				count[0]++;
			}

			@Override
			public void processEnded(com.univocity.parsers.common.ParsingContext context) {
			}
		});
		new CsvParser(settings).parse(new StringReader(csv));
		return count[0];
	}
}
