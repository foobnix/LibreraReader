package org.spreadme.pdfgadgets.model

import com.itextpdf.signatures.PdfGadgetsPKCS7
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.tsp.TimeStampToken
import java.security.cert.X509Certificate
import java.util.*

data class SignatureResult(
    val verifySignature: Boolean,
    val verifyTimestamp: Boolean,
    val signName: String?,
    val signDate: Calendar?,
    val reason: String?,
    val location: String?,
    val signingCertificate: X509Certificate,
    val timeStampToken: TimeStampToken?
) {

    companion object {
        fun getSubject(identifier: ASN1ObjectIdentifier, certificate: X509Certificate): String {
            val certHolder = JcaX509CertificateHolder(certificate)
            val subject = certHolder.subject
            return IETFUtils.valueToString(subject.getRDNs(identifier)[0].first.value)
        }
    }

    constructor(pkcS7: PdfGadgetsPKCS7): this(
        verifySignature = pkcS7.verifySignatureIntegrityAndAuthenticity(),
        verifyTimestamp = pkcS7.verifyTimestampImprint(),
        signName = getSubject(BCStyle.CN, pkcS7.signingCertificate),
        signDate = pkcS7.signDate,
        reason = pkcS7.reason,
        location = pkcS7.location,
        signingCertificate = pkcS7.signingCertificate,
        timeStampToken = pkcS7.timeStampToken
    )
}