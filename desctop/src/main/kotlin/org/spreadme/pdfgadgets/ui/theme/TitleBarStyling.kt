package org.spreadme.pdfgadgets.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.spreadme.compose.window.styling.TitleBarColors
import org.spreadme.compose.window.styling.TitleBarMetrics
import org.spreadme.compose.window.styling.TitleBarStyle

@Composable
fun TitleBarStyle.Companion.light(
    colors: TitleBarColors = TitleBarColors.light(),
    metrics: TitleBarMetrics = TitleBarMetrics.defaults()
): TitleBarStyle =
    TitleBarStyle(
        colors = colors,
        metrics = metrics
    )


@Composable
fun TitleBarStyle.Companion.dark(
    colors: TitleBarColors = TitleBarColors.dark(),
    metrics: TitleBarMetrics = TitleBarMetrics.defaults()
): TitleBarStyle =
    TitleBarStyle(
        colors = colors,
        metrics = metrics
    )


@Composable
fun TitleBarColors.Companion.light(
    backgroundColor: Color = Color(0xFF27282E),
    inactiveBackground: Color = Color(0xFF383A42),
    contentColor: Color = Color(0xFFEBECF0),
    borderColor: Color = Color(0xFF494B57),
    fullscreenControlButtonsBackground: Color = Color(0xFF7A7B80),
): TitleBarColors =
    TitleBarColors(
        background = backgroundColor,
        inactiveBackground = inactiveBackground,
        content = contentColor,
        border = borderColor,
        fullscreenControlButtonsBackground = fullscreenControlButtonsBackground,
    )

@Composable
fun TitleBarColors.Companion.dark(
    backgroundColor: Color = Color(0xFF2B2D30),
    inactiveBackground: Color = Color(0xFF393B40),
    fullscreenControlButtonsBackground: Color = Color(0xFF575A5C),
    contentColor: Color = Color(0xFFDFE1E5),
    borderColor: Color = Color(0xFF43454A),
): TitleBarColors =
    TitleBarColors(
        background = backgroundColor,
        inactiveBackground = inactiveBackground,
        content = contentColor,
        border = borderColor,
        fullscreenControlButtonsBackground = fullscreenControlButtonsBackground,
    )

fun TitleBarMetrics.Companion.defaults(
    height: Dp = 40.dp,
    gradientStartX: Dp = (-100).dp,
    gradientEndX: Dp = 400.dp,
): TitleBarMetrics = TitleBarMetrics(height, gradientStartX, gradientEndX)

