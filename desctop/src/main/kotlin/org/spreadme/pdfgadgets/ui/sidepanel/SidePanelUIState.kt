package org.spreadme.pdfgadgets.ui.sidepanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class SidePanelUIState {

    private val expandedMinSize: Dp = 300.dp
    var expandedSize by mutableStateOf(320.dp)
    var isResizing by mutableStateOf(false)
    var isResizeEnable by mutableStateOf(true)
    var verticalScroll by mutableStateOf(0)
    var verticalScrollOffset by mutableStateOf(0)

    fun calculateExpandSize(delta: Dp) {
        expandedSize = (expandedSize + delta).coerceAtLeast(expandedMinSize)
    }
}