package com.foobnix.android.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TtsSentenceSplitterTest {
    private static String split(String text) {
        return TtsSentenceSplitter.insertPauses(text, ".;:!?", "[pause]");
    }

    @Test public void keepsDialogueClosersOnThePrecedingSentence() {
        assertEquals("was the reason they were pulled over?'[pause]  'The driver has all the information.[pause]  He'll tell you.'[pause] ",
                split("was the reason they were pulled over?' 'The driver has all the information. He'll tell you.'"));
    }

    @Test public void handlesCurlyQuotesWithoutConsumingTheNextOpeningQuote() {
        assertEquals("‘Why?’[pause]  ‘He’ll tell you.’[pause] ", split("‘Why?’ ‘He’ll tell you.’"));
    }

    @Test public void preservesContractionsPossessivesAndElisions() {
        String text = "He'll explain the drivers’ route in the ’90s and say ’tis fine";
        assertEquals(text, split(text));
    }

    @Test public void keepsNestedClosingQuotesTogether() {
        assertEquals("“She said ‘yes.’”[pause]  Next.[pause] ", split("“She said ‘yes.’” Next."));
    }

    @Test public void keepsConsecutivePunctuationTogether() {
        assertEquals("‘What?!’[pause]  Next.[pause] ", split("‘What?!’ Next."));
    }

    @Test public void respectsConfiguredSentenceDelimiters() {
        assertEquals("One; two.'[pause]  Three?",
                TtsSentenceSplitter.insertPauses("One; two.' Three?", ".", "[pause]"));
    }

    @Test public void preservesAllCharactersWhenPausesAreRemoved() {
        String text = "‘Why?’ ‘He’ll tell you.’";
        assertEquals(text, TtsSentenceSplitter.insertPauses(text, ".!?", "[pause]")
                .replace("[pause] ", ""));
    }
    @Test public void pausesAfterShortWordsInTheDisplayedDialogue() {
        String text = "You’re picking them up.’ ‘No-no-no, I’m involved. Trust me. I’m very involved.’";
        assertEquals("You’re picking them up.’[pause]  ‘No-no-no, I’m involved.[pause]  Trust me.[pause]  I’m very involved.’[pause] ",
                split(TtsSentenceSplitter.protectShortAbbreviations(text)));
    }

    @Test public void doesNotTreatConsecutiveShortSentencesAsInitials() {
        String text = "Go. No. It is on. Is it?";
        assertEquals(split(text), split(TtsSentenceSplitter.protectShortAbbreviations(text)));
    }

    @Test public void preservesTitlesAndNameInitials() {
        assertEquals("Dr{dot} Smith met Mr{dot} Jones and J{dot} R{dot} R{dot} Tolkien.",
                TtsSentenceSplitter.protectShortAbbreviations(
                        "Dr. Smith met Mr. Jones and J. R. R. Tolkien."));
    }

    @Test public void preservesSentenceEndingCapitalI() {
        assertEquals("So did I. Next came you.",
                TtsSentenceSplitter.protectShortAbbreviations("So did I. Next came you."));
    }
}
