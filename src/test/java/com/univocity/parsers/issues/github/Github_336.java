/*******************************************************************************
 * Copyright 2019 Univocity Software Pty Ltd
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
package com.univocity.parsers.issues.github;


import com.univocity.parsers.csv.*;
import org.testng.annotations.*;

import java.io.*;
import java.nio.charset.*;

import static org.testng.Assert.*;

/**
 * From: https://github.com/univocity/univocity-parsers/issues/336
 *
 * @author Univocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class Github_336 {

	@Test
	public void testWrongColumnCountOnEmptyLines() {

		final CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		settings.setSkipEmptyLines(false);

		final CsvParser inputParser = new CsvParser(settings);
		inputParser.beginParsing(new StringReader("H\n\n\n"));

		String[] row = inputParser.parseNext();
		assertEquals(row.length, 1);
		assertNull(row[0]);

		row = inputParser.parseNext();
		assertEquals(row.length, 1);
		assertNull(row[0]);

		row = inputParser.parseNext();
		assertNull(row);
	}
}
