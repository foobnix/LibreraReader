package org.spreadme.pdfgadgets.ui.streamview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPainter
import org.spreadme.pdfgadgets.model.PdfImageInfo

@Composable
fun StreamImageView(
    streamImageUIState: StreamImageUIState
) {
    Box(
        Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        streamImageUIState.pdfImageInfo?.bufferedImage?.let {
            Image(
                it.toPainter(),
                contentDescription = ""
            )
        }
    }
}