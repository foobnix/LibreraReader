package org.spreadme.pdfgadgets.model

import com.itextpdf.kernel.pdf.PdfDocumentInfo
import com.itextpdf.kernel.pdf.PdfName
import org.spreadme.pdfgadgets.utils.pdfDate
import java.util.*

data class DocumentInfo(
    val version: String,
    val title: String,
    val author: String,
    val subject: String,
    val keywords: String,
    val creator: String,
    val producer: String,
    val creationDate: Date?,
    val modDate: Date?
) {

    constructor(version: String, documentInfo: PdfDocumentInfo) : this(
        version = version,
        title = documentInfo.title ?: "",
        author = documentInfo.author ?: "",
        subject = documentInfo.subject ?: "",
        keywords = documentInfo.keywords ?: "",
        creator = documentInfo.creator ?: "",
        producer = documentInfo.producer ?: "",
        creationDate = (documentInfo.getMoreInfo(PdfName.CreationDate.value) ?: "").pdfDate(),
        modDate = (documentInfo.getMoreInfo(PdfName.ModDate.value) ?: "").pdfDate(),
    )
}




