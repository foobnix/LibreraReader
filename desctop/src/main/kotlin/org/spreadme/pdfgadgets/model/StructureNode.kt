package org.spreadme.pdfgadgets.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.itextpdf.kernel.pdf.*

class StructureNode(
    val pdfName: PdfName?,
    val pdfObject: PdfObject,
    val type: PdfObjectType,
    val level: Int = 1,
    val parent: StructureNode? = null,
    var childs: List<StructureNode> = listOf(),
    val expanded: MutableState<Boolean> = mutableStateOf(false)
) {

    constructor(trailer: PdfObject) : this(null, trailer, 0)

    constructor(pdfName: PdfName? = null, pdfObject: PdfObject, level: Int = 0, parent: StructureNode? = null) : this(
        pdfName,
        pdfObject,
        getPdfObjectType(pdfObject.type.toInt()),
        level,
        parent
    )

    fun hasChild(): Boolean = type.hasChild && pdfName != PdfName.Parent

    fun childs(): List<StructureNode> {
        if (childs.isNotEmpty()) {
            return childs
        }
        if (pdfObject is PdfDictionary && pdfName != PdfName.Parent) {
            childs = pdfObject.entrySet().map { StructureNode(it.key, it.value, level + 1, this) }.toList()
        } else if (pdfObject is PdfStream) {
            childs = pdfObject.entrySet().map { StructureNode(it.key, it.value, level + 1, this) }.toList()
        } else if (pdfObject is PdfArray) {
            childs = pdfObject.map { StructureNode(null, it, level + 1) }.toList()
        }
        return childs
    }

    fun isPdfStream() = pdfObject is PdfStream
    fun isSignatureContent() = this.parent?.pdfName == PdfName.V && this.pdfName == PdfName.Contents
    fun isParseable() = isPdfStream() || isSignatureContent()

    override fun toString(): String {
        val name = pdfName?.toString() ?: ""
        val value = type.toString(pdfObject)
        if (name.isBlank()) {
            return value
        }
        if (value.isBlank()) {
            return name
        }
        return "$name : $value"
    }
}