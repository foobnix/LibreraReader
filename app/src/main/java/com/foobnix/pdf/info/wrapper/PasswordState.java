package com.foobnix.pdf.info.wrapper;

import android.content.Context;
import android.content.SharedPreferences;

import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;

public class PasswordState {

    private static PasswordState instance = new PasswordState();

    public static PasswordState get() {
        return instance;
    }

    private static final String PASSWORD_STATE = "PasswordState";

    public boolean isFingerPrintPassword = false;
    public String appPassword = "";
    /**
     * Whether the app asks for its password. Kept here, in the app's private storage: in the
     * settings on shared storage (AppState) any app able to write them could switch it off.
     */
    public boolean isAppPassword = false;
    /** The proxy's password, here for the same reason: AppState's file is readable by others. */
    public String proxyPassword = "";


    public boolean hasPassword(){
        return TxtUtils.isNotEmpty(PasswordState.get().appPassword) || PasswordState.get().isFingerPrintPassword;

    }

    public void save(final Context a) {
        if (a == null) {
            return;
        }
        SharedPreferences sp = a.getSharedPreferences(PASSWORD_STATE, Context.MODE_PRIVATE);
        Objects.saveToSP(PasswordState.get(), sp);
    }

    public void load(final Context a) {
        if (a == null) {
            return;
        }
        SharedPreferences sp = a.getSharedPreferences(PASSWORD_STATE, Context.MODE_PRIVATE);
        // Saved here before? Then the switch is this device's own from then on.
        final boolean first = !sp.contains("isAppPassword");
        Objects.loadFromSp(this, sp);

        // Both were kept in AppState before: taken over from there once, and the password no
        // longer left in it. AppState is loaded first (AppProfile.init).
        boolean moved = false;
        if (first && AppState.get().isAppPassword) {
            isAppPassword = true;
            moved = true;
        }
        if (TxtUtils.isNotEmpty(AppState.get().proxyPassword)) {
            if (TxtUtils.isEmpty(proxyPassword)) {
                proxyPassword = AppState.get().proxyPassword;
            }
            AppState.get().proxyPassword = "";
            moved = true;
        }
        if (moved) {
            save(a);
        }
    }

    /** Switches the password on or off, here and in AppState, which other devices sync. */
    public void setAppPassword(boolean on) {
        isAppPassword = on;
        AppState.get().isAppPassword = on;
    }

}