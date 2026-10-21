package com.google.android.gms.auth.api.identity;

import android.content.Intent;

import com.google.android.gms.tasks.Task;

/**
 * Stub of the Google Play services interface, for the flavours built without them. See
 * {@link Identity}.
 */
public interface AuthorizationClient {
    Task<AuthorizationResult> authorize(AuthorizationRequest request);

    AuthorizationResult getAuthorizationResultFromIntent(Intent intent);
}
