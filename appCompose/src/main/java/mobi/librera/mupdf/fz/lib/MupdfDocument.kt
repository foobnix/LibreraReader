package mobi.librera.mupdf.fz.lib

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.sun.jna.Pointer
import com.sun.jna.Structure


fun openDocument(
    name: String, document: ByteArray, width: Int, height: Int, fontSize: Int
): MupdfDocument {
    //val temp = MuFile.createTempFile(name, document)
    val temp = name
    val common = CommonLib(temp, width, height, fontSize)
    return object : MupdfDocument() {
        override val pageCount = common.fzPagesCount
        override val title = common.fzTitle

        override suspend fun renderPage(page: Int, pageWidth: Int): Bitmap {
            println("Load page number  $page ")
            val (array, width1, height1) = common.renderPage(page, pageWidth)

            val image = Bitmap.createBitmap(
                array, width1, height1, Bitmap.Config.ARGB_8888
            )

            return image

        }

        override fun close() {
            common.close()
        }

        override suspend fun getOutline(): List<Outline> = common.getOutline()
    }
}

open class MemoryStructure : Structure() {

    public override fun useMemory(m: Pointer?) {
        super.useMemory(m)
    }

}

data class Outline(
    val title: String,
    val page: Int,
    val url: String,
    val level: Int,
)


abstract class MupdfDocument {
    abstract val pageCount: Int
    abstract val title: String
    abstract suspend fun renderPage(page: Int, pageWidth: Int): Bitmap
    abstract fun close()

    abstract suspend fun getOutline(): List<Outline>

    companion object EmptyDoc : MupdfDocument() {
        override val pageCount: Int = 0;
        override val title: String = "";
        override suspend fun renderPage(page: Int, pageWidth: Int): Bitmap =
            ImageBitmap(1, 1).asAndroidBitmap()

        override fun close() {}
        override suspend fun getOutline(): List<Outline> = emptyList()

    }
}
