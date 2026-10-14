package com.foobnix.android.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The abbreviation pass, the one the dictionary of some four thousand rules runs over every TTS
 * page, measured against the pass it replaces. Writes build/abbreviations-benchmark.tsv.
 */
public class TxtUtilsAbbreviationsBenchmarkTest {

    private static final int RUNS = 9;

    /** The pass as it was: every rule, over the whole text. */
    private static String everyRule(String text, List<String> rules) {
        for (String r : rules) {
            if (!r.startsWith("!")) {
                text = text.replace(" " + r, " " + r.replace(".", "{dot}"));
            }
        }
        return text;
    }

    private static String page(int sentences, boolean withAbbreviations) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < sentences; i++) {
            b.append("Ein ganz gewöhnlicher Satz ohne alles, der einfach weitergeht und weitergeht. ");
            if (withAbbreviations && i % 11 == 0) {
                b.append("Dabei waren т.д. und abt. и.о. sowie Prof. Smith. ");
            }
        }
        return b.toString();
    }

    private static long median(long[] v) {
        long[] c = v.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }

    @Test
    public void abbreviationPassIsMeasuredAgainstThePassItReplaces() throws Exception {
        List<String> rules = TxtUtilsAbbreviationsTest.bundledDictionary();
        List<String[]> table = new ArrayList<>();
        table.add(new String[]{"page_chars", "abbreviations", "rules", "old_ms", "new_ms", "speedup"});

        for (int sentences : new int[]{40, 200, 800}) {
            for (boolean withAbbreviations : new boolean[]{true, false}) {
                String text = page(sentences, withAbbreviations);
                // both passes must agree before either is timed
                assertEquals(everyRule(text, rules), TxtUtils.protectAbbreviations(text, rules));

                long[] oldNs = new long[RUNS];
                long[] newNs = new long[RUNS];
                for (int i = 0; i < 3; i++) { // warmup
                    everyRule(text, rules);
                    TxtUtils.protectAbbreviations(text, rules);
                }
                for (int i = 0; i < RUNS; i++) {
                    long t0 = System.nanoTime();
                    everyRule(text, rules);
                    oldNs[i] = System.nanoTime() - t0;
                    t0 = System.nanoTime();
                    TxtUtils.protectAbbreviations(text, rules);
                    newNs[i] = System.nanoTime() - t0;
                }
                double oldMs = median(oldNs) / 1e6;
                double newMs = median(newNs) / 1e6;
                String row = String.format("%d\t%s\t%d\t%.1f\t%.2f\t%.1fx",
                        text.length(), withAbbreviations ? "yes" : "none", rules.size(), oldMs, newMs, oldMs / newMs);
                System.out.println("ABBREV " + row);
                table.add(row.split("\t"));
            }
        }
        File out = new File("build/abbreviations-benchmark.tsv");
        out.getParentFile().mkdirs();
        try (PrintWriter w = new PrintWriter(out, "UTF-8")) {
            for (String[] r : table) {
                w.println(String.join("\t", r));
            }
        }
    }
}
