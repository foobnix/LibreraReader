package com.google.android.ump;

public class ConsentInformation {
    public enum ConsentStatus {
        REQUIRED, OBTAINED
    }

    public ConsentStatus getConsentStatus() {
        return ConsentStatus.REQUIRED;
    }

    public void requestConsentInfoUpdate(Object mainTabs2, ConsentRequestParameters params, Runnable o, OnConsentInfoUpdateFailureListener o1) {
    }

    public boolean isConsentFormAvailable() {
        return true;
    }

    public interface OnConsentInfoUpdateFailureListener {
        void onConsentInfoUpdateFailure(FormError formError);
    }

}

