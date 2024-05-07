package org.spreadme.pdfgadgets.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import org.spreadme.compose.window.styling.DecoratedWindowStyle
import org.spreadme.compose.window.styling.LocalDecoratedWindowStyle
import org.spreadme.compose.window.styling.LocalTitleBarStyle
import org.spreadme.compose.window.styling.TitleBarStyle
import org.spreadme.common.choose

internal val lightTheme = lightColors(
    primaryVariant = Color(0xFF444791),
    primary = Color(0xFF5B5FC7),
    secondary = Color(0xFF005fB7),
    background = Color(0xFFEBEBEB),
    surface = Color(0xFFF7F7F7),

    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF171717),
    onSurface = Color(0xFF262626),
)

internal val darkTheme = darkColors(
    primaryVariant = Color(0xFF2F2F4A),
    primary = Color(0xFF4F52B2),
    secondary = Color(0xFF604DFF),
    background = Color(0xFF242424),
    surface = Color(0xFF282828),

    onPrimary = Color(0xFFE5E5E5),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFFAFAFA),
)


internal val LocalIsDarkTheme = compositionLocalOf { false }
internal val LocalGlobalColors = compositionLocalOf { lightColors() }
internal val LocalExtraColors = compositionLocalOf { lightExtraColors() }

interface PDFGadgetsTheme {
    companion object {

        val globalColors: Colors
            @Composable
            @ReadOnlyComposable
            get() = LocalGlobalColors.current

        val extraColors: ExtraColors
            @Composable
            @ReadOnlyComposable
            get() = LocalExtraColors.current

        val isDark: Boolean
            @Composable
            @ReadOnlyComposable
            get() = LocalIsDarkTheme.current

    }
}

@Composable
fun PDFGadgetsTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val globalColors = isDark.choose(darkTheme, lightTheme)
    MaterialTheme(
        colors = globalColors,
        typography = Typography,
        shapes = Shapes,
    ) {
        val extraTheme = isDark.choose(darkExtraColors(), lightExtraColors())

        val currentWindowStyle = isDark.choose(
            DecoratedWindowStyle.dark(),
            DecoratedWindowStyle.light()
        )
        val currentTitleBarStyle = isDark.choose(
            TitleBarStyle.dark(),
            TitleBarStyle.light()
        )

        CompositionLocalProvider(
            LocalDecoratedWindowStyle provides currentWindowStyle,
            LocalTitleBarStyle provides currentTitleBarStyle,
            LocalExtraColors provides extraTheme,
            LocalIsDarkTheme provides isDark,
            LocalGlobalColors provides globalColors
        ) {
            content()
        }
    }
}