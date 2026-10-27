package com.google.android.gms.tasks;

/**
 * Stub of the Google Play services class, for the flavours built without them. See
 * {@link com.google.android.gms.auth.api.identity.Identity}.
 */
public class Task<T> {
    public Task<T> addOnSuccessListener(OnSuccessListener<? super T> listener) {
        return this;
    }

    public Task<T> addOnFailureListener(OnFailureListener listener) {
        return this;
    }
}
