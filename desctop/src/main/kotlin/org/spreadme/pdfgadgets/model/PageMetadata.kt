package org.spreadme.pdfgadgets.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.itextpdf.kernel.geom.Rectangle
import org.spreadme.pdfgadgets.repository.PdfRenderer

class PageMetadata(
    val index: Int,
    val pageSize: Rectangle,
    private val rotation: Int,
    var mediabox: Rectangle,
    var renderer: PdfRenderer,
    val signatures: List<Signature> = listOf(),
    var pixmapMetadata: PixmapMetadata? = null,
    var textBlocks: List<TextBlock> = listOf(),
    var enabled: MutableState<Boolean> = mutableStateOf(true),
) {

    suspend fun render(dpi: Float): PageRenderInfo {
        return renderer.render(this, this.rotation, dpi)
    }
}
