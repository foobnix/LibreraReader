package com.foobnix.android.utils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TtsAbbreviationCandidatesTest {
    private static String previous(String text, List<String> rules) {
        for (String r : rules) {
            if (!r.startsWith("!")) {
                text = text.replace(" " + r, " " + r.replace(".", "{dot}"));
            }
        }
        return text;
    }

    @Test
    public void preservesLiteralBoundariesAndCascadingRuleOrder() {
        List<String> rules = new ArrayList<>(Arrays.asList(
                "Prof.", "Ph. D.", "Ph.", "D.", "C++.", "т.д.", "🙂.", "!etc.",
                "", ".", "Hello. {d", "{dot}X.", "no-dot"));
        List<String> samples = Arrays.asList("", "Prof. speaks.", " A Prof. speaks.",
                " Ph. D. and D. Ph.", " C++. т.д. 🙂. repeated Prof. Prof.",
                " Hello. .", " .X.", "\tProf.\nProf.", " No abbreviations here.",
                " no-dot words without periods", "   ", " No match before a very distant dot.");
        Random random = new Random(0);
        for (int order = 0; order < 20; order++) {
            for (String sample : samples) {
                assertEquals(previous(sample, rules), TxtUtils.protectAbbreviations(sample, rules));
            }
            Collections.shuffle(rules, random);
        }
    }

    @Test
    public void matchesPreviousPassWithTheBundledDictionary() throws Exception {
        List<String> rules = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get("src/main/assets/dict/Librera_Сокращения.txt"), StandardCharsets.UTF_8)) {
            String r = line.trim();
            if (!r.isEmpty()) {
                rules.add(r);
                rules.add(r.startsWith("!") ? TxtUtils.secondUppercase(r) : TxtUtils.firstUppercase(r));
            }
        }
        String sample = " " + String.join(" ", rules) + " Ordinary trailing text.";
        assertEquals(previous(sample, rules), TxtUtils.protectAbbreviations(sample, rules));
    }
}
