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
 *
 * <h3>Which account, and why the picker comes first</h3>
 *
 * The authorization API grants a scope; naming the account is the caller's job. Asked without one
 * it picks the account itself, and the {@link AuthorizationResult} it answers with need not carry
 * that account back: {@code toGoogleSignInAccount()} is only filled in when the grant came with
 * tokens, and even then it carries no address unless the request asked for the profile scopes the
 * sync has no use for. A sign-in that read the account off the result alone therefore had nothing
 * to store on the very devices where the scope was already granted - Google answered at once,
 * nothing appeared on screen, and the button went on saying "Sign in" with no word of why.
 *
 * So the account is settled before Google is asked anything: {@link GoogleAccountSheet} names it,
 * {@link AuthorizationRequest.Builder#setAccount} asks for the scope for that one account, and it
 * is that name - not whatever the result carries - that is stored once the grant is in.
 */
public class GoogleAuth {

    /** How AccountManager names a Google account, which is what GoogleAccountCredential expects. */
    private static final String ACCOUNT_TYPE = "com.google";

    private static final String PREFS = "gdrive-account";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DISPLAY_NAME = "displayName";
    /**
     * The account the user picked, kept only until the grant for it is in. It outlives the account
     * chooser and the consent screen, either of which can take the app's process down with it.
     */
    private static final String KEY_PENDING_EMAIL = "pendingEmail";
    /** The name Google shows for the account the user picked, kept beside its address. */
    private static final String KEY_PENDING_NAME = "pendingName";

    /** What to run when the sign-in the activity is in the middle of ends, one way or the other. */
    private static Runnable onAuthorized;
    private static Runnable onFailed;

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
    private static AuthorizationRequest request(final Account account) {
        return AuthorizationRequest.builder()
                                   .setRequestedScopes(Collections.singletonList(new Scope(GFile.MY_SCOPE)))
                                   .setAccount(account)
                                   .build();
    }

    /**
     * Starts a sign-in by asking the user which Google account to sync through, on the "Sign in
     * with Google" sheet and nowhere else. The rest of it - the Drive scope for that account, and
     * the consent screen when Google wants one - follows from {@link #onActivityResult}, which
     * the activity hands every answer to.
     *
     * @param onAuthorized run on the main thread once an account is stored
     * @param onFailed     run when Google refused, or the user backed out
     */
    public static void authorize(final Activity a, final Runnable onAuthorized, final Runnable onFailed) {
        GoogleAuth.onAuthorized = onAuthorized;
        GoogleAuth.onFailed = onFailed;
        prefs(a).edit().remove(KEY_PENDING_EMAIL).apply();

        GoogleAccountSheet.show(a, new GoogleAccountSheet.Listener() {

            @Override public void picked(final String email, final String displayName) {
                accountPicked(a, email, displayName);
            }

            @Override public void none() {
                failed(a);
            }
        });
    }

    /**
     * An answer to the consent screen a sign-in sends the user through.
     *
     * @return <code>true</code> when this was a sign-in's answer and has been dealt with
     */
    public static boolean onActivityResult(final Activity a, final int requestCode, final int resultCode,
                                           final Intent data) {
        if (requestCode == GFile.REQUEST_CODE_SIGN_IN) {
            onConsentResult(a, resultCode, data);
            return true;
        }
        return false;
    }

    /**
     * The sheet named an account: ask Google for the Drive scope for it. The name is kept with
     * it - the sheet knows it, and the authorization result never does.
     */
    private static void accountPicked(final Activity a, final String email, final String displayName) {
        if (TxtUtils.isEmpty(email)) {
            GFile.debug("\nSign-in: no account chosen");
            failed(a);
            return;
        }
        prefs(a).edit()
                .putString(KEY_PENDING_EMAIL, email)
                .putString(KEY_PENDING_NAME, TxtUtils.nullToEmpty(displayName))
                .apply();
        GFile.debug("\nSign-in: " + email);
        requestScope(a, new Account(email, ACCOUNT_TYPE));
    }

    /**
     * Asks for the Drive scope for one account, showing the consent screen when Google wants one.
     *
     * Google answers in one of two ways. When the scope has already been granted - the user
     * authorized this app before, or another device of theirs did - the result carries the grant
     * at once and nothing appears on screen. Otherwise the result carries an Intent to send the
     * user through, and the answer to that arrives in the activity's {@code onActivityResult}
     * under {@link GFile#REQUEST_CODE_SIGN_IN}.
     */
    private static void requestScope(final Activity a, final Account account) {
        try {
            Identity.getAuthorizationClient(a)
                    .authorize(request(account))
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
                                GFile.debug("\nSign-in: the consent screen would not open");
                                failed(a);
                                return;
                            }
                        }
                        // Granted already: there is nothing to ask the user
                        if (store(a, result)) {
                            authorized(a);
                        } else {
                            failed(a);
                        }
                    })
                    .addOnFailureListener(e -> {
                        LOG.e(e);
                        GFile.debug("\nSign-in: Google refused: " + e.getMessage());
                        failed(a);
                    });
        } catch (Throwable e) {
            // No Google Play services on the device, or a flavour built without them
            LOG.e(e);
            GFile.debug("\nSign-in: no Google Play services");
            failed(a);
        }
    }

    /** The consent screen came back. */
    private static void onConsentResult(final Activity a, final int resultCode, final Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            GFile.debug("\nSign-in: cancelled");
            failed(a);
            return;
        }
        final AuthorizationResult result;
        try {
            result = Identity.getAuthorizationClient(a).getAuthorizationResultFromIntent(data);
        } catch (Throwable e) {
            // Google refuses the same way the user does - by throwing here
            LOG.e(e);
            GFile.debug("\nSign-in: Google Drive access was not granted");
            failed(a);
            return;
        }
        if (store(a, result)) {
            authorized(a);
        } else {
            failed(a);
        }
    }

    /**
     * Keeps the account the Drive scope has just been granted for: the one the user picked, under
     * the name the result carries when it carries one.
     *
     * @return <code>false</code> when there is no account to keep, which leaves the stored one
     *         untouched: a failed re-authorization must not sign a working account out
     */
    private static boolean store(final Context c, final AuthorizationResult result) {
        final GoogleSignInAccount account = result == null ? null : result.toGoogleSignInAccount();
        String email = account == null ? null : account.getEmail();
        if (TxtUtils.isEmpty(email)) {
            // The usual answer: a grant carries tokens, not an address. The account is the one the
            // user picked, which is the one the scope was just asked for.
            email = TxtUtils.nullToEmpty(prefs(c).getString(KEY_PENDING_EMAIL, ""));
        }
        if (TxtUtils.isEmpty(email)) {
            GFile.debug("\nSign-in: no account to sync through");
            LOG.d(GFile.TAG, "authorize", "the result carries no account");
            return false;
        }
        String name = account == null ? null : account.getDisplayName();
        if (TxtUtils.isEmpty(name)) {
            name = prefs(c).getString(KEY_PENDING_NAME, "");
        }
        try {
            prefs(c).edit()
                    .putString(KEY_EMAIL, email)
                    .putString(KEY_DISPLAY_NAME, TxtUtils.nullToEmpty(name))
                    .apply();
            GFile.debug("\nSign-in: Google Drive access granted");
            LOG.d(GFile.TAG, "authorize", "granted", email);
            return true;
        } catch (Throwable e) {
            LOG.e(e);
            return false;
        }
    }

    private static void authorized(final Context c) {
        final Runnable run = onAuthorized;
        done(c);
        if (run != null) {
            run.run();
        }
    }

    private static void failed(final Context c) {
        final Runnable run = onFailed;
        done(c);
        if (run != null) {
            run.run();
        }
    }

    /** The sign-in is over: nothing is pending and no callback is owed twice. */
    private static void done(final Context c) {
        onAuthorized = null;
        onFailed = null;
        try {
            prefs(c).edit().remove(KEY_PENDING_EMAIL).remove(KEY_PENDING_NAME).apply();
        } catch (Throwable e) {
            LOG.e(e);
        }
    }

    /**
     * Forgets the account, which is what signing out amounts to here. The scope grant itself is
     * left with Google, exactly as the old {@code GoogleSignInClient.signOut()} left it, so
     * signing back in to the same account needs no second trip through the consent screen.
     */
    public static void clear(final Context c) {
        try {
            prefs(c).edit()
                    .remove(KEY_EMAIL)
                    .remove(KEY_DISPLAY_NAME)
                    .remove(KEY_PENDING_EMAIL)
                    .remove(KEY_PENDING_NAME)
                    .apply();
        } catch (Throwable e) {
            LOG.e(e);
        }
    }
}
