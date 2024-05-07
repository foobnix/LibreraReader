package org.spreadme.compose.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import org.spreadme.compose.window.styling.DecoratedWindowStyle
import org.spreadme.compose.window.styling.LocalDecoratedWindowStyle
import org.spreadme.compose.window.styling.LocalTitleBarStyle
import org.spreadme.compose.window.styling.TitleBarStyle

val defaultTitleBarStyle: TitleBarStyle
    @Composable @ReadOnlyComposable
    get() = LocalTitleBarStyle.current

val defaultDecoratedWindowStyle: DecoratedWindowStyle
    @Composable @ReadOnlyComposable
    get() = LocalDecoratedWindowStyle.current
