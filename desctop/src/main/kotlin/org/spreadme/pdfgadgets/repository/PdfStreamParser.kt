package org.spreadme.pdfgadgets.repository

import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spreadme.pdfgadgets.model.OperatorName
import org.spreadme.pdfgadgets.model.PdfImageInfo
import org.spreadme.pdfgadgets.model.PdfStreamTokenSequence
import org.spreadme.pdfgadgets.model.PdfStreamTokenType
import org.spreadme.common.applyMask
import org.spreadme.pdfgadgets.utils.getArray
import org.spreadme.pdfgadgets.utils.getBoolean
import java.io.ByteArrayInputStream

class PdfStreamParser : KoinComponent {

    private val customPdfCanvasProcessor by inject<CustomPdfCanvasProcessor>()

    suspend fun parse(pdfStream: PdfStream): List<PdfStreamTokenSequence> {
        return withContext(Dispatchers.IO) {
            contentStream(pdfStream)
        }
    }

    suspend fun parseXml(pdfStream: PdfStream): List<PdfStreamTokenSequence> {
        return withContext(Dispatchers.IO) {
            ByteArrayInputStream(pdfStream.getBytes(true)).bufferedReader().lines()
                .map {
                    PdfStreamTokenSequence().append(PdfStreamTokenType.string, it)
                }
                .toList()
        }
    }

    suspend fun parseImage(pdfStream: PdfStream): PdfImageInfo {
        return withContext(Dispatchers.IO) {
            val pdfImage = PdfImageXObject(pdfStream)

            val softMask = maskImage(pdfStream, PdfName.SMask)
            val mask = maskImage(pdfStream, PdfName.Mask)

            val bufferedImage = if (softMask != null) {
                applyMask(
                    pdfImage.bufferedImage, softMask.bufferedImage,
                    softMask.getBoolean(PdfName.Interpolate, false),
                    true,
                    softMask.getArray(PdfName("MATTE"))
                )
            } else if (mask != null && mask.getBoolean(PdfName.ImageMask, false)) {
                applyMask(
                    pdfImage.bufferedImage, mask.bufferedImage,
                    mask.getBoolean(PdfName.Interpolate, false),
                    true, null
                )
            } else {
                pdfImage.bufferedImage
            }
            PdfImageInfo(bufferedImage, imageType = pdfImage.identifyImageFileExtension())
        }
    }

    private fun contentStream(pdfStream: PdfStream): List<PdfStreamTokenSequence> {
        val streamTokenSequences = arrayListOf<PdfStreamTokenSequence>()
        val indentRule = IndentRule()
        if (pdfStream[PdfName.Length1] != null) {
            val bytes = pdfStream.getBytes(false)
            val tokenSequence = PdfStreamTokenSequence().append(PdfStreamTokenType.string, String(bytes))
            streamTokenSequences.add(tokenSequence)
        } else if (pdfStream[PdfName.Length1] == null) {
            val tokens = customPdfCanvasProcessor.parsePdfStream(pdfStream.bytes)
            var tokenSequence = PdfStreamTokenSequence()

            tokens.forEach {
                if (it is PdfLiteral) {
                    addOperators(it, tokenSequence, indentRule)
                } else {
                    addOperand(it, tokenSequence, indentRule)
                }

                if (tokenSequence.tokens.last().token.endsWith("\n")) {
                    streamTokenSequences.add(tokenSequence)
                    tokenSequence = PdfStreamTokenSequence()
                }
            }
        }
        return streamTokenSequences
    }

    private fun addOperators(literal: PdfLiteral, operators: PdfStreamTokenSequence, indentRule: IndentRule) {
        val operator = literal.toString()

        if (operator == OperatorName.END_TEXT || operator == OperatorName.RESTORE
            || operator == OperatorName.END_MARKED_CONTENT
        ) {
            indentRule.indent--
        }
        addIndent(operators, indentRule)

        operators.append(PdfStreamTokenType.operator, operator, "\n")
        // nested opening operators
        if (operator == OperatorName.BEGIN_TEXT ||
            operator == OperatorName.SAVE ||
            operator == OperatorName.BEGIN_MARKED_CONTENT ||
            operator == OperatorName.BEGIN_MARKED_CONTENT_SEQ
        ) {
            indentRule.indent++
        }
        indentRule.needIndent = true
    }

    private fun addOperand(pdfObject: PdfObject, operands: PdfStreamTokenSequence, indentRule: IndentRule) {
        addIndent(operands, indentRule)
        when (pdfObject) {
            is PdfName -> {
                operands.append(PdfStreamTokenType.tname, "$pdfObject ")
            }
            is PdfBoolean -> {
                operands.append(PdfStreamTokenType.string, "$pdfObject ")
            }
            is PdfArray -> {
                for (elem in pdfObject) {
                    addOperand(elem, operands, indentRule)
                }
            }
            is PdfString -> {
                val bytes = pdfObject.valueBytes
                for (b in bytes) {
                    val chr = b.toInt() and 0xff
                    if (chr == '('.code || chr == ')'.code || chr == '\\'.code) {
                        // PDF reserved characters must be escaped
                        val str = "\\" + chr.toChar()
                        operands.append(PdfStreamTokenType.escape, str)

                    } else if (chr < 0x20 || chr > 0x7e) {
                        // non-printable ASCII is shown as an octal escape
                        val str = String.format("\\%03o", chr)
                        operands.append(PdfStreamTokenType.escape, str)

                    } else {
                        val str = chr.toChar().toString()
                        operands.append(PdfStreamTokenType.escape, str)

                    }
                }
            }
            is PdfNumber -> {
                operands.append(PdfStreamTokenType.number, "$pdfObject ")
            }
            is PdfDictionary -> {
                pdfObject.entrySet().forEach {
                    addOperand(it.key, operands, indentRule)
                    addOperand(it.value, operands, indentRule)
                }
            }
            is PdfNull -> {
                operands.append(PdfStreamTokenType.string, "null ")
            }
            else -> {
                operands.append(PdfStreamTokenType.string, "$pdfObject ")
            }
        }
    }

    private fun addIndent(indent: PdfStreamTokenSequence, indentRule: IndentRule) {
        if (indentRule.needIndent) {
            for (i in 0 until indentRule.indent) {
                indent.append(PdfStreamTokenType.indent, "    ")
            }
            indentRule.needIndent = false
        }
    }

    private fun maskImage(pdfStream: PdfStream, pdfName: PdfName): PdfImageXObject? {
        val pdfObject = pdfStream[pdfName]
        if (pdfObject != null && pdfObject is PdfStream && pdfObject[PdfName.Subtype] == PdfName.Image) {
            return PdfImageXObject(pdfObject)
        }
        return null
    }
}

data class IndentRule(
    var indent: Int = 0,
    var needIndent: Boolean = false
)