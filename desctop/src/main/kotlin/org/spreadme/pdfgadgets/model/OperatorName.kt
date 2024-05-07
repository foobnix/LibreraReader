package org.spreadme.pdfgadgets.model

object OperatorName {
    // non stroking color
    const val NON_STROKING_COLOR = "sc"
    const val NON_STROKING_COLOR_N = "scn"
    const val NON_STROKING_RGB = "rg"
    const val NON_STROKING_GRAY = "g"
    const val NON_STROKING_CMYK = "k"
    const val NON_STROKING_COLORSPACE = "cs"

    // stroking color
    const val STROKING_COLOR = "SC"
    const val STROKING_COLOR_N = "SCN"
    const val STROKING_COLOR_RGB = "RG"
    const val STROKING_COLOR_GRAY = "G"
    const val STROKING_COLOR_CMYK = "K"
    const val STROKING_COLORSPACE = "CS"

    // marked content
    const val BEGIN_MARKED_CONTENT_SEQ = "BDC"
    const val BEGIN_MARKED_CONTENT = "BMC"
    const val END_MARKED_CONTENT = "EMC"
    const val MARKED_CONTENT_POINT_WITH_PROPS = "DP"
    const val MARKED_CONTENT_POINT = "MP"
    const val DRAW_OBJECT = "Do"

    // state
    const val CONCAT = "cm"
    const val RESTORE = "Q"
    const val SAVE = "q"
    const val SET_FLATNESS = "i"
    const val SET_GRAPHICS_STATE_PARAMS = "gs"
    const val SET_LINE_CAPSTYLE = "J"
    const val SET_LINE_DASHPATTERN = "d"
    const val SET_LINE_JOINSTYLE = "j"
    const val SET_LINE_MITERLIMIT = "M"
    const val SET_LINE_WIDTH = "w"
    const val SET_MATRIX = "Tm"
    const val SET_RENDERINGINTENT = "ri"

    // graphics
    const val APPEND_RECT = "re"
    const val BEGIN_INLINE_IMAGE = "BI"
    const val BEGIN_INLINE_IMAGE_DATA = "ID"
    const val END_INLINE_IMAGE = "EI"
    const val CLIP_EVEN_ODD = "W*"
    const val CLIP_NON_ZERO = "W"
    const val CLOSE_AND_STROKE = "s"
    const val CLOSE_FILL_EVEN_ODD_AND_STROKE = "b*"
    const val CLOSE_FILL_NON_ZERO_AND_STROKE = "b"
    const val CLOSE_PATH = "h"
    const val CURVE_TO = "c"
    const val CURVE_TO_REPLICATE_FINAL_POINT = "y"
    const val CURVE_TO_REPLICATE_INITIAL_POINT = "v"
    const val ENDPATH = "n"
    const val FILL_EVEN_ODD_AND_STROKE = "B*"
    const val FILL_EVEN_ODD = "f*"
    const val FILL_NON_ZERO_AND_STROKE = "B"
    const val FILL_NON_ZERO = "f"
    const val LEGACY_FILL_NON_ZERO = "F"
    const val LINE_TO = "l"
    const val MOVE_TO = "m"
    const val SHADING_FILL = "sh"
    const val STROKE_PATH = "S"

    // text
    const val BEGIN_TEXT = "BT"
    const val END_TEXT = "ET"
    const val MOVE_TEXT = "Td"
    const val MOVE_TEXT_SET_LEADING = "TD"
    const val NEXT_LINE = "T*"
    const val SET_CHAR_SPACING = "Tc"
    const val SET_FONT_AND_SIZE = "Tf"
    const val SET_TEXT_HORIZONTAL_SCALING = "Tz"
    const val SET_TEXT_LEADING = "TL"
    const val SET_TEXT_RENDERINGMODE = "Tr"
    const val SET_TEXT_RISE = "Ts"
    const val SET_WORD_SPACING = "Tw"
    const val SHOW_TEXT = "Tj"
    const val SHOW_TEXT_ADJUSTED = "TJ"
    const val SHOW_TEXT_LINE = "'"
    const val SHOW_TEXT_LINE_AND_SPACE = "\""

    // type3 font
    const val TYPE3_D0 = "d0"
    const val TYPE3_D1 = "d1"

    // compatibility section
    const val BEGIN_COMPATIBILITY_SECTION = "BX"
    const val END_COMPATIBILITY_SECTION = "EX"
}