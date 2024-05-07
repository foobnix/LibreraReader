package org.spreadme.pdfgadgets.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.spreadme.compose.window.DecoratedDialog
import org.spreadme.compose.window.TitleBar


@Composable
fun Dialog(
    onClose: () -> Unit,
    title: String,
    resizable: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit)
) {

    DecoratedDialog(
        onCloseRequest = onClose,
        visible = true,
        title = title,
        resizable = resizable
    ) {
        TitleBar {  }
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}