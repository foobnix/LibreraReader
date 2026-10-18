package com.google.android.gms.auth.api.identity;

import android.app.PendingIntent;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

/**
 * Stub of the Google Play services class, for the flavours built without them. See
 * {@link Identity}.
 */
public class AuthorizationResult {
    public boolean hasResolution() {
        return false;
    }

    public PendingIntent getPendingIntent() {
        return null;
    }

    public GoogleSignInAccount toGoogleSignInAccount() {
        return null;
    }
}
