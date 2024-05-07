package org.spreadme.pdfgadgets.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class Signature(
    val fieldName: String,
    val signedLength: Long,
    val content: ByteArray,
    val signatureResult: SignatureResult,
    val signatureCoversWholeDocument: Boolean,
    var position: Position? = null,
    val expand: MutableState<Boolean> = mutableStateOf(false)
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Signature

        if (fieldName != other.fieldName) return false

        return true
    }

    override fun hashCode(): Int {
        return fieldName.hashCode()
    }
}