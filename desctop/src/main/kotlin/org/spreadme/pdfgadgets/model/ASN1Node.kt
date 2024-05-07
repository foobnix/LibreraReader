package org.spreadme.pdfgadgets.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.bouncycastle.asn1.*

class ASN1Node(
    val primitive: ASN1Primitive,
    val type: ASN1NodeType,
    var level: Int = -1,
    var childs: List<ASN1Node> = listOf(),
    val expanded: MutableState<Boolean> = mutableStateOf(false)
) {

    constructor(primitive: ASN1Primitive) : this(primitive, 0)

    constructor(primitive: ASN1Primitive, level: Int) : this(
        primitive,
        getASN1NodeType(primitive),
        level
    )

    fun childs(): List<ASN1Node> {
        if (childs.isNotEmpty()) {
            return childs
        }
        if (primitive is ASN1Sequence) {
            childs = primitive.toArray().map { ASN1Node(it.toASN1Primitive(), level + 1) }.toList()
        } else if (primitive is ASN1Set) {
            childs = primitive.toArray().map { ASN1Node(it.toASN1Primitive(), level + 1) }.toList()
        } else if (primitive is ASN1TaggedObject) {
            childs = listOf(ASN1Node(primitive.`object`, level + 1))
        }
        return childs
    }

    override fun toString(): String {
        return type.toString(primitive)
    }
}