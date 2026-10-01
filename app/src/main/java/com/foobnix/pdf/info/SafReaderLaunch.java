package com.foobnix.pdf.info;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import com.foobnix.LibreraApp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Authenticate internal SAF handoffs to the exported reader activities. */
public final class SafReaderLaunch {
    private static final String ORIGINAL = "SAF_ORIGINAL_URI";
    private static final String SIGNATURE = "SAF_LAUNCH_SIGNATURE";
    private static byte[] secret;
    private SafReaderLaunch() {}

    private static synchronized byte[] secret(Context context) {
        if (secret != null) return secret;
        // One app-private key survives notification/process restarts; it is never exported
        // with profiles or reading preferences. It is independent of the selected profile.
        android.content.SharedPreferences prefs = context.getSharedPreferences("SafReaderLaunch", Context.MODE_PRIVATE);
        String saved = prefs.getString("key", null);
        if (saved != null) secret = Base64.decode(saved, Base64.NO_WRAP);
        else {
            byte[] created = new byte[32];
            new SecureRandom().nextBytes(created);
            if (!prefs.edit().putString("key", Base64.encodeToString(created, Base64.NO_WRAP)).commit())
                throw new IllegalStateException("Cannot persist reader launch key");
            secret = created;
        }
        return secret;
    }

    private static byte[] signature(Context context, Intent intent, String original) throws Exception {
        if (intent.getData() == null) throw new IllegalArgumentException("Reader data missing");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret(context), "HmacSHA256"));
        return mac.doFinal((intent.getData().toString() + "\n" + original).getBytes(StandardCharsets.UTF_8));
    }

    public static void attach(Context context, Intent intent, String original) {
        if (original == null || original.isEmpty()) return;
        String identity = SafDocumentIdentity.canonical(Uri.parse(original)).toString();
        if (!"content".equals(Uri.parse(identity).getScheme())) throw new IllegalArgumentException("Not a SAF identity");
        try {
            intent.putExtra(ORIGINAL, identity);
            intent.putExtra(SIGNATURE, Base64.encodeToString(signature(context, intent, identity), Base64.NO_WRAP));
        } catch (Exception failure) { throw new IllegalStateException("Cannot authenticate reader launch", failure); }
    }

    public static String original(Intent intent) {
        if (intent == null) return null;
        try {
            String original = intent.getStringExtra(ORIGINAL);
            String signed = intent.getStringExtra(SIGNATURE);
            if (original == null || signed == null || LibreraApp.context == null) return null;
            return MessageDigest.isEqual(Base64.decode(signed, Base64.NO_WRAP),
                    signature(LibreraApp.context, intent, original)) ? original : null;
        } catch (Exception invalid) { return null; }
    }
}
