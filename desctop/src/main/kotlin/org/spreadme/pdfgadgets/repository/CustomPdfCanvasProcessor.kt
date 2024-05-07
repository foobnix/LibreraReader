package org.spreadme.pdfgadgets.repository

import com.itextpdf.io.source.PdfTokenizer
import com.itextpdf.io.source.RandomAccessFileOrArray
import com.itextpdf.io.source.RandomAccessSourceFactory
import com.itextpdf.kernel.pdf.PdfObject
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener
import com.itextpdf.kernel.pdf.canvas.parser.util.PdfCanvasParser

class CustomPdfCanvasProcessor : PdfCanvasProcessor(EmptyEventListener) {

    fun parsePdfStream(byteArray: ByteArray): List<PdfObject> {
        val tokens = arrayListOf<PdfObject>()
        val tokeniser = PdfTokenizer(RandomAccessFileOrArray(RandomAccessSourceFactory().createSource(byteArray)))
        val ps = PdfCanvasParser(tokeniser)
        val operands: List<PdfObject> = ArrayList()
        while (ps.parse(operands).size > 0) {
            tokens.addAll(operands)
        }
        return tokens
    }

}

object EmptyEventListener : IEventListener {

    override fun eventOccurred(data: IEventData, type: EventType) {

    }

    override fun getSupportedEvents(): MutableSet<EventType> = mutableSetOf()

}