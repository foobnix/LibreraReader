package com.google.android.ump;

public class ConsentRequestParameters {
    public static class Builder {

        public Builder() {
        }


        public Builder setConsentDebugSettings(ConsentDebugSettings debugSettings) {
            return this;
        }

        public Builder setTagForUnderAgeOfConsent(boolean b) {
            return this;
        }

        public ConsentRequestParameters build() {
            return new ConsentRequestParameters();
        }
    }
}
