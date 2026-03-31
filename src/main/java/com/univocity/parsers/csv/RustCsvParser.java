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

import java.nio.charset.StandardCharsets;

/**
 * A thin Java wrapper around the native Rust CSV parser built from the
 * {@code rust-csv/} module.  The native library
 * ({@code libunivocity_parsers_rust.so} on Linux,
 * {@code univocity_parsers_rust.dll} on Windows) must be available on
 * {@code java.library.path} before any method is called.
 *
 * <p>This class exists solely for the performance comparison described in
 * {@code src/test/java/com/univocity/parsers/csv/RustVsJavaBenchmarkTest.java}.
 * It is <em>not</em> a production-ready replacement for {@link CsvParser}.</p>
 *
 * <h3>Building the native library</h3>
 * <pre>
 *   cd rust-csv
 *   cargo build --release
 *   # The library is produced at:
 *   #   target/release/libunivocity_parsers_rust.so  (Linux)
 *   #   target/release/univocity_parsers_rust.dll    (Windows)
 * </pre>
 */
public final class RustCsvParser {

	private static boolean nativeLoaded = false;
	private static Throwable nativeLoadError = null;

	static {
		try {
			System.loadLibrary("univocity_parsers_rust");
			nativeLoaded = true;
		} catch (UnsatisfiedLinkError e) {
			nativeLoadError = e;
		}
	}

	/**
	 * Returns {@code true} if the native Rust library was loaded successfully.
	 */
	public static boolean isNativeLoaded() {
		return nativeLoaded;
	}

	/**
	 * Returns the error that occurred when attempting to load the native
	 * library, or {@code null} if the library loaded successfully.
	 */
	public static Throwable getNativeLoadError() {
		return nativeLoadError;
	}

	// -----------------------------------------------------------------------
	// Native methods
	// -----------------------------------------------------------------------

	/**
	 * Parse {@code csvData} (UTF-8 encoded CSV bytes) and return the total
	 * number of records.  No Java objects are created for individual field
	 * values, so this measures <em>raw parsing throughput</em>.
	 *
	 * @param csvData UTF-8 encoded CSV bytes
	 * @return number of rows parsed, or -1 on error
	 * @throws UnsatisfiedLinkError if the native library is not loaded
	 */
	public static native long countRowsNative(byte[] csvData);

	/**
	 * Parse {@code csvData} (UTF-8 encoded CSV bytes) and return every field
	 * value as a Java {@code String[][]}.  The first dimension is the row
	 * index; the second is the column index.
	 *
	 * @param csvData      UTF-8 encoded CSV bytes
	 * @param expectedRows hint for pre-allocation (use 0 if unknown)
	 * @return parsed rows, or {@code null} on error
	 * @throws UnsatisfiedLinkError if the native library is not loaded
	 */
	public static native String[][] parseAllNative(byte[] csvData, int expectedRows);

	// -----------------------------------------------------------------------
	// Convenience helpers
	// -----------------------------------------------------------------------

	/**
	 * Count CSV rows in a {@link String}.
	 *
	 * @param csv raw CSV text
	 * @return number of rows
	 */
	public static long countRows(String csv) {
		return countRowsNative(csv.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Parse all CSV rows from a {@link String}.
	 *
	 * @param csv raw CSV text
	 * @return 2-D array of field values
	 */
	public static String[][] parseAll(String csv) {
		byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
		return parseAllNative(bytes, 0);
	}
}
