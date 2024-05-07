package org.spreadme.pdfgadgets.ui.streamview

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.spreadme.pdfgadgets.ui.common.CircularProgressIndicator
import org.spreadme.pdfgadgets.ui.common.Toast
import org.spreadme.pdfgadgets.ui.common.ToastType
import org.spreadme.pdfgadgets.ui.common.clickable
import org.spreadme.pdfgadgets.ui.theme.LocalStreamKeywordColors
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme
import org.spreadme.pdfgadgets.ui.theme.darkKeywordColor
import org.spreadme.pdfgadgets.ui.theme.lightKeywordColor
import org.spreadme.common.choose

@Composable
fun StreamPanel(
    streamPanelViewModel: StreamPanelViewModel,
    isDark: MutableState<Boolean>
) {
    Column(Modifier.fillMaxSize()) {
        Divider(color = PDFGadgetsTheme.extraColors.border, thickness = 1.dp)
        ActionToolbar {
            streamPanelViewModel.enabled = false
        }
        Divider(color = PDFGadgetsTheme.extraColors.border, thickness = 1.dp)

        LaunchedEffect(streamPanelViewModel.streamUIState) {
            streamPanelViewModel.parse()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (streamPanelViewModel.finished) {
                streamPanelViewModel.streamUIState?.let {
                    Column(Modifier.fillMaxSize()) {
                        when (it.streamPanelViewType) {
                            StreamPanelViewType.SIGCONTENT -> {
                                StreamASN1View(it as StreamASN1UIState)
                            }
                            StreamPanelViewType.IMAGE -> {
                                StreamImageView(it as StreamImageUIState)
                            }
                            else -> {
                                val keywordColor = isDark.value.choose(lightKeywordColor(), darkKeywordColor())
                                CompositionLocalProvider(
                                    LocalStreamKeywordColors provides keywordColor,
                                ) {
                                    StreamTextView(it as StreamTextUIState)
                                }

                            }
                        }
                    }
                }

                streamPanelViewModel.message?.let {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Toast(
                            it,
                            ToastType.ERROR,
                            -1L
                        )
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).zIndex(99f))

            }
        }
    }
}

@Composable
private fun ActionToolbar(
    onClose: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = "",
            tint = MaterialTheme.colors.onBackground,
            modifier = Modifier.size(16.dp).clickable {
                onClose()
            }
        )
    }
}
