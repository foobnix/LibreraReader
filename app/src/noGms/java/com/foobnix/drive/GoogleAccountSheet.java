package com.foobnix.drive;

import android.app.Activity;
import android.content.Context;

/**
 * Stand-in for the flavours built without Google Play services, where Credential Manager and
 * Google's credential type are not on the classpath. See the real one in src/gms/java: those
 * flavours hide the Drive sync altogether, so nothing here is ever reached, and a call that did
 * get through would only send {@link GoogleAuth} to the system account chooser.
 */
public final class GoogleAccountSheet {

    public interface Listener {
        void picked(String email, String displayName);

        void none();
    }

    private GoogleAccountSheet() {
    }

    public static boolean isAvailable(final Context c) {
        return false;
    }

    public static void show(final Activity a, final Listener listener) {
        listener.none();
    }
}
