package org.spreadme.pdfgadgets.repository

import org.spreadme.pdfgadgets.model.PageMetadata
import org.spreadme.pdfgadgets.model.PageRenderInfo
import java.io.Closeable

interface PdfRenderer: Closeable {

    suspend fun render(page: PageMetadata, rotation: Int, dpi: Float): PageRenderInfo
}