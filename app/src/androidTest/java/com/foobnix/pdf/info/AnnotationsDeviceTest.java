package com.foobnix.pdf.info;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppAnnotation;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The marks Librera keeps itself: the store they live in, and finding them again on a page.
 *
 * The engine-specific half of this - a CFI anchor surviving a re-layout - is in
 * {@code FoliateEngineDeviceTest}; what is here is everything that holds whatever engine made
 * the mark.
 */
@RunWith(AndroidJUnit4.class)
public class AnnotationsDeviceTest {

    private static final String BOOK = "/sdcard/Books/annotations-device-test.epub";

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        AnnotationsData.get().removeAll(BOOK);
    }

    @After
    public void tearDown() {
        AnnotationsData.get().removeAll(BOOK);
    }

    // ---------------------------------------------------------------- the store

    @Test
    public void aMarkRoundTripsThroughTheStore() {
        final AppAnnotation mark = new AppAnnotation(BOOK);
        mark.setType(AnnotationType.UNDERLINE);
        mark.color = "#FF00FF";
        mark.text = "carefully numbered pages";
        mark.page = 7;
        mark.p = 0.42f;
        mark.cfi = "epubcfi(/6/4!/4/2/2[p1]/1:10)";
        AnnotationsData.get().add(mark);

        final List<AppAnnotation> stored = AnnotationsData.get().byBook(BOOK);
        assertEquals("one mark on the book", 1, stored.size());
        final AppAnnotation back = stored.get(0);
        assertEquals(mark.t, back.t);
        assertEquals(AnnotationType.UNDERLINE, back.getType());
        assertEquals("#FF00FF", back.color);
        assertEquals("carefully numbered pages", back.text);
        assertEquals(7, back.page);
        assertEquals(0.42f, back.p, 0.001f);
        assertEquals(mark.cfi, back.cfi);
        assertNotNull("it knows which device's file it came from", back.file);
        assertEquals("and the book, as an absolute path again",
                new File(BOOK).getName(), new File(back.getPath()).getName());
    }

    @Test
    public void removingTakesItOutOfTheStore() {
        final AppAnnotation mark = new AppAnnotation(BOOK);
        mark.text = "to be removed";
        AnnotationsData.get().add(mark);
        assertTrue(AnnotationsData.get().hasAny(BOOK));

        AnnotationsData.get().remove(AnnotationsData.get().byBook(BOOK).get(0));
        assertFalse("it is gone", AnnotationsData.get().hasAny(BOOK));
    }

    @Test
    public void marksOfOtherBooksAreNotMixedIn() {
        final AppAnnotation mine = new AppAnnotation(BOOK);
        mine.text = "mine";
        AnnotationsData.get().add(mine);

        final String other = "/sdcard/Books/annotations-device-test-other.epub";
        final AppAnnotation theirs = new AppAnnotation(other);
        theirs.text = "theirs";
        AnnotationsData.get().add(theirs);
        try {
            final List<AppAnnotation> stored = AnnotationsData.get().byBook(BOOK);
            assertEquals("only this book's", 1, stored.size());
            assertEquals("mine", stored.get(0).text);
        } finally {
            AnnotationsData.get().removeAll(other);
        }
    }

    /**
     * The page a book with no marks holds a list of its own.
     *
     * The first mark made on a book is added to whatever list its page was given when it was
     * rendered. Handed a shared immutable empty list, that add throws, the mark is never shown
     * and the page is never redrawn - which looks exactly like the mark not having been saved.
     */
    @Test
    public void aPageWithNoMarksStillTakesTheFirstOne() {
        final AnnotationsFinder finder = new AnnotationsFinder(BOOK);
        assertTrue("nothing on this book yet", finder.isEmpty());

        final List<AnnotationsFinder.Drawn> marks = finder.forPage(null, null, 0);
        assertTrue("and so nothing on the page", marks.isEmpty());

        final AppAnnotation mark = new AppAnnotation(BOOK);
        mark.text = "the first one";
        // The add a reader's first mark does - it must not throw
        marks.add(new AnnotationsFinder.Drawn(mark, AnnotationsFinder.parse("0.1,0.1,0.2,0.2")));
        assertEquals("the mark is on the page", 1, marks.size());
    }

    // ---------------------------------------------------------------- rectangles

    @Test
    public void rectanglesSurviveBeingWrittenDown() {
        final List<RectF> rects = new ArrayList<RectF>();
        rects.add(new RectF(0.1f, 0.2f, 0.3f, 0.25f));
        rects.add(new RectF(0.1f, 0.26f, 0.9f, 0.31f));

        final List<RectF> back = AnnotationsFinder.parse(AnnotationsFinder.write(rects));
        assertEquals(2, back.size());
        for (int i = 0; i < rects.size(); i++) {
            assertEquals(rects.get(i).left, back.get(i).left, 0.0002f);
            assertEquals(rects.get(i).top, back.get(i).top, 0.0002f);
            assertEquals(rects.get(i).right, back.get(i).right, 0.0002f);
            assertEquals(rects.get(i).bottom, back.get(i).bottom, 0.0002f);
        }
        assertTrue("nothing is read out of nothing", AnnotationsFinder.parse("").isEmpty());
        assertTrue("and rubbish is not guessed at", AnnotationsFinder.parse("1,2;x").isEmpty());
    }

    /** A selection of three lines is three marks, not one box around all of it. */
    @Test
    public void aSelectionBecomesOneRectanglePerLine() {
        final List<TextWord> words = new ArrayList<TextWord>();
        words.add(word("The", 0.10f, 0.10f, 0.20f, 0.12f));
        words.add(word("first", 0.21f, 0.10f, 0.35f, 0.12f));
        words.add(word("line", 0.36f, 0.10f, 0.48f, 0.12f));
        words.add(word("second", 0.10f, 0.14f, 0.30f, 0.16f));

        final List<RectF> lines = AnnotationsFinder.mergeLines(words);
        assertEquals("one rectangle per line", 2, lines.size());
        assertEquals("the first spans its words", 0.10f, lines.get(0).left, 0.0001f);
        assertEquals(0.48f, lines.get(0).right, 0.0001f);
        assertEquals("and does not reach down to the next", 0.12f, lines.get(0).bottom, 0.0001f);
        assertEquals("The first line second", AnnotationsFinder.textOf(words));
    }

    // ---------------------------------------------------------------- finding the words again

    @Test
    public void aMarkWithoutAnExactAnchorIsFoundByItsWords() {
        final TextWord[][] page = {
                {word("A", 0.1f, 0.1f, 0.15f, 0.12f), word("page", 0.16f, 0.1f, 0.3f, 0.12f),
                        word("of", 0.31f, 0.1f, 0.36f, 0.12f), word("words", 0.37f, 0.1f, 0.5f, 0.12f)},
                {word("and", 0.1f, 0.14f, 0.2f, 0.16f), word("another", 0.21f, 0.14f, 0.4f, 0.16f),
                        word("line", 0.41f, 0.14f, 0.5f, 0.16f)},
        };

        List<RectF> found = AnnotationsFinder.findText(page, "of words");
        assertEquals("the run is one rectangle, both words being on one line", 1, found.size());
        assertEquals(0.31f, found.get(0).left, 0.0001f);
        assertEquals(0.5f, found.get(0).right, 0.0001f);

        found = AnnotationsFinder.findText(page, "WORDS AND");
        assertEquals("a run across a line break is one rectangle per line", 2, found.size());

        assertTrue("words that are not there are not found",
                AnnotationsFinder.findText(page, "not on this page").isEmpty());
        assertTrue("and neither is nothing", AnnotationsFinder.findText(page, "").isEmpty());
    }

    private static TextWord word(final String text, final float l, final float t,
                                 final float r, final float b) {
        return new TextWord(text, new RectF(l, t, r, b));
    }
}
