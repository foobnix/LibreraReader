package com.google.android.gms.auth.api.signin;

import android.content.Context;
import android.content.Intent;


public class GoogleSignIn {
    public static GoogleSignInAccount getLastSignedInAccount(Context activity) {
        return null;
    }

    public static GoogleSignInClient getClient(Context c, GoogleSignInOptions signInOptions) {
        return null;
    }

    public static GoogleSignIn getSignedInAccountFromIntent(Intent data) {
        return null;
    }


    public GoogleSignIn addOnSuccessListener(Result result) {
        return null;
    }
    public GoogleSignIn addOnFailureListener(Result1 result) {
        return null;
    }
    public interface Result{
         void onResult(GoogleSignInAccount account);
    }
    public interface Result1{
        void onResult(Throwable account);
    }
}
