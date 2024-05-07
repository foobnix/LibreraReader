package org.spreadme.pdfgadgets.model

import com.itextpdf.kernel.pdf.PdfDictionary
import com.itextpdf.kernel.pdf.PdfDocument
import mu.KotlinLogging
import java.io.Closeable

class PdfMetadata(
    val fileMetadata: FileMetadata,
    val documentInfo: DocumentInfo,
    val numberOfPages: Int,
    val trailer: PdfDictionary,
    val structureRoot: StructureNode,
    var pages: List<PageMetadata> = listOf(),
    val document: PdfDocument,
    val outlines: Outlines = Outlines(document),
    var signatures: List<Signature> = listOf(),
) : Closeable {

    private val logger = KotlinLogging.logger { }

    override fun close() {
        logger.debug("document[{}] close", fileMetadata.name)
        document.close()
        pages.forEach { it.renderer.close() }
    }

}
