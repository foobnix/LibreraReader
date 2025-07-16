package mobi.librera.mupdf.fz

import com.sun.jna.Pointer
import com.sun.jna.Structure
import mobi.librera.mupdf.fz.lib.MemoryStructure

/**
 *
 * typedef struct fz_outline
 * {
 * 	int refs;
 * 	char *title;
 * 	char *uri;
 * 	fz_location page;
 * 	float x, y;
 * 	struct fz_outline *next;
 * 	struct fz_outline *down;
 * 	int is_open;
 * } fz_outline;
 */

@Structure.FieldOrder("refs", "title", "uri", "page", "x", "y", "next", "down", "is_open")
class fz_outline : MemoryStructure(), Structure.ByReference {
    @JvmField
    var refs: Int = 0
    @JvmField
    var title: String = ""
    @JvmField
    var uri: String = ""
    @JvmField
    var page: fz_location? = null
    @JvmField
    var x: Float = 0f
    @JvmField
    var y: Float = 0f
    @JvmField
    var next: Pointer? = null
    @JvmField
    var down: Pointer? = null
    @JvmField
    var is_open: Int = 0


}