package org.ebookdroid.common.touch;

import android.content.Context;
import android.view.GestureDetector;

public class DefaultGestureDetector extends GestureDetector implements IGestureDetector {

    public DefaultGestureDetector(final Context context, final OnGestureListener listener) {
        super(context, listener);
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
