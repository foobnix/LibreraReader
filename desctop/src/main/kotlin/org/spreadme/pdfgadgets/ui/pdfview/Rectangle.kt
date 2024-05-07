package org.spreadme.pdfgadgets.ui.pdfview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.itextpdf.kernel.geom.Rectangle

@Composable
fun Rectangle(
    modifier: Modifier = Modifier,
    rectangle: Rectangle,
    mediaBox: Rectangle,
    initColor: Color = MaterialTheme.colors.secondary.copy(alpha = 0f),
    selectable: MutableState<Boolean> = mutableStateOf(false),
    selectColor: Color = MaterialTheme.colors.secondary,
    scale: Float,
    content: @Composable BoxScope.() -> Unit = {}
) {

    var seletedState by remember { selectable }
    val backgoundColor = animateColorAsState(
        targetValue = if (seletedState) {
            selectColor.copy(0.6f)
        } else {
            initColor
        },
        animationSpec = repeatable(
            iterations = 3,
            animation = keyframes {
                durationMillis = 500
                selectColor.copy(0f) at 0
            },
            repeatMode = RepeatMode.Reverse
        ),
        finishedListener = {
            seletedState = false
        }
    )

    Box(
        modifier = Modifier
            .size((rectangle.width * scale).dp, (rectangle.height * scale).dp)
            .offset(
                x = (rectangle.x * scale).dp,
                y = ((mediaBox.height - (rectangle.y - mediaBox.y) - rectangle.height) * scale).dp
            ).background(backgoundColor.value).then(modifier)
    ) {
        content()
    }
}

@Composable
fun Rectangle(
    modifier: Modifier = Modifier,
    scale: Float,
    rectangle: Rectangle,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size((rectangle.width * scale).dp, (rectangle.height * scale).dp)
            .offset(
                x = (rectangle.x * scale).dp,
                y = (rectangle.y * scale).dp
            )
            .then(modifier)
    ){
        content()
    }
}