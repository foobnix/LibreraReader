package mobi.librera.appcompose.booksync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import mobi.librera.appcompose.components.GoogleSignInButton
import org.koin.androidx.compose.koinViewModel


@Composable
fun GoogleSignInScreen(
) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    val googleModel: GoogleSingInViewModel = koinViewModel()

    val userMail by googleModel.userEmail.collectAsState()

    Column {
        if (googleModel.singInErrorMsg.isNotEmpty()) {
            Text(googleModel.singInErrorMsg)
        }

        if (userMail.isNullOrEmpty()) {
            GoogleSignInButton(
                "Sing in with Google", onClick = {
                    scope.launch {
                        googleModel.signInWithGoogle(context)
                    }
                })
        } else {

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = googleModel.photoUrl,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentDescription = googleModel.userName
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(googleModel.userName)
                    Text(userMail.orEmpty())
                }
            }
            GoogleSignInButton(
                "Sing out", onClick = {
                    scope.launch {
                        googleModel.signOut(context)
                    }
                })


        }
    }


}