/*
   Copyright 2009 IBM Corp

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package cue.lang;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Construct with a {@link String}; retrieve a sequence of {@link String}s, each
 * of which is a "sentence" according to Java's built-in model for the given
 * {@link Locale}.
 * 
 * @author Jonathan Feinberg <jdf@us.ibm.com>
 * 
 */
public class SentenceIterator extends IterableText {
	private final String text;
	private final BreakIterator breakIterator;
	int start, end;

	/**
	 * Uses the default {@link Locale} for the running isnatnce of the JVM.
	 * 
	 * @param text
	 */
	public SentenceIterator(final String text) {
		this(text, Locale.getDefault());
	}

	public SentenceIterator(final String text, final Locale locale) {
		this.text = text;
		breakIterator = BreakIterator.getSentenceInstance(locale);
		breakIterator.setText(text);
		start = end = breakIterator.first();
		advance();
	}

	private static final Pattern ABBREVS = Pattern
			.compile("(?:Mrs?|Ms|Dr|Rev)\\.\\s*$");

	private void advance() {
		start = end;
		while (hasNext()
				&& ((end == start) || ABBREVS.matcher(
						text.substring(start, end)).find())) {
			end = breakIterator.next();
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public String next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		final String result = text.substring(start, end)
				.replaceAll("\\s+", " ");
		advance();
		return result;
	}

	public final boolean hasNext() {
		return end != BreakIterator.DONE;
	}

}
