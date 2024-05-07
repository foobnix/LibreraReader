package org.spreadme.pdfgadgets.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun LoadText(
    message: String,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier
) {
    var enabled by remember { mutableStateOf(true) }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) {
            1f
        } else {
            0.2f
        },
        animationSpec = tween(200),
        finishedListener = {
            enabled = !enabled
        }
    )

    Text(
        text = message,
        color = color,
        style = style,
        modifier = modifier.alpha(animatedAlpha)
    )

    LaunchedEffect(Unit) {
        enabled = !enabled
    }
}