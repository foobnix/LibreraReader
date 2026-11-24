package com.foobnix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.foobnix.pdf.info.wrapper.ReadingPlace;

import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * The place in words: taken from the top of the screen part way down a page, and found again on
 * a page laid out another way - cut at other line ends, by another engine.
 */
public class TestReadingPlace {

    /** A page of lines 0.1 high, one line per string, words split at spaces. */
    private static TextWord[][] page(String... lines) {
        final List<TextWord> words = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            float left = 0;
            for (String w : lines[i].split(" ")) {
                final TextWord word = new TextWord();
                word.w = w;
                word.left = left;
                word.right = left + 0.05f;
                word.top = 0.1f * i;
                word.bottom = 0.1f * i + 0.08f;
                left += 0.06f;
                words.add(word);
            }
        }
        return new TextWord[][]{words.toArray(new TextWord[0])};
    }

    @Test
    public void theTextStartsAtTheFirstLineOnScreen() {
        final TextWord[][] words = page("– Нет, – покачал головой Дима.", "– Они были вырезаны на могильном кре-",
                                        "сте в Антарктиде, поставленном на холме", "Глава 2", "Прошло около двух часов.");
        // The top of the screen is just above the third line
        assertEquals("сте в Антарктиде, поставленном на холме Глава 2", ReadingPlace.textFrom(words, 0.19f));
        // At the top of the page, the page start
        assertTrue(ReadingPlace.textFrom(words, 0f).startsWith("– Нет, – покачал"));
    }

    @Test
    public void aWordCutAtTheLineEndIsPutBackTogether() {
        final TextWord[][] words = page("Никому не хо-", "телось разговаривать");
        assertEquals("Никому не хотелось разговаривать", ReadingPlace.textFrom(words, 0f));
    }

    @Test
    public void theTextIsFoundOnAnotherLayout() {
        final String saved = "сте в Антарктиде, поставленном на холме Глава 2";
        // Another device: wider lines, the cut word whole, the text on the fifth line
        final TextWord[][] other = page("одна", "две", "три", "четыре слова", "кресте в Антарк-",
                                        "тиде, поставленном на холме Обзервейшн", "Глава 2");
        final float top = ReadingPlace.topOf(other, saved);
        assertTrue("found on the fifth line, got " + top, top > 0.37f && top <= 0.4f);
    }

    @Test
    public void textNotOnThePageIsNotFound() {
        assertEquals(-1f, ReadingPlace.topOf(page("Совсем другая страница книги"), "сте в Антарктиде, поставленном"), 0f);
    }

    @Test
    public void tooLittleTextIsNoPlace() {
        assertEquals(-1f, ReadingPlace.topOf(page("Глава 2 текст"), "Глава 2"), 0f);
        assertNull(ReadingPlace.textFrom(page("Глава"), 0.5f));
    }

    @Test
    public void theSavedTextIsShortAndCutAtAWord() {
        final String text = ReadingPlace.shorter("Прошло около двух часов. Никому не хотелось разговаривать, каждый");
        assertTrue(text.length() <= 48);
        assertEquals("Прошло около двух часов. Никому не хотелось", text);
    }
}
