package com.google.android.gms.common;

import android.content.Context;

public class GoogleApiAvailability {
    static GoogleApiAvailability instance = new GoogleApiAvailability();
    public static GoogleApiAvailability getInstance() {
        return instance;
    }

    public int isGooglePlayServicesAvailable(Context context) {
        return  ConnectionResult.FAIL;
    }
}
