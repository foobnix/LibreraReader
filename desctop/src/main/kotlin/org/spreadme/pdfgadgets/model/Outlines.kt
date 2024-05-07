package org.spreadme.pdfgadgets.model

import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfArray
import com.itextpdf.kernel.pdf.PdfDictionary
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfNumber
import com.itextpdf.kernel.pdf.PdfObject
import com.itextpdf.kernel.pdf.PdfOutline

class Outlines(
    val document: PdfDocument,
    val content: List<PdfOutline> = arrayListOf(),
    val destNames: Map<String, PdfObject> = mutableMapOf(),
) {

    fun getPosition(array: PdfArray, pages: List<PageMetadata>): Position{
        var pageIndex = -1
        val rectangle = Rectangle(0f,0f)
        for(index in 0 until  array.size()) {
            val pdfObject = array[index]
            if(pdfObject is PdfDictionary) {
                pageIndex = document.getPageNumber(pdfObject)
            }
            if(pdfObject is PdfNumber) {
                if(index == 2) {
                    rectangle.x = pdfObject.floatValue()
                }
                if (index ==3) {
                    rectangle.y = pdfObject.floatValue()

                }
            }
        }
        val page = pages[pageIndex - 1]
        return Position(index = pageIndex, pageSize = page.pageSize, rectangle = rectangle)
    }
}