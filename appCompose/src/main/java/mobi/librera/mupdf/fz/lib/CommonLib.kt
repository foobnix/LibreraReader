package mobi.librera.mupdf.fz.lib

import com.sun.jna.Native
import com.sun.jna.Pointer
import mobi.librera.mupdf.fz.fz_irect
import mobi.librera.mupdf.fz.fz_library
import mobi.librera.mupdf.fz.fz_matrix
import mobi.librera.mupdf.fz.fz_outline

val fz: fz_library = Native.load(
    "mupdf_java",
    fz_library::class.java
)

class CommonLib(tempFile: String, width: Int, height: Int, fontSize: Int) {
    private var fzContext: Pointer? = null
    private var fzDocument: Pointer? = null
    var fzPagesCount: Int = 0
    var fzTitle: String = "title"
    private var fzMupdfVersion: String = "1.25.4"

    init {
        println("Open document")
        fzContext = fz.fz_new_context_imp(null, null, 2560000, fzMupdfVersion)
        fz.fz_register_document_handlers(fzContext)

        fz.fz_set_user_css(fzContext, "body, div,p {margin:0em !important;}")
        fz.fz_set_use_document_css(fzContext, 1)


        //  val stream = fz.fz_open_memory(fzContext, document, document.size)
        // fzDocument = fz.fz_open_document_with_stream(fzContext, ".epub", stream)


        //var buffer = fz.fz_new_buffer_from_data(fzContext, document, document.size)
        //fzDocument = fz.fz_open_document_with_buffer(fzContext, "epub", buffer)

        fzDocument = fz.fz_open_document(fzContext, tempFile)

        fz.fz_layout_document(
            fzContext,
            fzDocument,
            width.toFloat(),
            height.toFloat(),
            fontSize.toFloat()
        )

        fzPagesCount = fz.fz_count_pages(fzContext, fzDocument)

        println("fzPagesCount $fzPagesCount")


    }

    fun getOutline(): List<Outline> {
        var fzOutline = fz.fz_load_outline(fzContext, fzDocument)
        val initOutline = fzOutline

        val result = mutableListOf<Outline>()

        var level = 0
        while (fzOutline != null) {
            val title = fzOutline.title
            val page = fzOutline.page?.page!!
            val uri = fzOutline.uri

            println("Outline-title: $title page: $page")

            result.add(Outline(title, page, uri, level))
            //fzOutline = fzOutline.next.toStructure(fz_outline::class.java)
            //fzOutline = fzOutline.next.toStructure1<fz_outline>()
            fzOutline = fzOutline.next.toStructure2 { fz_outline() }
        }

        //fz.fz_drop_outline(fzContext, initOutline) crash on desktop
        return result

    }


    private fun <T : MemoryStructure> Pointer?.toStructure(clazz: Class<T>): T? {
        if (this == null) return null
        val structure = clazz.getDeclaredConstructor().newInstance()
        structure.useMemory(this)
        structure.read()
        return structure
    }

    inline fun <reified T : MemoryStructure> Pointer?.toStructure1(): T? {
        if (this == null) return null
        val structure = T::class.java.getDeclaredConstructor().newInstance()
        structure.useMemory(this)
        structure.read()
        return structure
    }

    inline fun <reified T : MemoryStructure> Pointer?.toStructure2(creator: () -> T): T? {
        if (this == null) return null
        val structure = creator()
        structure.useMemory(this)
        structure.read()
        return structure
    }


    fun close() {
        fz.fz_drop_document(fzContext, fzDocument)
        fz.fz_drop_context(fzContext)
    }


    fun renderPage(page: Int, pageWidth: Int): Triple<IntArray, Int, Int> {

        val fzPage = fz.fz_load_page(fzContext, fzDocument, page)
        val fzBounds = fz.fz_bound_page(fzContext, fzPage);
        val fzColor: Pointer? = fz.fz_device_bgr(fzContext)


        println("log : pageWidth $pageWidth x1 ${fzBounds.x1} y1 ${fzBounds.y1}")

        val scale: Float = pageWidth / fzBounds.x1

        val pWidth: Int = pageWidth
        val pHeight: Int = (fzBounds.y1 * scale).toInt()



        println("log : scale $scale width ${pWidth} height $pHeight")
        val fzMatrix = fz_matrix().apply {
            a = scale
            d = scale
        }
        var bbox = fz_irect().apply {
            x0 = 0
            y0 = 0
            x1 = pWidth
            y1 = pHeight
        }


        val fzPixmap =
            fz.fz_new_pixmap_with_bbox(fzContext, fzColor, bbox, null, 1);


        fz.fz_clear_pixmap_with_value(fzContext, fzPixmap, 0xff)


        val fzDev = fz.fz_new_draw_device(fzContext, fzMatrix, fzPixmap)
        fz.fz_run_page(fzContext, fzPage, fzDev, fz_matrix(), null)
        // val width = fz.fz_pixmap_width(fzContext, fzPixmap)
        //val height = fz.fz_pixmap_height(fzContext, fzPixmap)
        //val size = fz.fz_pixmap_size(fzContext, fzPixmap)
        //val stride = fz.fz_pixmap_stride(fzContext, fzPixmap)

        val samples = fz.fz_pixmap_samples(fzContext, fzPixmap)

        val array = samples.getIntArray(0, pWidth * pHeight)



        fz.fz_drop_page(fzContext, fzPage)
        fz.fz_drop_pixmap(fzContext, fzPixmap)

        fz.fz_close_device(fzContext, fzDev)
        fz.fz_drop_device(fzContext, fzDev)

        return Triple(array, pWidth, pHeight)

    }


}

