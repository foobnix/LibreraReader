package com.foobnix.hypen;

import android.os.SystemClock;
import android.util.Log;
import com.foobnix.model.AppState;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HypenTokenizerTest {
    @Test public void largeEpubTextTokenizesWithoutChangingMarkupOrText() {
        boolean previous = AppState.get().isExperimental;
        AppState.get().isExperimental = false;
        try {
            StringBuilder input = new StringBuilder(3_000_000);
            for (int i = 0; i < 45_000; i++)
                input.append("<p>reading &amp; exploring the city and its architecture.</p>\n");
            String source = input.toString();
            StringBuilder actual = new StringBuilder(source.length());
            HypenUtils.resetTokenizer();
            long start = SystemClock.elapsedRealtime();
            HypenUtils.tokenize(source, new HypenUtils.TokensListener() {
                @Override public void findText(String text) { actual.append(text); }
                @Override public void findOther(char ch) { actual.append(ch); }
            });
            Log.i("HypenTokenizerTest", "chars=" + source.length()
                    + " elapsedMs=" + (SystemClock.elapsedRealtime() - start));
            assertEquals(source, actual.toString());
        } finally {
            AppState.get().isExperimental = previous;
        }
    }
}
