package com.foobnix.drive;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.concurrent.Executor;

/**
 * The "Sign in with Google" sheet the Drive sync picks its account from - the one that names the
 * app, shows each account's picture and takes a single tap, in place of the bare list of addresses
 * the system account chooser draws.
 *
 * It is Credential Manager's, shown by Google Play services, and it needs the app to name itself
 * with an OAuth client of its own Google project: the {@code librera.GOOGLE_WEB_CLIENT_ID}
 * meta-data, a <b>Web application</b> client of the project that holds the app's Android client.
 * Google checks the three of them against each other - the web client, the package and the
 * signing certificate - and a client that is of the wrong kind, or of another project, makes the
 * sheet fail on the account the user picks with "Developer console is not set up correctly".
 * Where the manifest names none, the web client of the app's own Firebase project is used, the
 * one the google-services plugin takes out of google-services.json - so a build whose signing
 * certificate is registered with that project needs no client of its own here.
 *
 * The flavours built without Google Play services have their own copy of this class in
 * src/noGms/java, which reports the sheet unavailable and nothing more.
 */
public final class GoogleAccountSheet {

    /** How the sheet answers: with the account the user picked, or with nothing. */
    public interface Listener {
        /**
         * @param email       the address of the account picked
         * @param displayName the name Google shows for it, or an empty string
         */
        void picked(String email, String displayName);

        /** The sheet could not be shown, Google refused it, or the user dismissed it. */
        void none();
    }

    private GoogleAccountSheet() {
    }

    /**
     * The client the sheet names the app by, or an empty string when the build has none: the
     * manifest's, where a build sets one of its own, and otherwise the web client of the app's
     * Firebase project, which the google-services plugin writes into the resources from
     * google-services.json - the same one LibreraX signs in with.
     */
    private static String clientId(final Context c) {
        final String own = TxtUtils.nullToEmpty(Apps.getMetaData(c, "librera.GOOGLE_WEB_CLIENT_ID"));
        if (TxtUtils.isNotEmpty(own)) {
            return own;
        }
        try {
            final int id = c.getResources().getIdentifier("default_web_client_id", "string", c.getPackageName());
            return id == 0 ? "" : TxtUtils.nullToEmpty(c.getString(id));
        } catch (Throwable e) {
            LOG.e(e);
            return "";
        }
    }

    public static boolean isAvailable(final Context c) {
        return TxtUtils.isNotEmpty(clientId(c));
    }

    /**
     * Shows the sheet. The listener is called on the main thread, exactly once.
     */
    public static void show(final Activity a, final Listener listener) {
        final String clientId = clientId(a);
        if (TxtUtils.isEmpty(clientId)) {
            GFile.debug("\nSign-in: this build names no Google client to sign in with");
            listener.none();
            return;
        }
        final Handler main = new Handler(Looper.getMainLooper());
        final Executor onMain = main::post;
        try {
            CredentialManager.create(a).getCredentialAsync(
                    a,
                    new GetCredentialRequest.Builder()
                            .addCredentialOption(new GetSignInWithGoogleOption.Builder(clientId).build())
                            .build(),
                    null,
                    onMain,
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

                        @Override public void onResult(final GetCredentialResponse response) {
                            read(response, listener);
                        }

                        @Override public void onError(final GetCredentialException e) {
                            // The user dismissed the sheet, the device has no account to offer,
                            // or Google would not sign this build in at all - which is what an
                            // OAuth client it does not know its way back to looks like from here.
                            LOG.e(e);
                            GFile.debug("\nSign-in: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                            listener.none();
                        }
                    });
        } catch (Throwable e) {
            LOG.e(e);
            listener.none();
        }
    }

    private static void read(final GetCredentialResponse response, final Listener listener) {
        try {
            final androidx.credentials.Credential credential = response.getCredential();
            if (credential instanceof CustomCredential
                    && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                final GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(credential.getData());
                listener.picked(TxtUtils.nullToEmpty(google.getId()), TxtUtils.nullToEmpty(google.getDisplayName()));
                return;
            }
            GFile.debug("\nSign-in: Google handed back no account");
        } catch (Throwable e) {
            LOG.e(e);
        }
        listener.none();
    }
}
