package org.spreadme.pdfgadgets.repository

import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spreadme.pdfgadgets.model.*

class DefaultPdfMetadataParser : PdfMetadataParser, KoinComponent {

    private val signatureParser by inject<SignatureParser>()

    override suspend fun parse(fileMetadata: FileMetadata): PdfMetadata {
        val readerProperties = ReaderProperties()
        fileMetadata.openProperties.password?.let {
            readerProperties.setPassword(it)
        }
        val document = PdfDocument(PdfReader(fileMetadata.file().absolutePath, readerProperties))

        val numberOfPages = document.numberOfPages
        val documentInfo = DocumentInfo(document.pdfVersion.toString(), document.documentInfo)

        // get outlines
        val catalog = document.catalog
        val pdfOutline = document.getOutlines(true)
        val destNameTree = catalog.getNameTree(PdfName.Dests)
        val outlines = Outlines(
            document,
            if (pdfOutline == null) {
                mutableListOf()
            } else {
                pdfOutline.allChildren
            },
            destNameTree.names
        )

        // pdf renderer
        val renderer = DefaultPdfRenderer(fileMetadata)

        // signatures
        val signatures = signatureParser.parse(document)
        val signatureMap = signatures.associateBy { it.fieldName }

        // pages info
        val pages = IntRange(1, numberOfPages).map {
            val originPage = document.getPage(it)
            val rotation = originPage.rotation
            val cropbox = cropbox(originPage)
            val mediabox = mediabox(originPage)
            val pageSize = cropbox?.let {
                if (cropbox.equalsWithEpsilon(mediabox)) mediabox else cropbox
            } ?: mediabox

            // get signatures from page
            val signaturesOfPage = originPage.annotations?.mapNotNull { a ->
                val merged: PdfDictionary = a.pdfObject
                val fieldName = merged.getAsString(PdfName.T)?.value ?: ""
                val signature = signatureMap[fieldName]
                if (signature != null) {
                    val rectangle = a.rectangle?.toRectangle()
                    if (rectangle != null) {
                        signature.position = Position(it, pageSize, rectangle)
                    }
                }
                signature
            }?.toList() ?: listOf()

            PageMetadata(it, pageSize, rotation, mediabox, renderer, signaturesOfPage)

        }.toList()

        return PdfMetadata(
            fileMetadata,
            documentInfo,
            numberOfPages,
            document.trailer,
            StructureNode(document.trailer),
            pages,
            document,
            outlines,
            signatures
        )
    }

    private fun cropbox(page: PdfPage): Rectangle? = page.cropBox

    private fun mediabox(page: PdfPage): Rectangle = page.mediaBox
}