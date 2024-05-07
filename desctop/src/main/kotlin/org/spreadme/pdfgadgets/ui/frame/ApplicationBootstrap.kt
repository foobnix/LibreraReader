package org.spreadme.pdfgadgets.ui.frame

import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.MutableStateFlow
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.spreadme.pdfgadgets.resources.R
import org.spreadme.pdfgadgets.ui.common.CircularProgressIndicator
import org.spreadme.pdfgadgets.ui.common.LoadText
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme

class ApplicationBootstrap(
    private val applicationViewModel: ApplicationViewModel
) : KoinComponent {

    private val logger = KotlinLogging.logger {}

    @Composable
    fun bootstrap(
        onFinished: @Composable () -> Unit
    ) {

        LaunchedEffect(Unit) {
            applicationViewModel.bootstrap()
        }

        if (!applicationViewModel.bootFinished) {
            logger.info("the application bootstrap!!!!")
            AppLoadProgressIndicator(applicationViewModel.bootMessage)
        }

        AnimatedVisibility(
            applicationViewModel.bootFinished,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            onFinished()
        }
    }

}

@Composable
fun AppLoadProgressIndicator(message: MutableStateFlow<String>) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize().zIndex(-1f)) {
            Image(
                painter = painterResource(R.Drawables.indicate),
                contentDescription = "",
                contentScale = ContentScale.Inside,
                modifier = Modifier.fillMaxSize()
            )
        }
        CircularProgressIndicator(Modifier.zIndex(1f))
        Column(
            Modifier.fillMaxSize().background(PDFGadgetsTheme.globalColors.background.copy(0.8f)),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.padding(bottom = 32.dp).height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val text by message.collectAsState()
                LoadText(
                    text,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onPrimary
                )
            }
        }

    }
}

@Composable
@Preview
fun AppLoadProgressIndicatorPreview() {
    AppLoadProgressIndicator(MutableStateFlow("dowload the mupdf lib from https://spreadme.oss-cn-shanghai.aliyuncs.com/mupdf/mupdf-macos-arm64.dylib"))
}
