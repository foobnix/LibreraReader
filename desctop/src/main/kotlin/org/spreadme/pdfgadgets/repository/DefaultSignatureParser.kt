package org.spreadme.pdfgadgets.repository

import com.itextpdf.forms.PdfAcroForm
import com.itextpdf.forms.fields.PdfFormField
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.source.RASInputStream
import com.itextpdf.io.source.RandomAccessSourceFactory
import com.itextpdf.kernel.pdf.PdfDate
import com.itextpdf.kernel.pdf.PdfDictionary
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.signatures.ContentsChecker
import com.itextpdf.signatures.PdfGadgetsPKCS7
import com.itextpdf.signatures.PdfSignature
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.spreadme.pdfgadgets.model.Signature
import org.spreadme.pdfgadgets.model.SignatureResult
import java.io.InputStream
import java.security.Security

class DefaultSignatureParser : SignatureParser {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override fun parse(document: PdfDocument): List<Signature> {
        val comparator = Comparator<Signature> { s1, s2 -> (s1.signedLength - s2.signedLength).toInt() }

        return PdfAcroForm.getAcroForm(document, false)
            ?.formFields
            ?.map { (fieldName, field) -> getSignatureInfo(fieldName, field, document) }
            ?.filterNotNull()
            ?.sortedWith(comparator)
            ?: arrayListOf()
    }

    private fun getSignatureInfo(fieldName: String, field: PdfFormField, document: PdfDocument): Signature? {
        val merged: PdfDictionary = field.pdfObject
        if (PdfName.Sig != merged.get(PdfName.FT) && PdfName.Btn != merged.get(PdfName.FT)) {
            return null
        }
        val v = merged.getAsDictionary(PdfName.V) ?: return null
        val contents = v.getAsString(PdfName.Contents)
        if (contents == null) {
            return null
        } else {
            contents.markAsUnencryptedObject()
        }
        val byteRange = v.getAsArray(PdfName.ByteRange) ?: return null
        val rangeSize = byteRange.size()
        if (rangeSize < 2) {
            return null
        }
        val signedLength = byteRange.getAsNumber(rangeSize - 1).intValue() + byteRange.getAsNumber(rangeSize - 2).intValue()
        try {
            val signature = PdfSignature(v)
            val content = PdfEncodings.convertToBytes(signature.contents.value, null)
            val pkcS7 = parsePdfPKCS7(signature, content, document)

            // checko signatureCoversWholeDocument
            val contentCheckRange = longArrayOf(0, signedLength.toLong())
            val bytesSource = RandomAccessSourceFactory().createRanged(document.reader.safeFile.createSourceView(), contentCheckRange)
            val contentsChecker = ContentsChecker(bytesSource)
            val signatureCoversWholeDocument = contentsChecker.checkWhetherSignatureCoversWholeDocument(field)

            return Signature(fieldName, signedLength.toLong(), content, SignatureResult(pkcS7), signatureCoversWholeDocument)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parsePdfPKCS7(signature: PdfSignature, content: ByteArray, document: PdfDocument): PdfGadgetsPKCS7 {
        val pkcs7 = if (PdfName.Adbe_x509_rsa_sha1.equals(signature.subFilter)) {
            var cert = signature.pdfObject.getAsString(PdfName.Cert)
            if (cert == null) {
                cert = signature.pdfObject.getAsArray(PdfName.Cert).getAsString(0)
            }
            PdfGadgetsPKCS7(
                content,
                cert.valueBytes,
                BouncyCastleProvider.PROVIDER_NAME
            )
        } else if (PdfName.Adbe_pkcs7_detached.equals(signature.subFilter)) {
            val p7 = PdfGadgetsPKCS7(
                content,
                signature.subFilter,
                BouncyCastleProvider.PROVIDER_NAME
            )
            p7.rsaData = null
            p7

        } else {
            PdfGadgetsPKCS7(
                content,
                signature.subFilter,
                BouncyCastleProvider.PROVIDER_NAME
            )
        }

        signature.byteRange.let { byteRange ->
            var rg: InputStream? = null
            try {
                val raf = document.reader.safeFile
                rg = RASInputStream(RandomAccessSourceFactory().createRanged(raf.createSourceView(), byteRange.toLongArray()))
                val buf = ByteArray(8092)
                do {
                    val rd: Int = rg.read(buf, 0, buf.size)
                    if (rd >= 0) {
                        pkcs7.update(buf, 0, rd)
                    }
                } while (rd > 0)

            } finally {
                rg?.close()
            }
        }

        signature.date?.let { date ->
            pkcs7.signDate = PdfDate.decode(date.toString())
        }
        signature.reason?.let { reason ->
            pkcs7.reason = reason
        }
        signature.location?.let { location ->
            pkcs7.location = location
        }
        pkcs7.signName = signature.name
        return pkcs7
    }
}