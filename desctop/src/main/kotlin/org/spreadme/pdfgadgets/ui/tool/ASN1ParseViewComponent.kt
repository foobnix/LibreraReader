package org.spreadme.pdfgadgets.ui.tool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.koin.core.component.inject
import org.spreadme.pdfgadgets.common.AppComponent
import org.spreadme.pdfgadgets.repository.ASN1Parser
import org.spreadme.pdfgadgets.ui.common.Toast
import org.spreadme.pdfgadgets.ui.common.ToastType
import org.spreadme.pdfgadgets.ui.common.VerticalScrollable
import org.spreadme.pdfgadgets.ui.frame.MainApplicationFrame
import org.spreadme.pdfgadgets.ui.sidepanel.SidePanelUIState
import org.spreadme.pdfgadgets.ui.streamview.ASN1NodeView

class ASN1ParseViewComponent : AppComponent("ASN1 Parser") {

    private val asn1Parser by inject<ASN1Parser>()

    private val asn1ParseViewModel = getViewModel<ASN1ParseViewModel>(asn1Parser)
    private val sidePanelUIState = SidePanelUIState()

    private var currentContent by mutableStateOf("")

    @Composable
    override fun onRender() {
        MainApplicationFrame {
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.padding(8.dp).fillMaxSize()) {
                    Column(Modifier.fillMaxHeight().weight(0.5f).padding(8.dp)) {
                        ASN1Base64Input(Modifier.fillMaxWidth().weight(1f)) {
                            asn1ParseViewModel.decode(it)
                        }
                    }
                    Column(
                        Modifier.fillMaxHeight().weight(0.5f).padding(8.dp)
                            .background(MaterialTheme.colors.background)
                    ) {
                        ASN1NodeTreeView()
                    }
                }

                if (asn1ParseViewModel.message.isNotBlank()) {
                    ErrorMessageToast()
                }
            }
        }
    }

    @Composable
    private fun ASN1Base64Input(
        modifier: Modifier = Modifier,
        onValueChange: (String) -> Unit
    ) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentContent,
                onValueChange = {
                    currentContent = it
                    onValueChange(it)
                },
                textStyle = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.onSurface
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @Composable
    private fun ASN1NodeTreeView() {
        asn1ParseViewModel.asn1Node?.let {
            VerticalScrollable(sidePanelUIState) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val childs = it.childs()
                    if (childs.isNotEmpty()) {
                        childs.forEach {
                            ASN1NodeView(it)
                        }
                    } else {
                        ASN1NodeView(it)
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorMessageToast() {
        Box(
            Modifier.fillMaxSize().zIndex(999f).padding(top = 56.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Toast(
                asn1ParseViewModel.message,
                ToastType.WARNING,
                timeout = 5_000,
                onFinished = {
                    asn1ParseViewModel.message = ""
                }
            )
        }
    }
}