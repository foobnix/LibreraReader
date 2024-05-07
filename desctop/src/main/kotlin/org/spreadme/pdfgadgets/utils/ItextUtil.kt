package org.spreadme.pdfgadgets.utils

import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDate
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import java.util.*


/**
 * decode pdfdata
 */
fun String.pdfDate(): Date? {
    if (this.isBlank()) {
        return null
    }
    return PdfDate.decode(this).time
}

fun PdfImageXObject.getBoolean(pdfName: PdfName, defaultValue: Boolean) =
    this.pdfObject?.getAsBoolean(pdfName)?.value ?: defaultValue

fun PdfImageXObject.getArray(pdfName: PdfName): FloatArray? =
    this.pdfObject?.getAsArray(pdfName)?.toFloatArray()

fun Rectangle.rotate(rotation: Int, pageSize: Rectangle): Rectangle {
    if (rotation != 0) {
        return when ((rotation / 90) % 4) {
            1 -> Rectangle(pageSize.width - this.top, this.left, this.height, this.width)
            2 -> Rectangle(pageSize.width - this.right, pageSize.height - this.top, this.width, this.height)
            3 -> Rectangle(this.left, pageSize.height - this.right, this.height, this.width)
            else -> this

        }
    }
    return this
}