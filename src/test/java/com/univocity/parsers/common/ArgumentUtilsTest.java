/*******************************************************************************
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
package com.univocity.parsers.common;

import org.testng.annotations.*;

import static org.testng.Assert.*;

public class ArgumentUtilsTest {

	@DataProvider
	public Object[][] trimCases() {
		return new Object[][]{
				{"", false, true, ""},
				{"\t \r\n", false, false, "\t \r\n"},
				{"\t \r\n", true, false, ""},
				{"\t \r\n", false, true, ""},
				{"\t \r\n", true, true, ""},
				{"a", false, false, "a"},
				{"a", true, false, "a"},
				{"a", false, true, "a"},
				{"a", true, true, "a"},
				{" a ", false, false, " a "},
				{" a ", true, false, "a "},
				{" a ", false, true, " a"},
				{" a ", true, true, "a"},
				{" ab ", true, true, "ab"},
				{"\0\t a \r\037", true, true, "a"},
				{"\u00a0a\u2003", true, true, "\u00a0a\u2003"}
		};
	}

	@Test(dataProvider = "trimCases")
	public void trimKeepsTheRemainingCharacters(String input, boolean left, boolean right, String expected) {
		assertEquals(ArgumentUtils.trim(input, left, right), expected);
	}
}
