package org.ebookdroid.droids.foliate;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;

/**
 * An empty activity for the foliate tests to run against.
 *
 * The engine parks its WebView in whatever activity is on screen, because Chromium builds no
 * subframe tree for a WebView outside a window - see
 * {@link org.ebookdroid.droids.foliate.codec.FoliateRenderer}. In the app that activity is the
 * reader; here it is this. Debug builds only - it is in the debug source set.
 */
public class FoliateHostActivity extends Activity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new FrameLayout(this));
    }
}
