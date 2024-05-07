package org.spreadme.pdfgadgets.model

import com.itextpdf.kernel.pdf.*
import org.bouncycastle.util.encoders.Hex
import org.spreadme.pdfgadgets.resources.R
import java.util.*

enum class PdfObjectType(
    val type: Int,
    val icon: String,
    val hasChild: Boolean) {

    BOOLEAN(2, R.Icons.pdfboolean, false) {
        override fun toString(pdfObject: PdfObject): String {
            pdfObject as PdfBoolean
            return pdfObject.toString()
        }
    },

    DICTIONARY(3, R.Icons.pdfdictionary, true) {
        override fun toString(pdfObject: PdfObject): String {
            val pdfDictionary = pdfObject as PdfDictionary
            val dictionaryType = pdfDictionary.get(PdfName.Type)
            return if (dictionaryType is PdfName) {
                dictionaryType.toString()
            } else {
                this.name.lowercase(Locale.getDefault())
            }
        }
    },

    NAME(6, R.Icons.pdfnanme, false) {
        override fun toString(pdfObject: PdfObject): String {
            pdfObject as PdfName
            return pdfObject.value
        }
    },

    NUMBER(8, R.Icons.pdfnumber, false) {
        override fun toString(pdfObject: PdfObject): String {
            pdfObject as PdfNumber
            return pdfObject.value.toString()
        }
    },

    STRING(10, R.Icons.pdfstring, false) {
        override fun toString(pdfObject: PdfObject): String {
            val pdfString = pdfObject as PdfString
            return if (pdfString.isHexWriting) {
                Hex.toHexString(pdfString.valueBytes)
            } else {
                pdfString.toUnicodeString()
            }
        }
    },

    STREAM(9, R.Icons.pdfstream, true),
    ARRAY(1, R.Icons.pdfarray, true),
    LITERAL(4, R.Icons.pdfliteral, false),
    NULL(7, R.Icons.pdfnull, false),
    INDIRECT_REFERENCE(5, R.Icons.pdfindirect_reference, true);

    open fun toString(pdfObject: PdfObject): String {
        return this.name.lowercase(Locale.getDefault())
    }
}

fun getPdfObjectType(type: Int) = when (type) {
    1 -> PdfObjectType.ARRAY
    2 -> PdfObjectType.BOOLEAN
    3 -> PdfObjectType.DICTIONARY
    4 -> PdfObjectType.LITERAL
    5 -> PdfObjectType.INDIRECT_REFERENCE
    6 -> PdfObjectType.NAME
    7 -> PdfObjectType.NULL
    8 -> PdfObjectType.NUMBER
    9 -> PdfObjectType.STREAM
    10 -> PdfObjectType.STRING
    else -> {
        PdfObjectType.NULL
    }
}