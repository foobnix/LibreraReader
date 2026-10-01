package com.foobnix.android.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TtsQuoteCleanerTest {
    @Test public void removesTheReportedOpeningQuoteWithoutChangingContractions() {
        assertEquals("No I'm not. I'm here as a concerned citizen.'",
                TtsQuoteCleaner.clean("'No I'm not. I'm here as a concerned citizen.'"));
    }

    @Test public void handlesCurlyDialogueAndClosingQuotesAfterSentencePunctuation() {
        assertEquals("No I’m not. I’m here as a concerned citizen.’",
                TtsQuoteCleaner.clean("‘No I’m not. I’m here as a concerned citizen.’"));
    }

    @Test public void preservesPossessivesIncludingPluralPossessives() {
        String text = "James' book, James’s book, and the drivers’ route";
        assertEquals(text, TtsQuoteCleaner.clean(text));
    }

    @Test public void preservesLeadingApostrophesInDecadesAndElisions() {
        String text = "In the '90s, ’tis said ’twas fine to call ’em friends";
        assertEquals(text, TtsQuoteCleaner.clean(text));
    }

    @Test public void dialogueQuotesImmediatelyAfterExistingPausesAreDetached() {
        assertEquals("Hello.ttsPAUSENo I'm not.'",
                TtsQuoteCleaner.clean("Hello.ttsPAUSE'No I'm not.'"));
    }

    @Test public void doesNotStripApostrophesWhenAWordContinuesOntoTheNextPage() {
        assertEquals("I’m", TtsQuoteCleaner.clean("I’m"));
        assertEquals("don’", TtsQuoteCleaner.clean("don’"));
        assertEquals("’t", TtsQuoteCleaner.clean("’t"));
        assertEquals("'m not.", TtsQuoteCleaner.clean("'m not."));
    }
    @Test public void preservesTheReportedDialogueBoundary() {
        String cleaned = TtsQuoteCleaner.clean("You're picking them up.' 'No-no-no, I'm very involved.'");
        assertEquals("You're picking them up.' No-no-no, I'm very involved.'", cleaned);
        assertEquals("You're picking them up.'[pause]  No-no-no, I'm very involved.'[pause] ",
                TtsSentenceSplitter.insertPauses(cleaned, ".!?", "[pause]"));
    }

    @Test public void preservesCurlyClosingQuotesAtChangesOfSpeaker() {
        assertEquals("You’re picking them up.’ No-no-no, I’m very involved.’",
                TtsQuoteCleaner.clean("‘You’re picking them up.’ ‘No-no-no, I’m very involved.’"));
    }

    @Test public void removesAnOpeningQuoteAfterPunctuationWithoutWhitespace() {
        assertEquals("Hello.No I'm not.'", TtsQuoteCleaner.clean("Hello.'No I'm not.'"));
    }

    @Test public void attachesClosingQuotesAfterPunctuationReplacementsInsertASpace() {
        String cleaned = TtsQuoteCleaner.clean("You're picking them up. ' 'No-no-no, I'm very involved. '");
        assertEquals("You're picking them up.' No-no-no, I'm very involved.'", cleaned);
        assertEquals("You're picking them up.'[pause]  No-no-no, I'm very involved.'[pause] ",
                TtsSentenceSplitter.insertPauses(cleaned, ".!?", "[pause]"));
    }

}
