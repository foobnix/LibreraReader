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

package cue.lang.stop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import cue.lang.Counter;
import cue.lang.WordIterator;

/**
 * 
 * @author Jonathan Feinberg <jdf@us.ibm.com>
 * 
 */
public enum StopWords {
    Arabic(), Armenian(), Catalan(true), Croatian(), Czech(), Dutch(), //
    Danish(), English(), Esperanto(), Farsi(), Finnish(), //
    French(true), German(), Greek(), Hindi(), Hungarian(), //
    Italian(), Latin(), Norwegian(), Polish(), Portuguese(), //
    Romanian(), Russian(), Slovenian(), Slovak(), Spanish(), //
    Swedish(), Hebrew(), Turkish(), Custom();

    public static StopWords guess(final String text) {
        return guess(new Counter<String>(new WordIterator(text)));
    }

    public static StopWords guess(final Counter<String> wordCounter) {
        return guess(wordCounter.getMostFrequent(50));
    }

    public static StopWords guess(final Collection<String> words) {
        StopWords currentWinner = null;
        int currentMax = 0;
        for (final StopWords stopWords : StopWords.values()) {
            int count = 0;
            for (final String word : words) {
                if (stopWords.isStopWord(word)) {
                    count++;
                }
            }
            if (count > currentMax) {
                currentWinner = stopWords;
                currentMax = count;
            }
        }
        return currentWinner;
    }

    public final boolean stripApostrophes;
    private final Set<String> stopwords = new HashSet<String>();

    private StopWords() {
        this(false);
    }

    private StopWords(final boolean stripApostrophes) {
        this.stripApostrophes = stripApostrophes;
        loadLanguage();
    }

    public boolean isStopWord(final String s) {
        if (s.length() == 1) {
            return true;
        }
        // check rightquotes as apostrophes
        return stopwords.contains(s.replace('\u2019', '\'').toLowerCase(Locale.ENGLISH));
    }

    private void loadLanguage() {
        final String wordlistResource = name().toLowerCase(Locale.ENGLISH);
        if (!wordlistResource.equals("custom")) {
            readStopWords(getClass().getResourceAsStream(wordlistResource),
                    Charset.forName("UTF-8"));
        }
    }

    public void readStopWords(final InputStream inputStream, final Charset encoding) {
        try {
            final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream,
                    encoding));
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.replaceAll("\\|.*", "").trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    for (final String w : line.split("\\s+")) {
                        stopwords.add(w.toLowerCase(Locale.ENGLISH));
                    }
                }
            } finally {
                in.close();
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
