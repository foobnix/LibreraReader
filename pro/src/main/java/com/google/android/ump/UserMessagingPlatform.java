package com.google.android.ump;

public class UserMessagingPlatform {
    public static ConsentInformation getConsentInformation(Object mainTabs2) {
        return new ConsentInformation();
    }

    public static void loadConsentForm(Object o, UserMessagingPlatform.OnConsentFormLoadSuccessListener onConsentFormLoadSuccessListener, UserMessagingPlatform.OnConsentFormLoadFailureListener onConsentFormLoadFailureListener) {
    }

    public interface OnConsentFormLoadFailureListener {
        void onConsentFormLoadFailure(FormError formError);
    }

    public interface OnConsentFormLoadSuccessListener {
        void onConsentFormLoadSuccess(ConsentForm consentForm);
    }
}
