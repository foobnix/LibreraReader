package com.google.android.ump;

import android.app.Activity;

public class UserMessagingPlatform {
    public static ConsentInformation getConsentInformation(Object mainTabs2) {
        return new ConsentInformation();
    }

    public static void loadConsentForm(Object o, UserMessagingPlatform.OnConsentFormLoadSuccessListener onConsentFormLoadSuccessListener, UserMessagingPlatform.OnConsentFormLoadFailureListener onConsentFormLoadFailureListener) {
    }

    public static void showPrivacyOptionsForm(Activity activity, ConsentForm.OnConsentFormDismissedListener onConsentFormDismissedListener) {
    }

    public interface OnConsentFormLoadFailureListener {
        void onConsentFormLoadFailure(FormError formError);
    }

    public interface OnConsentFormLoadSuccessListener {
        void onConsentFormLoadSuccess(ConsentForm consentForm);
    }
}
