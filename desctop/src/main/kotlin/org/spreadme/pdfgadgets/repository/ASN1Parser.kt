package org.spreadme.pdfgadgets.repository

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.util.ASN1Dump
import org.spreadme.pdfgadgets.model.ASN1Node
import java.io.ByteArrayInputStream

class ASN1Parser {

    fun parse(byteArray: ByteArray): ASN1Node {
        val din = ASN1InputStream(ByteArrayInputStream(byteArray))
        val primitive = din.readObject()
        return ASN1Node(primitive)
    }
}