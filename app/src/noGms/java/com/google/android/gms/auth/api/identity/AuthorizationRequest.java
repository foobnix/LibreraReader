package com.google.android.gms.auth.api.identity;

import com.google.android.gms.common.api.Scope;

import java.util.List;

/**
 * Stub of the Google Play services class, for the flavours built without them. See
 * {@link Identity}.
 */
public class AuthorizationRequest {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public Builder setRequestedScopes(List<Scope> scopes) {
            return this;
        }

        public AuthorizationRequest build() {
            return new AuthorizationRequest();
        }
    }
}
