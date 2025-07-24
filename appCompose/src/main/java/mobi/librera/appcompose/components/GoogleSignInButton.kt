package mobi.librera.appcompose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobi.librera.appcompose.R
import mobi.librera.appcompose.ui.theme.LibreraTheme

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
) {
    Button(
        onClick = {},
        modifier = Modifier.padding(4.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 3.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.google_icon),
                modifier = Modifier
                    .size(30.dp)
                    .padding(4.dp),
                contentDescription = ""
            )
            Spacer(Modifier.width(10.dp))
            Text("Sing in with Google")
        }

    }
}

@Preview
@Composable
fun Preview() {
    LibreraTheme {
        GoogleSignInButton { }
    }
}

