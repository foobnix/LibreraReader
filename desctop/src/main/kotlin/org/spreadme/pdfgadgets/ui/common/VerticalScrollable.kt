package org.spreadme.pdfgadgets.ui.common

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.spreadme.pdfgadgets.ui.sidepanel.SidePanelUIState

@Composable
fun VerticalScrollable(
    sidePanelUIState: SidePanelUIState,
    content: @Composable BoxScope.() -> Unit
) {

    Box(modifier = Modifier.fillMaxSize()) {
        val verticalScrollState = rememberScrollState(sidePanelUIState.verticalScroll)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            sidePanelUIState.verticalScroll = verticalScrollState.value
            content()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp),
            adapter = rememberScrollbarAdapter(verticalScrollState)
        )
    }

}