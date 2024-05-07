package org.spreadme.pdfgadgets.repository

import org.spreadme.pdfgadgets.model.FileMetadata
import org.spreadme.pdfgadgets.model.PdfMetadata

interface PdfMetadataParser {

    suspend fun parse(fileMetadata: FileMetadata): PdfMetadata
}