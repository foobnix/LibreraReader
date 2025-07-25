package mobi.librera.appcompose.booksync

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import coil3.Uri
import coil3.toCoilUri
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mobi.librera.appcompose.R

data class User(val name: String, val email: String, val photoUrl: Uri?)

fun FirebaseUser?.toUser(): User = User(
    this?.displayName.orEmpty(),
    this?.email.orEmpty(),
    this?.photoUrl?.toCoilUri()
)

sealed class SingInState {
    data object NotSignIn : SingInState()
    data class Success(val user: User) : SingInState()
    data class Error(val message: String) : SingInState()
}


class GoogleSingInViewModel() : ViewModel() {

    private val _state = MutableStateFlow<SingInState>(SingInState.NotSignIn)
    val singInState: StateFlow<SingInState> = _state


    fun checkSingInState() {
        val auth = Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _state.value = SingInState.NotSignIn
        } else {
            _state.value = SingInState.Success(currentUser.toUser())
        }
    }

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
                _state.value = SingInState.Error(e.message.orEmpty())
            }
        }

    }

    private fun handleSignInWithGoogleOption(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)

                    } catch (e: GoogleIdTokenParsingException) {
                        e.printStackTrace()
                        _state.value = SingInState.Error(e.message.orEmpty())
                    }
                } else {
                    _state.value = SingInState.Error("Unexpected type of credential")
                }
            }

            else -> {
                _state.value = SingInState.Error("Unexpected Error")

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
                _state.value = SingInState.Success(user.toUser())
            } else {
                _state.value = SingInState.Error(task.exception?.message.orEmpty())
            }
        }
    }

    suspend fun signOut(context: Context) {
        val auth = Firebase.auth

        val credentialManager = CredentialManager.create(context)

        val clearRequest = ClearCredentialStateRequest()
        coroutineScope {
            credentialManager.clearCredentialState(clearRequest)
        }
        
        auth.signOut()

        _state.value = SingInState.NotSignIn
    }


}