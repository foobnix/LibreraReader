package mobi.librera.mupdf.fz

import com.sun.jna.Pointer
import com.sun.jna.Structure

class fz_pixmap : Structure(), Structure.ByValue {
    @JvmField
    var storable: Pointer? = null

    // int x, y, w, h
    @JvmField
    var x: Int = 0
    @JvmField
    var y: Int = 0
    @JvmField
    var w: Int = 0
    @JvmField
    var h: Int = 0

    // unsigned char n, s, alpha, flags
    @JvmField
    var n: Byte = 0
    @JvmField
    var s: Byte = 0
    @JvmField
    var alpha: Byte = 0
    @JvmField
    var flags: Byte = 0

    // ptrdiff_t stride
    @JvmField
    var stride: Long = 0

    // fz_separations *seps
    @JvmField
    var seps: Pointer? = null

    // int xres, yres
    @JvmField
    var xres: Int = 0
    @JvmField
    var yres: Int = 0

    // fz_colorspace *colorspace
    @JvmField
    var colorspace: Pointer? = null

    // unsigned char *samples
    @JvmField
    var samples: Pointer? = null

    // fz_pixmap *underlying
    @JvmField
    var underlying: Pointer? = null

    override fun getFieldOrder(): List<String> {
        return listOf(
            "storable", "x", "y", "w", "h", "n", "s", "alpha", "flags",
            "stride", "seps", "xres", "yres", "colorspace", "samples", "underlying"
        )
    }

    public override fun useMemory(m: Pointer?) {
        super.useMemory(m)
    }
}