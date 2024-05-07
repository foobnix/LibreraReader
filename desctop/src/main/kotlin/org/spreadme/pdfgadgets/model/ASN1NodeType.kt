package org.spreadme.pdfgadgets.model

import org.bouncycastle.asn1.*

enum class ASN1NodeType(
    val hasChild: Boolean
) {
    ASN1Sequence(true) {
        override fun toString(primitive: ASN1Primitive): String {
            val prefix = when (primitive) {
                is BERSequence -> {
                    "BER Sequence"
                }
                is DERSequence -> {
                    "DER Sequence"
                }
                else -> {
                    "Sequence"
                }
            }
            return "$prefix (${(primitive as org.bouncycastle.asn1.ASN1Sequence).size()})"
        }
    },
    ASN1TaggedObject(true) {
        override fun toString(primitive: ASN1Primitive): String {
            var tag = if (primitive is BERTaggedObject) {
                "BER Tagged"
            } else {
                "Tagged"
            }
            val taggedObject = primitive as org.bouncycastle.asn1.ASN1TaggedObject
            tag = "$tag [${taggedObject.tagNo}] "
            if (!taggedObject.isExplicit) {
                tag += "IMPLICIT"
            }
            return tag
        }
    },
    ASN1Set(true) {
        override fun toString(primitive: ASN1Primitive): String {
            val prefix = when (primitive) {
                is BERSet -> {
                    "BER Set"
                }
                is DERSet -> {
                    "DER Set"
                }
                else -> {
                    "Set"
                }
            }
            return "$prefix (${(primitive as org.bouncycastle.asn1.ASN1Set).size()})"
        }
    },
    ASN1OctetString(false),
    ASN1ObjectIdentifier(false),
    ASN1Boolean(false),
    ASN1Integer(false),
    DERBitString(false),
    DERIA5String(false),
    DERUTF8String(false),
    DERPrintableString(false),
    DERVisibleString(false),
    DERBMPString(false),
    DERT61String(false),
    DERGraphicString(false),
    DERVideotexString(false),
    ASN1UTCTime(false) {
        override fun toString(primitive: ASN1Primitive): String {
            if (primitive is org.bouncycastle.asn1.ASN1UTCTime) {
                return "UTCTime [${primitive.time}]"
            }
            return super.toString(primitive)
        }
    },
    ASN1GeneralizedTime(false) {
        override fun toString(primitive: ASN1Primitive): String {
            if(primitive is org.bouncycastle.asn1.ASN1GeneralizedTime){
                return "GeneralizedTime [${primitive.time}]"
            }
            return super.toString(primitive)
        }
    },
    BERApplicationSpecific(false),
    DERApplicationSpecific(false),
    DLApplicationSpecific(true),
    ASN1Enumerated(false),
    ASN1External(true),
    DERNull(false),
    UNKONW(false);

    open fun toString(primitive: ASN1Primitive): String {
        return "$name [$primitive]"
    }
}

fun getASN1NodeType(primitive: ASN1Primitive): ASN1NodeType =
    when (primitive) {
        is ASN1Sequence -> {
            ASN1NodeType.ASN1Sequence
        }
        is ASN1TaggedObject -> {
            ASN1NodeType.ASN1TaggedObject
        }
        is ASN1Set -> {
            ASN1NodeType.ASN1Set
        }
        is ASN1OctetString -> {
            ASN1NodeType.ASN1OctetString
        }
        is ASN1ObjectIdentifier -> {
            ASN1NodeType.ASN1ObjectIdentifier
        }
        is ASN1Boolean -> {
            ASN1NodeType.ASN1Boolean
        }
        is ASN1Integer -> {
            ASN1NodeType.ASN1Integer
        }
        is DERBitString -> {
            ASN1NodeType.DERBitString
        }
        is DERIA5String -> {
            ASN1NodeType.DERIA5String
        }
        is DERUTF8String -> {
            ASN1NodeType.DERUTF8String
        }
        is DERPrintableString -> {
            ASN1NodeType.DERPrintableString
        }
        is DERVisibleString -> {
            ASN1NodeType.DERVisibleString
        }
        is DERBMPString -> {
            ASN1NodeType.DERBMPString
        }
        is DERT61String -> {
            ASN1NodeType.DERT61String
        }
        is DERGraphicString -> {
            ASN1NodeType.DERGraphicString
        }
        is DERVideotexString -> {
            ASN1NodeType.DERVideotexString
        }
        is ASN1UTCTime -> {
            ASN1NodeType.ASN1UTCTime
        }
        is ASN1GeneralizedTime -> {
            ASN1NodeType.ASN1GeneralizedTime
        }
        is BERApplicationSpecific -> {
            ASN1NodeType.BERApplicationSpecific
        }
        is DERApplicationSpecific -> {
            ASN1NodeType.DERApplicationSpecific
        }
        is DLApplicationSpecific -> {
            ASN1NodeType.DLApplicationSpecific
        }
        is ASN1Enumerated -> {
            ASN1NodeType.ASN1Enumerated
        }
        is ASN1External -> {
            ASN1NodeType.ASN1External
        }
        is DERNull -> {
            ASN1NodeType.DERNull
        }
        else -> {
            ASN1NodeType.UNKONW
        }
    }
