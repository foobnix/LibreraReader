package org.spreadme.pdfgadgets.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import java.awt.Cursor


@Composable
fun Modifier.clickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {

    return this.pointerHoverIcon(PointerIcon(Cursor(Cursor.DEFAULT_CURSOR))).clickable(
        enabled = enabled,
        onClick = onClick,
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    )
}

suspend fun AwaitPointerEventScope.awaitEventFirstDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (
        !event.changes.all { it.changedToDown() }
    )
    return event
}

fun Modifier.onBindKeyEvent(
    key: Key,
    onKeyDown: () -> Unit = {},
    onKeyUp: () -> Unit = {}
): Modifier {
    return this.onPreviewKeyEvent {
        if (it.key == key) {
            if (it.type == KeyEventType.KeyDown) {
                onKeyDown()
            }
            if (it.type == KeyEventType.KeyUp) {
                onKeyUp()
            }
            true
        } else {
            false
        }
    }
}