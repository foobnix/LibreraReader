package org.spreadme.pdfgadgets.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt


/**
 * Converts a [Color] to an RGBA formatted color `#RRGGBBAA` hex string;
 * e.g., `#FFFFFF1A` (a translucent white).
 */
fun Color.toRgbaHexString(): String {
    val r = Integer.toHexString((red * 255).roundToInt())
    val g = Integer.toHexString((green * 255).roundToInt())
    val b = Integer.toHexString((blue * 255).roundToInt())

    return buildString {
        append('#')
        append(r.padStart(2, '0'))
        append(g.padStart(2, '0'))
        append(b.padStart(2, '0'))

        if (alpha != 1.0f) {
            val a = Integer.toHexString((alpha * 255).roundToInt())
            append(a.padStart(2, '0'))
        }
    }
}