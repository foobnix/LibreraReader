package org.spreadme.pdfgadgets.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

class ExtraColors(
    success: Color,
    onSuccess: Color,
    successBackground: Color,
    successBorder: Color,

    error: Color,
    onError: Color,
    errorBackground: Color,
    errorBorder: Color,

    warning: Color,
    onWarning: Color,
    warningBackground: Color,
    warningBorder: Color,

    iconDisable: Color,
    contentDisable: Color,
    border: Color,
    sidePanelBackground: Color,

    isLight: Boolean
) {
    var success by mutableStateOf(success, structuralEqualityPolicy())
        internal set
    var onSuccess by mutableStateOf(onSuccess, structuralEqualityPolicy())
        internal set
    var successBackground by mutableStateOf(successBackground, structuralEqualityPolicy())
        internal set
    var successBorder by mutableStateOf(successBorder, structuralEqualityPolicy())
        internal set

    var error by mutableStateOf(error, structuralEqualityPolicy())
        internal set
    var onError by mutableStateOf(onError, structuralEqualityPolicy())
        internal set
    var errorBackground by mutableStateOf(errorBackground, structuralEqualityPolicy())
        internal set
    var errorBorder by mutableStateOf(errorBorder, structuralEqualityPolicy())
        internal set

    var warning by mutableStateOf(warning, structuralEqualityPolicy())
        internal set
    var onWarning by mutableStateOf(onWarning, structuralEqualityPolicy())
        internal set
    var warningBackground by mutableStateOf(warningBackground, structuralEqualityPolicy())
        internal set
    var warningBorder by mutableStateOf(warningBorder, structuralEqualityPolicy())
        internal set

    var iconDisable by mutableStateOf(iconDisable, structuralEqualityPolicy())
        internal set
    var contentDisable by mutableStateOf(contentDisable, structuralEqualityPolicy())
        internal set
    var border by mutableStateOf(border, structuralEqualityPolicy())
        internal set
    var sidePanelBackground by mutableStateOf(sidePanelBackground, structuralEqualityPolicy())
        internal set

    var isLight by mutableStateOf(isLight, structuralEqualityPolicy())
        internal set
}

fun lightExtraColors(
    success: Color = Color(0xFF237B4B),
    onSuccess: Color = Color(0xFF237B4B),
    successBackground: Color = Color(0xFFE7F2DA),
    successBorder: Color = Color(0xFFBDDA9B),

    error: Color = Color(0xFFC4314B),
    onError: Color = Color(0xFFC4314B),
    errorBackground: Color = Color(0xFFFCF4F6),
    errorBorder: Color = Color(0xFFF3D6D8),

    warning: Color = Color(0xFF835C00),
    onWarning: Color = Color(0xFF835C00),
    warningBackground: Color = Color(0xFFFBF6D9),
    warningBorder: Color = Color(0xFFF2E384),

    iconDisable: Color = Color(0xFFA3A3A3),
    contentDisable: Color = Color(0xFFA3A3A3),
    border: Color = Color(0xc6c7c8),
    sidePanelBackground: Color = Color(0xFFF1F1F1)

): ExtraColors = ExtraColors(
    success,
    onSuccess,
    successBackground,
    successBorder,

    error,
    onError,
    errorBackground,
    errorBorder,

    warning,
    onWarning,
    warningBackground,
    warningBorder,

    iconDisable,
    contentDisable,
    border,
    sidePanelBackground,

    true
)

fun darkExtraColors(
    success: Color = Color(0xFF92C353),
    onSuccess: Color = Color(0xFF92C353),
    successBackground: Color = Color(0xFF0D2E0D),
    successBorder: Color = Color(0xFF032003),

    error: Color = Color(0xFFF9526B),
    onError: Color = Color(0xFFF9526B),
    errorBackground: Color = Color(0xFF3E1F25),
    errorBorder: Color = Color(0xFF1E040A),

    warning: Color = Color(0xFFF2E384),
    onWarning: Color = Color(0xFFF2E384),
    warningBackground: Color = Color(0xFF463100),
    warningBorder: Color = Color(0xFF261A00),

    iconDisable: Color = Color(0xFF404040),
    contentDisable: Color = Color(0xFF404040),
    border: Color = Color(0xFF1F1F1F),
    sidePanelBackground: Color = Color(0xFF27272A)

): ExtraColors = ExtraColors(
    success,
    onSuccess,
    successBackground,
    successBorder,

    error,
    onError,
    errorBackground,
    errorBorder,

    warning,
    onWarning,
    warningBackground,
    warningBorder,

    iconDisable,
    contentDisable,
    border,
    sidePanelBackground,

    true
)

class StreamKeywordColors(
    operator: Color,
    number: Color,
    string: Color,
    escape: Color,
    name: Color
) {
    var operator by mutableStateOf(operator, structuralEqualityPolicy())
        internal set
    var number by mutableStateOf(number, structuralEqualityPolicy())
        internal set
    var string by mutableStateOf(string, structuralEqualityPolicy())
        internal set
    var escape by mutableStateOf(escape, structuralEqualityPolicy())
        internal set
    var name by mutableStateOf(name, structuralEqualityPolicy())
        internal set
}

fun lightKeywordColor(
    operator: Color = Color(0xffCC7832),
    number: Color = Color(0xff6897BB),
    string: Color = Color(0xff8EA765),
    escape: Color = Color(0xffCC7832),
    name: Color = Color(140, 38, 145),
) = StreamKeywordColors(
    operator,
    number,
    string,
    escape,
    name
)

fun darkKeywordColor(
    operator: Color = Color(0xffCC7832),
    number: Color = Color(0xff6897BB),
    string: Color = Color(0xff8EA765),
    escape: Color = Color(0xffCC7832),
    name: Color = Color(0xffCE7832),
) = StreamKeywordColors(
    operator,
    number,
    string,
    escape,
    name
)

val LocalStreamKeywordColors = compositionLocalOf { lightKeywordColor() }