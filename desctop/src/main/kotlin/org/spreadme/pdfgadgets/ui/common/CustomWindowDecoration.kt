package org.spreadme.pdfgadgets.ui.common

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

@Composable
fun CustomWindowDecoration(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    windowState: WindowState? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onDoubleClick = {
                windowState?.let {
                    if (windowState.placement == WindowPlacement.Floating) {
                        windowState.placement = WindowPlacement.Maximized
                    } else {
                        windowState.placement = WindowPlacement.Floating
                    }
                }
            },
            onClick = {
            }),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment
    ) {
        content()
    }
}