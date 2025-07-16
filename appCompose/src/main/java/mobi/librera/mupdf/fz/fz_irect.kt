package mobi.librera.mupdf.fz

import com.sun.jna.Structure

@Structure.FieldOrder("x0", "y0", "x1", "y1")
class fz_irect : Structure(), Structure.ByValue {
    @JvmField
    var x0: Int = 0
    @JvmField
    var y0: Int = 0
    @JvmField
    var x1: Int = 0
    @JvmField
    var y1: Int = 0

    fun make(x0: Int, y0: Int, x1: Int, y1: Int): fz_irect {
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
        return this
    }


    override fun toString(): String {
        return "fz_rect [x0: $x0, y0: $y0, x1: $x1, y1: $y1]"
    }
}