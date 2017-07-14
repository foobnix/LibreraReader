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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.NoSuchElementException;

import cue.lang.stop.StopWords;

/**
 * Construct with a {@link String}, some integer n, and a {@link Locale};
 * retrieve a sequence of {@link String}s, each of which has n words that appear
 * contiguously within a sentence. "Words" are as defined by the
 * {@link WordIterator}.
 * 
 * <p>
 * If you don't provide a {@link Locale}, you get the default {@link Locale} for
 * your system, which may or may not be what you want. The {@link Locale} is
 * used by a {@link SentenceIterator} to find sentence breaks.
 * 
 * <p>
 * Example:
 * 
 * <pre>
 * final String lyric = "This happened once before. I came to your door. No reply.";
 * for (final String s : new NGramIterator(3, lyric)) {
 *     System.out.println(s);
 * }
 * for (final String s : new NGramIterator(2, lyric)) {
 *     System.out.println(s);
 * }
 *  
 * This happened once
 * happened once before
 * I came to
 * came to your
 * to your door
 * 
 * This happened
 * happened once
 * once before
 * I came
 * came to
 * to your
 * your door
 * No reply
 * </pre>
 * 
 * @author Jonathan Feinberg <jdf@us.ibm.com>
 * 
 */
public class NGramIterator extends IterableText {
	private final SentenceIterator sentenceIterator;
	private final LinkedList<String> grams = new LinkedList<String>();
	private final int n;
	private final StopWords stopWords;

	private String next;
	private Iterator<String> currentWordIterator;

	public NGramIterator(final int n, final String text) {
		this(n, text, Locale.getDefault());
	}

	public NGramIterator(final int n, final String text, final Locale locale) {
		this(n, text, locale, null);
	}

	public NGramIterator(final int n, final String text, final Locale locale,
			final StopWords stopWords) {
		this.n = n;
		this.sentenceIterator = new SentenceIterator(text, locale);
		this.stopWords = stopWords;
		loadNext();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public String next() {
		if (next == null) {
			throw new NoSuchElementException();
		}
		final String result = next;
		loadNext();
		return result;
	}

	public boolean hasNext() {
		return next != null;
	}

	private void loadNext() {
		next = null;
		if (!grams.isEmpty()) {
			grams.pop();
		}
		while (grams.size() < n) {
			while (currentWordIterator == null
					|| !currentWordIterator.hasNext()) {
				if (!sentenceIterator.hasNext()) {
					return;
				}
				grams.clear();
				currentWordIterator = new WordIterator(sentenceIterator.next())
						.iterator();
				for (int i = 0; currentWordIterator.hasNext() && i < n - 1; i++) {
					maybeAddWord();
				}
			}
			// now grams has n-1 words in it and currentWordIterator hasNext
			maybeAddWord();
		}
		final StringBuilder sb = new StringBuilder();
		for (final String gram : grams) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(gram);
		}
		next = sb.toString();
	}

	private void maybeAddWord() {
		final String nextWord = currentWordIterator.next();
		if (stopWords != null && stopWords.isStopWord(nextWord)) {
			grams.clear();
		} else {
			grams.add(nextWord);
		}
	}
}
