package org.spreadme.pdfgadgets.ui.common

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Immutable
public class CircularProgressStyle(
    public val frameTime: Duration = 125.milliseconds,
    public val color: Color,
) {

    public companion object
}
