package com.foobnix.drive;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;

import java.util.Collections;

/**
 * The Google account the Drive sync works through, and the Drive scope granted for it.
 *
 * play-services-auth 22 removed the legacy Google Sign-In entry points - {@code GoogleSignIn} and
 * {@code GoogleSignInClient} - which is what the sync used to both start a sign-in and ask
 * afterwards who was signed in. The Google Identity authorization API that replaced them does one
 * thing: it grants an app a scope for an account. It keeps no "currently signed in" state, so
 * there is no {@code getLastSignedInAccount()} to call any more, and the account the user picked
 * is remembered here instead.
 *
 * It is kept in a preferences file of its own rather than in {@link com.foobnix.model.AppSP}: that
 * one is part of the profile the sync itself uploads, and the signed-in account is of this device
 * alone - it must not travel to the user's other devices.
 *
 * Only the account name is stored, never a token. {@link GFile} goes on building its Drive client
 * from a {@link com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential},
 * which mints and refreshes tokens itself for whatever account it is given - and can, because this
 * is the account the scope was granted for.
 */
public class GoogleAuth {

    /** How AccountManager names a Google account, which is what GoogleAccountCredential expects. */
    private static final String ACCOUNT_TYPE = "com.google";

    private static final String PREFS = "gdrive-account";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DISPLAY_NAME = "displayName";

    private static SharedPreferences prefs(final Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * @return the address of the authorized account, or an empty string when there is none
     */
    public static String getAccountEmail(final Context c) {
        if (c == null) {
            return "";
        }
        try {
            return TxtUtils.nullToEmpty(prefs(c).getString(KEY_EMAIL, ""));
        } catch (Throwable e) {
            LOG.e(e);
            return "";
        }
    }

    /**
     * Whether the sync has an account to work through. This is what the app asks in place of the
     * {@code GoogleSignIn.getLastSignedInAccount(context) != null} it used to.
     */
    public static boolean isAuthorized(final Context c) {
        return TxtUtils.isNotEmpty(getAccountEmail(c));
    }

    /**
     * @return the authorized account, or <code>null</code> when there is none
     */
    public static Account getAccount(final Context c) {
        final String email = getAccountEmail(c);
        return TxtUtils.isEmpty(email) ? null : new Account(email, ACCOUNT_TYPE);
    }

    /**
     * The account as the settings screen shows it: the display name and the address, or just the
     * address when Google gave no name, or an empty string when no account is authorized.
     */
    public static String getDisplayInfo(final Context c) {
        final String email = getAccountEmail(c);
        if (TxtUtils.isEmpty(email)) {
            return "";
        }
        final String name = TxtUtils.nullToEmpty(prefs(c).getString(KEY_DISPLAY_NAME, ""));
        return TxtUtils.isEmpty(name) ? email : name + " (" + email + ")";
    }

    /** The one scope the sync needs, the same one the Drive credential asks for a token for. */
    private static AuthorizationRequest request() {
        return AuthorizationRequest.builder()
                                   .setRequestedScopes(Collections.singletonList(new Scope(GFile.MY_SCOPE)))
                                   .build();
    }

    /**
     * Asks for the Drive scope, showing the account picker and the consent screen when Google
     * wants them.
     *
     * Google answers in one of two ways. When the scope has already been granted - the user
     * authorized this app before, or another device of theirs did - the result carries the account
     * at once and {@code onAuthorized} runs without anything appearing on screen. Otherwise the
     * result carries an Intent to send the user through, and the answer to that arrives in the
     * activity's {@code onActivityResult} under {@link GFile#REQUEST_CODE_SIGN_IN}, where
     * {@link #readResult(Activity, Intent)} finishes the job.
     *
     * @param onAuthorized run on the main thread once an account is stored
     * @param onFailed     run when Google refused, or the user backed out of the consent screen
     */
    public static void authorize(final Activity a, final Runnable onAuthorized, final Runnable onFailed) {
        try {
            Identity.getAuthorizationClient(a)
                    .authorize(request())
                    .addOnSuccessListener(result -> {
                        if (result.hasResolution()) {
                            try {
                                a.startIntentSenderForResult(
                                        result.getPendingIntent().getIntentSender(),
                                        GFile.REQUEST_CODE_SIGN_IN, null, 0, 0, 0);
                                // The answer comes back through onActivityResult
                                return;
                            } catch (Throwable e) {
                                LOG.e(e);
                                onFailed.run();
                                return;
                            }
                        }
                        // Granted already: there is nothing to ask the user
                        if (store(a, result)) {
                            onAuthorized.run();
                        } else {
                            onFailed.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        LOG.e(e);
                        onFailed.run();
                    });
        } catch (Throwable e) {
            // No Google Play services on the device, or a flavour built without them
            LOG.e(e);
            onFailed.run();
        }
    }

    /**
     * Reads the answer to the consent screen {@link #authorize} sent the user through.
     *
     * @param data the Intent handed to {@code onActivityResult}
     * @return <code>true</code> when an account was authorized and stored
     */
    public static boolean readResult(final Activity a, final Intent data) {
        if (data == null) {
            LOG.d(GFile.TAG, "authorize", "cancelled");
            return false;
        }
        try {
            return store(a, Identity.getAuthorizationClient(a).getAuthorizationResultFromIntent(data));
        } catch (Throwable e) {
            LOG.e(e);
            return false;
        }
    }

    /**
     * Keeps the account an authorization result was granted for.
     *
     * @return <code>false</code> when the result named no account, which leaves the stored one
     *         untouched: a failed re-authorization must not sign a working account out
     */
    private static boolean store(final Context c, final AuthorizationResult result) {
        final GoogleSignInAccount account = result == null ? null : result.toGoogleSignInAccount();
        final String email = account == null ? null : account.getEmail();
        if (TxtUtils.isEmpty(email)) {
            LOG.d(GFile.TAG, "authorize", "the result carries no account");
            return false;
        }
        try {
            prefs(c).edit()
                    .putString(KEY_EMAIL, email)
                    .putString(KEY_DISPLAY_NAME, TxtUtils.nullToEmpty(account.getDisplayName()))
                    .apply();
            LOG.d(GFile.TAG, "authorize", "granted", email);
            return true;
        } catch (Throwable e) {
            LOG.e(e);
            return false;
        }
    }

    /**
     * Forgets the account, which is what signing out amounts to here. The scope grant itself is
     * left with Google, exactly as the old {@code GoogleSignInClient.signOut()} left it, so
     * signing back in to the same account needs no second trip through the consent screen.
     */
    public static void clear(final Context c) {
        try {
            prefs(c).edit().remove(KEY_EMAIL).remove(KEY_DISPLAY_NAME).apply();
        } catch (Throwable e) {
            LOG.e(e);
        }
    }
}
