package com.google.android.gms.auth.api.identity;

import android.app.Activity;
import android.content.Context;

/**
 * Stub of the Google Play services class, for the flavours built without them.
 *
 * Those flavours hide the Google Drive sync altogether - {@link com.foobnix.pdf.info.wrapper.UITab}
 * drops its tab and the settings screen leaves out its section - so nothing here is ever reached.
 * It exists only so {@link com.foobnix.drive.GoogleAuth}, which lives in the shared sources,
 * compiles. {@link com.foobnix.drive.GoogleAuth#authorize} catches what a null client throws and
 * reports the failure, so even a call that did get through would do no harm.
 */
public final class Identity {
    public static AuthorizationClient getAuthorizationClient(Activity activity) {
        return null;
    }

    public static AuthorizationClient getAuthorizationClient(Context context) {
        return null;
    }
}
