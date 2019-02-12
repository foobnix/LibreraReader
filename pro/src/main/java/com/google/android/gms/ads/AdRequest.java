package com.google.android.gms.ads;

public class AdRequest {

    public static final int DEVICE_ID_EMULATOR = 1;
    public static Builder builder = new Builder();

    public static class Builder {

        public Builder addTestDevice(int id) {
            return builder;
        }

        public Builder addTestDevice(String id) {
            return builder;
        }

        public AdRequest build() {
            return new AdRequest();
        }

    }
}
