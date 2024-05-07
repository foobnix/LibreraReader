package org.spreadme.compose.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations.CustomTitleBar
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.spreadme.compose.window.styling.TitleBarStyle
import java.awt.Dialog
import java.awt.Frame
import java.awt.Window

@Composable
internal fun TitleBarOnWindows(
    modifier: Modifier = Modifier,
    gradientStartColor: Color = Color.Unspecified,
    style: TitleBarStyle = defaultTitleBarStyle,
    window: Window,
    state: DecoratedWindowState,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit,
) {
    val titleBar = remember { JBR.getWindowDecorations().createCustomTitleBar() }

    TitleBarImpl(
        modifier = modifier.customTitleBarMouseEventHandler(titleBar),
        gradientStartColor = gradientStartColor,
        style = style,
        window = window,
        state = state,
        applyTitleBar = { height, _ ->
            titleBar.height = height.value
            titleBar.putProperty("controls.dark", style.colors.background.isDark())
            if (window is Frame) {
                JBR.getWindowDecorations().setCustomTitleBar(window, titleBar)
            }
            if(window is Dialog) {
                JBR.getWindowDecorations().setCustomTitleBar(window, titleBar)
            }
            PaddingValues(start = titleBar.leftInset.dp, end = titleBar.rightInset.dp)
        },
        content = content,
    )
}

internal fun Modifier.customTitleBarMouseEventHandler(titleBar: CustomTitleBar): Modifier =
    pointerInput(Unit) {
        val currentContext = currentCoroutineContext()
        awaitPointerEventScope {
            var inUserControl = false
            while (currentContext.isActive) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                event.changes.forEach {
                    if (!it.isConsumed && !inUserControl) {
                        titleBar.forceHitTest(false)
                    } else {
                        if (event.type == PointerEventType.Press) {
                            inUserControl = true
                        }
                        if (event.type == PointerEventType.Release) {
                            inUserControl = false
                        }
                        titleBar.forceHitTest(true)
                    }
                }
            }
        }
    }

fun Color.isDark(): Boolean = (luminance() + 0.05) / 0.05 < 4.5