package org.spreadme.pdfgadgets.repository

import com.itextpdf.kernel.pdf.PdfDocument
import org.spreadme.pdfgadgets.model.Signature

interface SignatureParser {

    fun parse(document: PdfDocument): List<Signature>
}