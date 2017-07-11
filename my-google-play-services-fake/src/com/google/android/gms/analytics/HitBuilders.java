package com.google.android.gms.analytics;

public class HitBuilders {

    public static class ScreenViewBuilder {

        public int build() {
            return 1;
        }
    }

    public static class ExceptionBuilder {

        public ExceptionBuilder setDescription(String str) {
            return this;
        }

        public ExceptionBuilder setFatal(boolean id) {
            return this;
        }

        public int build() {
            return 1;
        }

    }

}
