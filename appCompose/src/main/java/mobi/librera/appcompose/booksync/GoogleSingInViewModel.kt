package mobi.librera.appcompose.booksync

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import mobi.librera.appcompose.R

class GoogleSingInViewModel : ViewModel() {


    suspend fun signInWithGoogle(context: Context) {

        val credentialManager = CredentialManager.create(context)

        val serverClientId = context.getString(R.string.default_web_client_id)

        val googleIdOption: GetGoogleIdOption =
            GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(true)
                .setServerClientId(serverClientId).build()

        val request: GetCredentialRequest =
            GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        // coroutineScope {
        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
            //handleSignIn(result)
            //handleSignInWithGoogleOption(result)
        } catch (e: GetCredentialException) {
            println(e)
        }
        //}

    }
}