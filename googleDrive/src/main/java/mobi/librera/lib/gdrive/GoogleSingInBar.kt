package mobi.librera.lib.gdrive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch


@Composable
fun GoogleSignInScreen(
    clientId: String
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    val viewModel: GoogleSingInViewModel = viewModel()
    val singInState by viewModel.singInState.collectAsState()

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
            Row(
                verticalAlignment = Alignment.Top,
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
            }
            GoogleSignInButton(
                "Sing out", onClick = {
                    scope.launch {
                        viewModel.signOut(context)
                    }
                })
        }

        is SingInState.Error -> {
            Text("Error: ${state.message}")
        }
    }


}