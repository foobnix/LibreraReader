package com.google.android.ump;

public class ConsentDebugSettings {
    public static enum DebugGeography {
        DEBUG_GEOGRAPHY_EEA;
    }

    public static class Builder {
        public Builder(Object mainTabs2) {
        }

        public Builder() {
        }

        public Builder setDebugGeography(DebugGeography debugGeographyEea) {
            return this;
        }

        public Builder addTestDeviceHashedId(String byTestID) {
            return this;
        }

        public ConsentDebugSettings build() {
            return new ConsentDebugSettings();
        }
    }
}



