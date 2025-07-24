package mobi.librera.appcompose.booksync

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mobi.librera.appcompose.R

class GoogleSingInViewModel() : ViewModel() {

    val auth = Firebase.auth

    var singInErrorMsg by mutableStateOf("")
    var userName by mutableStateOf(auth.currentUser?.displayName.orEmpty())
    var photoUrl by mutableStateOf(auth.currentUser?.photoUrl)


    private val _userEmailFlow = MutableStateFlow<String?>(auth.currentUser?.email)
    val userEmail: StateFlow<String?> = _userEmailFlow.asStateFlow()


    suspend fun signInWithGoogle(context: Context) {
        val credentialManager = CredentialManager.create(context)

        val serverClientId = context.getString(R.string.default_web_client_id)

        val googleIdOption: GetGoogleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId).build()

        val request: GetCredentialRequest =
            GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        coroutineScope {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )

                handleSignInWithGoogleOption(result)
            } catch (e: GetCredentialException) {
                e.printStackTrace()
                singInErrorMsg = e.message.orEmpty()
            }
        }

    }

    fun handleSignInWithGoogleOption(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)

                    } catch (e: GoogleIdTokenParsingException) {
                        e.printStackTrace()
                        singInErrorMsg = e.message.orEmpty()
                    }
                } else {
                    singInErrorMsg = "Unexpected type of credential"
                }
            }

            else -> {
                singInErrorMsg = "Unexpected type of credential"
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val auth = Firebase.auth
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                println("current user $user")
                //updateUI(user)
            } else {
                singInErrorMsg = "Fail sing in with firebase ${task.exception}"
                //updateUI(null)
            }
        }
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        val credentialManager = CredentialManager.create(context)

        val clearRequest = ClearCredentialStateRequest()
        coroutineScope {
            credentialManager.clearCredentialState(clearRequest)
        }

    }


}