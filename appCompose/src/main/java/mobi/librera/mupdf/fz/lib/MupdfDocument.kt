package mobi.librera.mupdf.fz.lib

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.sun.jna.Pointer
import com.sun.jna.Structure
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val mutex = Mutex()

suspend fun openDocument(
    name: String, document: ByteArray, width: Int, height: Int, fontSize: Int
): MupdfDocument {
    //val temp = MuFile.createTempFile(name, document)
    val temp = name
    val common = mutex.withLock {
        CommonLib(temp, width, height, fontSize)
    }
    println("openDocument $width $height $fontSize $name")
    return object : MupdfDocument() {
        override val pageCount = common.fzPagesCount
        override val title = common.fzTitle

        override suspend fun renderPage(page: Int, pageWidth: Int): Bitmap {
            if (pageCount == 0 || pageWidth == 0) {
                return ImageBitmap(1, 1).asAndroidBitmap()
            }

            println("Load page number  $page ")
            val (array, width1, height1) =
                mutex.withLock { common.renderPage(page, pageWidth) }

            val image = Bitmap.createBitmap(
                array, width1, height1, Bitmap.Config.ARGB_8888
            )

            return image

        }

        override suspend fun close() {
            mutex.withLock {
                common.close()
            }
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

class EmptyDoc2 : MupdfDocument() {
    override val pageCount: Int
        get() = TODO("Not yet implemented")
    override val title: String
        get() = TODO("Not yet implemented")

    override suspend fun renderPage(page: Int, pageWidth: Int): Bitmap {
        TODO("Not yet implemented")
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }

    override suspend fun getOutline(): List<Outline> {
        TODO("Not yet implemented")
    }
}


abstract class MupdfDocument {
    abstract val pageCount: Int
    abstract val title: String
    abstract suspend fun renderPage(page: Int, pageWidth: Int): Bitmap
    abstract suspend fun close()

    abstract suspend fun getOutline(): List<Outline>

    companion object EmptyDoc : MupdfDocument() {
        override val pageCount: Int = 0;
        override val title: String = "";
        override suspend fun renderPage(page: Int, pageWidth: Int): Bitmap =
            ImageBitmap(1, 1).asAndroidBitmap()

        override suspend fun close() {}
        override suspend fun getOutline(): List<Outline> = emptyList()

    }
}
