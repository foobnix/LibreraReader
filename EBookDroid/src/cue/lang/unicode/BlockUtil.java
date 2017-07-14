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
package cue.lang.unicode;

import java.lang.Character.UnicodeBlock;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import cue.lang.Counter;
import cue.lang.WordIterator;

public class BlockUtil {
	static final Pattern EXTENDED_LATIN = Pattern
			.compile("\\p{InLatin-1Supplement}");
	static final Pattern HYPEREXTENDED_LATIN = Pattern.compile("[" //
			+ "\\p{InLatinExtended-A}\\p{InLatinExtended-B}" //
			+ "\\p{InSpacingModifierLetters}" //
			+ "\\p{InIPAExtensions}" //
			+ "\\p{InCombiningDiacriticalMarks}]");

	private static UnicodeBlock getBlock(final String word) {
		final int c = word.codePointAt(0);
		final UnicodeBlock block = UnicodeBlock.of(c);
		if (block == UnicodeBlock.BASIC_LATIN
				&& HYPEREXTENDED_LATIN.matcher(word).find()) {
			return UnicodeBlock.LATIN_EXTENDED_A;
		}
		if (block == UnicodeBlock.BASIC_LATIN
				&& EXTENDED_LATIN.matcher(word).find()) {
			return UnicodeBlock.LATIN_1_SUPPLEMENT;
		}
		return block;
	}

	public static UnicodeBlock guessUnicodeBlock(final String text) {
		return guessUnicodeBlock(new Counter<String>(new WordIterator(text)));
	}

	public static UnicodeBlock guessUnicodeBlock(
			final Counter<String> wordCounter) {
		return guessUnicodeBlock(wordCounter.getMostFrequent(50));
	}

	/**
	 * This method is for helping you guess, e.g., what kind of font you'll need
	 * in order to represent some text. In particular, if a lot of the
	 * characters in your text are Latin, but there are a few LATIN_EXTENDED_A
	 * characters lurking in there, then we say the result is LATIN_EXTENDED_A,
	 * because you'll need to be able to handle those characters.
	 * 
	 * @param words
	 * @return The most representative UnicodeBlock for the given words.
	 */
	public static UnicodeBlock guessUnicodeBlock(final Collection<String> words) {
		boolean hasExtendedLatin = false;
		boolean hasHyperextendedLatin = false;
		final Counter<UnicodeBlock> counter = new Counter<UnicodeBlock>();
		for (final String word : words) {
			final UnicodeBlock block = getBlock(word);
			if (block == UnicodeBlock.LATIN_1_SUPPLEMENT) {
				hasExtendedLatin = true;
			}
			if (block == UnicodeBlock.LATIN_EXTENDED_A) {
				hasHyperextendedLatin = true;
			}
			counter.note(block);
		}
		final List<UnicodeBlock> mostFrequent = counter.getMostFrequent(1);
		if (mostFrequent.size() == 0) {
			return null;
		}
		UnicodeBlock b = mostFrequent.get(0);
		/*
		 * If we've seen *any* extended latin, and we're mostly latin, then
		 * treat the whole thing as extended.
		 */
		if (b == UnicodeBlock.BASIC_LATIN
				|| b == UnicodeBlock.LATIN_1_SUPPLEMENT) {
			if (hasHyperextendedLatin) {
				return UnicodeBlock.LATIN_EXTENDED_A;
			}
			if (hasExtendedLatin) {
				return UnicodeBlock.LATIN_1_SUPPLEMENT;
			}
		}
		return b;
	}

}
