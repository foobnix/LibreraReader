package com.foobnix.tts;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpeechRequestTest {
    @Test public void rejectsCallbacksFromAnEarlierPageIncludingSamePageSeeks() {
        SpeechRequest old = new SpeechRequest(12);
        SpeechRequest current = new SpeechRequest(12);
        assertNull(current.signal(old.id("Finished0")));
        assertNull(current.signal(old.id("LirbiReader")));
        assertNull(current.signal(old.id("Stoped")));
        assertEquals("Finished0", current.signal(current.id("Finished0")));
    }

    @Test public void turnsWhenAWordCrossesTheBoundaryIncludingJoinedHyphenatedWords() {
        SpeechRequest request = new SpeechRequest(10);
        request.register("Finished0", 10, 30);
        assertFalse(request.reachesPage("Finished0", 10));
        assertTrue(request.reachesPage("Finished0", 14));
        assertFalse(request.reachesPage("Temp", Integer.MAX_VALUE));
    }

    @Test public void precedingParagraphCannotTriggerFallbackButFollowingParagraphCan() {
        SpeechRequest request = new SpeechRequest(20);
        request.register("Finished0", 20, 10);
        request.register("Finished1", -3, 10);
        assertFalse(request.reachesPage("Finished0", Integer.MAX_VALUE));
        assertTrue(request.reachesPage("Finished1", 1));
    }

    @Test public void fallbackStopsAfterFirstSentenceOnNewPageRatherThanWholePage() {
        String text = "Carried sentence ends. The rest of the page follows.";
        int boundary = text.indexOf("ends");
        int end = SpeechRequest.firstSentenceEnd(text, boundary);
        assertEquals("Carried sentence ends.", text.substring(0, end));
        assertEquals(" The rest of the page follows.", text.substring(end));
    }
    @Test public void fallbackIncludesClosingQuotesBeforeTheFollowingSentence() {
        String text = "‘Carried sentence ends.’ ‘More follows.’";
        int end = SpeechRequest.firstSentenceEnd(text, text.indexOf("ends"));
        assertEquals("‘Carried sentence ends.’", text.substring(0, end));
        assertEquals(" ‘More follows.’", text.substring(end));
    }

}
