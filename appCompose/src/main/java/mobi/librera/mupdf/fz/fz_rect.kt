package mobi.librera.mupdf.fz

import com.sun.jna.Structure

@Structure.FieldOrder("x0", "y0", "x1", "y1")
class fz_rect : Structure(), Structure.ByValue {
    @JvmField
    var x0: Float = 0f
    @JvmField
    var y0: Float = 0f
    @JvmField
    var x1: Float = 0f
    @JvmField
    var y1: Float = 0f
}