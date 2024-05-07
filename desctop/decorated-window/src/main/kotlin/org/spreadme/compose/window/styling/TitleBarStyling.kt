package org.spreadme.compose.window.styling

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import org.spreadme.compose.window.DecoratedWindowState

@Stable
class TitleBarStyle(
    val colors: TitleBarColors,
    val metrics: TitleBarMetrics
) {

    companion object
}

@Immutable
class TitleBarColors(
    val background: Color,
    val inactiveBackground: Color,
    val content: Color,
    val border: Color,

    // The background color for newControlButtons(three circles in left top corner) in MacOS
    // fullscreen mode
    val fullscreenControlButtonsBackground: Color,
) {

    @Composable
    fun backgroundFor(state: DecoratedWindowState): State<Color> =
        rememberUpdatedState(
            when {
                !state.isActive -> inactiveBackground
                else -> background
            },
        )

    companion object
}

@Immutable
class TitleBarMetrics(
    val height: Dp,
    val gradientStartX: Dp,
    val gradientEndX: Dp,
) {

    companion object
}

val LocalTitleBarStyle: ProvidableCompositionLocal<TitleBarStyle> =
    staticCompositionLocalOf {
        error("No TitleBarStyle provided. Have you forgotten the theme?")
    }
