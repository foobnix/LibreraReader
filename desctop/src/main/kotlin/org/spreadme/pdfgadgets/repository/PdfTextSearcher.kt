package org.spreadme.pdfgadgets.repository

import org.spreadme.pdfgadgets.model.PdfMetadata
import org.spreadme.pdfgadgets.model.Position

interface PdfTextSearcher {

    fun search(pdfMetadata: PdfMetadata, keyword: String): List<Position>
}