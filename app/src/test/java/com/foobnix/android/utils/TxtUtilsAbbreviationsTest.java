package com.foobnix.android.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The fast abbreviation pass has to come out exactly like the one it replaces, which searched the
 * whole page for every rule of the dictionary. That old pass is the oracle here.
 */
public class TxtUtilsAbbreviationsTest {

    /** The pass as it was before: every rule, over the whole text, in the order of the list. */
    private static String everyRuleOverTheWholeText(String text, List<String> rules) {
        for (String r : rules) {
            if (!r.startsWith("!")) {
                text = text.replace(" " + r, " " + r.replace(".", "{dot}"));
            }
        }
        return text;
    }

    private static void same(String text, List<String> rules) {
        assertEquals("text [" + text + "]",
                everyRuleOverTheWholeText(text, rules), TxtUtils.protectAbbreviations(text, rules));
    }

    private static List<String> rules(String... r) {
        return new ArrayList<>(Arrays.asList(r));
    }

    @Test
    public void appliesTheRulesThatAreInTheText() {
        assertEquals(" Prof{dot} Smith und т{dot}д{dot}",
                TxtUtils.protectAbbreviations(" Prof. Smith und т.д.", rules("Prof.", "т.д.", "abc.")));
    }

    @Test
    public void leavesTextWithoutAbbreviationsAlone() {
        String text = " A sentence. Another one.";
        assertEquals(text, TxtUtils.protectAbbreviations(text, rules("Prof.", "т.д.")));
    }

    @Test
    public void onlyMatchesBehindASpace() {
        // the old pass looked for " " + rule, so an abbreviation glued to a word is not one
        same("xProf. and Prof.", rules("Prof."));
        same("Prof. at the very beginning", rules("Prof."));
    }

    @Test
    public void keepsTheOrderOfCascadingRules() {
        // "т.д." first takes the match away from "д."
        List<String> before = rules("т.д.", "д.");
        List<String> after = rules("д.", "т.д.");
        assertEquals(" т{dot}д{dot}", TxtUtils.protectAbbreviations(" т.д.", before));
        same(" т.д.", before);
        same(" т.д.", after);
        assertEquals(everyRuleOverTheWholeText(" т.д.", after), TxtUtils.protectAbbreviations(" т.д.", after));
    }

    @Test
    public void appliesRulesThatMatchTheMarkersThisPassWrites() {
        // "A." turns " A.B." into " A{dot}B.", and only then does "A{dot}B." have a match: a rule
        // like that is never found by a scan of the text as it is before the pass
        List<String> rules = rules("A.", "A{dot}B.");
        assertEquals(" A{dot}B{dot}", TxtUtils.protectAbbreviations(" A.B.", rules));
        same(" A.B.", rules);
    }

    @Test
    public void handlesRulesWithBracesOfTheirOwn() {
        List<String> rules = rules("A.", "}B.", "t}y.", "{dot}C.");
        for (String text : new String[]{" A.B.", " }B. x", " A. }B.", " t}y.", " {dot}C.", " A.C."}) {
            same(text, rules);
        }
    }

    @Test
    public void skipsRulesThatCannotChangeAnything() {
        // no dot: the old pass replaced such a rule by itself
        same(" a word x and s here", rules("x", "a", "s", ""));
    }

    @Test
    public void handlesTextsWithoutSpacesOrWithoutDots() {
        same("", rules("Prof."));
        same("   ", rules("Prof.", "."));
        same("NoSpacesAtAllProf.", rules("Prof."));
        same(" ", rules("Prof."));
        same(" .", rules("."));
        same(" ..", rules(".", ".."));
    }

    @Test
    public void handlesRulesLongerThanTheText() {
        same(" Pr", rules("Prof.", "Professor emeritus."));
    }

    @Test
    public void matchesTheOldPassOnRandomTexts() {
        List<String> rules = rules("Prof.", "Ph. D.", "Ph.", "D.", "т.д.", "т.п.", "и.о.", "C++.",
                "🙂.", "!etc.", "!т.е.", "", ".", "x", "{dot}X.", "}Y.", "Hello. {d", "no-dot", "a.b.c.");
        String[] pieces = {" ", "  ", "\t", "\n", "Prof.", "prof.", "Ph.", "D.", "Ph. D.", "т.д.",
                "т.п.", "и.о.", "C++.", "🙂.", "etc.", "т.е.", "word", "x", ".", "..", "{dot}",
                "}Y.", "a.b.c.", "A.B.", "sentence end. ", "-", "no-dot"};
        Random random = new Random(42);
        for (int round = 0; round < 3000; round++) {
            StringBuilder text = new StringBuilder();
            int parts = random.nextInt(14);
            for (int i = 0; i < parts; i++) {
                text.append(pieces[random.nextInt(pieces.length)]);
            }
            same(text.toString(), rules);
            if (round % 50 == 0) {
                Collections.shuffle(rules, random);
            }
        }
    }

    @Test
    public void matchesTheOldPassWithTheBundledDictionary() throws Exception {
        List<String> rules = bundledDictionary();
        assertTrue("the dictionary should hold thousands of rules", rules.size() > 3000);

        // every rule in one text, so each one of them has a match to find
        same(" " + String.join(" ", rules) + " and some ordinary trailing text.", rules);

        // and a page of prose with a few abbreviations in it
        StringBuilder prose = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            prose.append("Ein ganz gewöhnlicher Satz ohne alles, der einfach weitergeht. ");
            if (i % 37 == 0) {
                prose.append("Es waren т.д. und abt. и.о. dabei. ");
            }
        }
        same(prose.toString(), rules);
    }

    static List<String> bundledDictionary() throws Exception {
        List<String> rules = new ArrayList<>();
        for (String line : Files.readAllLines(
                Paths.get("src/main/assets/dict/Librera_Сокращения.txt"), StandardCharsets.UTF_8)) {
            String r = line.trim();
            if (!r.isEmpty()) {
                rules.add(r);
                rules.add(r.startsWith("!") ? TxtUtils.secondUppercase(r) : TxtUtils.firstUppercase(r));
            }
        }
        return rules;
    }
}
