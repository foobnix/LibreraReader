package org.spreadme.pdfgadgets.repository

import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor
import com.itextpdf.kernel.pdf.canvas.parser.listener.RegexBasedLocationExtractionStrategy
import mu.KotlinLogging
import org.spreadme.pdfgadgets.model.PdfMetadata
import org.spreadme.pdfgadgets.model.Position

class DefaultPdfTextSearcher : PdfTextSearcher {

    private val logger = KotlinLogging.logger {}

    override fun search(pdfMetadata: PdfMetadata, keyword: String): List<Position> {
        val positions = arrayListOf<Position>()
        pdfMetadata.pages.forEach { page ->
            val strategy = RegexBasedLocationExtractionStrategy(keyword)
            val parser = PdfCanvasProcessor(strategy, HashMap())
            parser.processPageContent(pdfMetadata.document.getPage(page.index))
            val locations = strategy.resultantLocations
            if (locations.isNotEmpty()) {
                locations.forEach {
                    logger.debug(
                        "find ${it.text} on pageNum: ${page.index}, location: (${it.rectangle.x}, ${it.rectangle.y}), " +
                                "size: w = ${it.rectangle.width}, h = ${it.rectangle.height}"
                    )
                    positions.add(Position(index = page.index, pageSize = page.pageSize, rectangle = it.rectangle))
                }
            }
        }
        return positions
    }
}