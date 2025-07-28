package mobi.librera.appcompose.booksync

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.Uri
import coil3.toCoilUri
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mobi.librera.appcompose.App
import mobi.librera.appcompose.R
import mobi.librera.appcompose.room.BookRepository

data class User(val name: String, val email: String, val photoUrl: Uri?)

fun FirebaseUser?.toUser(): User = User(
    this?.displayName.orEmpty(), this?.email.orEmpty(), this?.photoUrl?.toCoilUri()
)

sealed class SingInState {
    data object NotSignIn : SingInState()
    data class Success(val user: User) : SingInState()
    data class Error(val message: String) : SingInState()
}


class GoogleSingInViewModel(private val bookRepository: BookRepository) : ViewModel() {

    private val _state = MutableStateFlow<SingInState>(SingInState.NotSignIn)
    val singInState: StateFlow<SingInState> = _state


    private fun observeFirestoreAndSyncToRoom() {
        println("Sync: ${App.DEVICE_ID}")
        FirestoreBooksRepository.listenToNotes { remoteBooks ->
            viewModelScope.launch(Dispatchers.IO) {
                val localBooks = bookRepository.getAllBookState()
                //val all = bookRepository.getAllBooks().last()

                println("Sync remoteBooks ${remoteBooks.size}")
                println("Sync localBooks ${localBooks.size}")

                val allKeys = localBooks.map { it.fileName }.toMutableSet()
                allKeys.addAll(remoteBooks.map { it.fileName })


                allKeys.forEach { fileName ->
                    val remoteBook = remoteBooks.find { it.fileName == fileName }
                    val localBook = localBooks.find { it.fileName == fileName }
                    if (remoteBook == null && localBook == null) {
                        println("Sync skip")
                    } else if (remoteBook == null && localBook != null) {
                        FirestoreBooksRepository.syncBook(localBook)
                        println("Sync Remote syncBook $localBook")
                    } else if (localBook == null && remoteBook != null) {


                        bookRepository.insertBookState(remoteBook)
                        println("Sync Local insertBook $remoteBook")
                    }
                    if (remoteBook != null && localBook != null) {
                        if (remoteBook.time == localBook.time) {
                            println("Sync skip by time")
                        } else if (remoteBook.time > localBook.time) {
                            remoteBook.bookPaths = remoteBook.bookPaths.plus(localBook.bookPaths)
                            bookRepository.insertBookState(remoteBook)
                            println("Sync Local insertBook2 $localBook")
                        } else {
                            localBook.bookPaths = localBook.bookPaths.plus(remoteBook.bookPaths)
                            FirestoreBooksRepository.syncBook(localBook)
                            println("Sync Remote syncBook2 $localBook")
                        }
                    }
                }

            }
        }
    }


    init {
        observeFirestoreAndSyncToRoom()
    }


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
            GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false)
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

                observeFirestoreAndSyncToRoom()

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