package mobi.librera.lib.gdrive

import androidx.compose.ui.platform.ComposeView

object GoogleSignInComposeHelper {

    @JvmStatic
    fun createSimpleGoogleSignInButton(
        composeView: ComposeView, clientId: String
    ) {
        composeView.setContent {
            GoogleSignInScreen(
                clientId
            )
        }
    }
}
