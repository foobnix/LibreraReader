package mobi.librera.lib.gdrive

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch


@Composable
fun GDriveButton(
    googleDriveHelper: GoogleDriveHelper,
    isSignedIn: Boolean,
    onSingIn: () -> Unit,
    onSingOut: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val googleSignInClient =
        remember { GoogleSignIn.getClient(context, googleDriveHelper.getGoogleSignInOptions()) }

    // Google Sign-In launcher for OAuth authorization
    val googleSignInLauncher =
        rememberLauncherForActivityResult(StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                googleDriveHelper.initializeDriveService(account)
                onSingIn()
                Toast.makeText(context, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "Signed in as: ${account.email}")
            } catch (e: ApiException) {
                Log.e("MainActivity", "Google sign-in failed", e)
                Toast.makeText(
                    context,
                    "Drive authorization failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    if (!isSignedIn) {
        Button(
            modifier = Modifier.padding(start = 12.dp),
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect Google Drive")
        }
    } else {
        Button(
            modifier = Modifier.padding(start = 12.dp),
            onClick = {
                scope.launch {
                    val result = googleDriveHelper.signOut()
                    if (result.isSuccess) {
                        onSingOut()
                        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            context,
                            "Failed to sign out: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Disconnect Google Drive")
        }
    }
}

@Composable
fun GoogleSignInScreen(
    clientId: String
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val viewModel: GoogleSingInViewModel = viewModel()
    val singInState by viewModel.singInState.collectAsState()

    var isSignedIn by remember { mutableStateOf(false) }
    val googleDriveHelper = remember { GoogleDriveHelper(context) }

    LaunchedEffect(Unit) {
        viewModel.checkSingInState()
    }


    when (val state = singInState) {
        is SingInState.NotSignIn -> {
            GoogleSignInButton(
                "Sing in with Google", onClick = {
                    scope.launch {
                        viewModel.signInWithGoogle(context, clientId)
                    }
                })
        }

        is SingInState.Success -> {
            Column(Modifier.padding(4.dp)) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    AsyncImage(
                        model = state.user.photoUrl,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentDescription = state.user.name
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(state.user.name)
                        Text(state.user.email)
                    }

                    GoogleSignInButton(
                        "Sing out", onClick = {
                            scope.launch {
                                viewModel.signOut(context)
                            }
                        })


                }
                GDriveButton(
                    googleDriveHelper, isSignedIn,
                    onSingIn = { isSignedIn = true }, onSingOut = { isSignedIn = false })
                //GoogleDriveBrowser(googleDriveHelper, isSignedIn)
            }

        }

        is SingInState.Error -> {
            Text("Error: ${state.message}")
        }
    }


}