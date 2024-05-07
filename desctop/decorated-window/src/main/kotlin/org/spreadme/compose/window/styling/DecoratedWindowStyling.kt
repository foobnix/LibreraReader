package org.spreadme.compose.window.styling

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import org.spreadme.compose.window.DecoratedWindowState

@Immutable
class DecoratedWindowStyle(
    val colors: DecoratedWindowColors,
    val metrics: DecoratedWindowMetrics,
) {

    companion object
}

@Immutable
class DecoratedWindowColors(
    val border: Color,
    val borderInactive: Color,
) {

    @Composable
    fun borderFor(state: DecoratedWindowState): State<Color> =
        rememberUpdatedState(
            when {
                !state.isActive -> borderInactive
                else -> border
            },
        )

    companion object
}

@Immutable
class DecoratedWindowMetrics(val borderWidth: Dp) {

    companion object
}

val LocalDecoratedWindowStyle: ProvidableCompositionLocal<DecoratedWindowStyle> =
    staticCompositionLocalOf {
        error("No DecoratedWindowStyle provided. Have you forgotten the theme?")
    }
