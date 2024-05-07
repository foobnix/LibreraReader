package org.spreadme.pdfgadgets.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

val GoogleSans = FontFamily(
    Font("fonts/GoogleSans-Regular.ttf", FontWeight.Normal),
    Font("fonts/GoogleSans-Medium.ttf", FontWeight.Medium),
    Font("fonts/GoogleSans-Bold.ttf", FontWeight.Bold),
)

val FiraCode = FontFamily(
    Font("fonts/FiraCode-Regular.ttf", FontWeight.Normal),
)

// Material typography styles
val Typography = Typography(

    h4 = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp
    ),
    h5 = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp
    ),
    h6 = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp
    ),
    subtitle2 = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    body2 = TextStyle(
        fontFamily = GoogleSans,
        fontSize = 14.sp
    )
)