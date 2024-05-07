package org.spreadme.pdfgadgets.ui.frame

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme

@Composable
fun MainApplicationFrame(content: @Composable (BoxScope.() -> Unit)) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Divider(color = PDFGadgetsTheme.extraColors.border)
                        Box(Modifier.fillMaxSize()) {
                            content()
                        }
                    }
                }
            }
        }
    }
}