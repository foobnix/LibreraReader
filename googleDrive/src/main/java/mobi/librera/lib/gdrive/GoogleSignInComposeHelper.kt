package mobi.librera.lib.gdrive

import androidx.compose.ui.platform.ComposeView

object GoogleSignInComposeHelper {

    @JvmStatic
    fun createSimpleGoogleSignInButton(
        composeView: ComposeView,
        buttonText: String,
        onSignInClick: Runnable
    ) {
        composeView.setContent {
            GoogleSignInButton(
                text = buttonText,
                onClick = {
                    onSignInClick.run()
                }
            )
        }
    }
}
