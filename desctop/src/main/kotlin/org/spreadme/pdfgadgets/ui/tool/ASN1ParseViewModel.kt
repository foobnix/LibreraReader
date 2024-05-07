package org.spreadme.pdfgadgets.ui.tool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.spreadme.pdfgadgets.common.ViewModel
import org.spreadme.pdfgadgets.common.viewModelScope
import org.spreadme.pdfgadgets.model.ASN1Node
import org.spreadme.pdfgadgets.repository.ASN1Parser
import org.spreadme.pem.Pem
import java.util.*

class ASN1ParseViewModel(
    private val asN1Parser: ASN1Parser
) : ViewModel() {

    private val logger = KotlinLogging.logger {}

    var asn1Node by mutableStateOf<ASN1Node?>(null)
    var message by mutableStateOf("")

    fun decode(base64: String) {
        viewModelScope.launch {
            if (base64.isNotBlank()) {
                try {
                    val bytes = if(base64.startsWith(Pem.BEGIN, true)) {
                        Pem.read(base64.byteInputStream())
                    } else {
                        Base64.getDecoder().decode(base64.trim())
                    }
                    asn1Node = asN1Parser.parse(bytes)
                } catch (e: Exception) {
                    logger.error("解析ASN1数据失败", e)
                    e.message?.let {
                        message = it
                    }
                }
            }
        }
    }
}