package org.spreadme.pdfgadgets.ui.common

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun Tipable(
    tooltip: @Composable () -> Unit,
    delay: Int = 600,
    content: @Composable () -> Unit,
) {

    TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(4.dp)
            ) {
                tooltip()
            }
        },
        // in milliseconds
        delayMillis = delay,
        tooltipPlacement = TooltipPlacement.CursorPoint(
            alignment = Alignment.BottomEnd,
            offset = DpOffset(8.dp, 8.dp)
        ),
        content = content
    )
}

@Composable
fun Tipable(tooltip: String, content: @Composable () -> Unit) = Tipable(
    tooltip = {
        Text(
            tooltip,
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(8.dp)
        )
    }
) {
    content()
}