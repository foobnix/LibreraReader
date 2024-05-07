package org.spreadme.pdfgadgets.ui.sidepanel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme
import java.awt.Cursor

@Composable
fun SidePanel(
    sidePanelUIState: SidePanelUIState,
    reverseDirection: Boolean = false,
    content: @Composable BoxScope.(SidePanelUIState) -> Unit
) {

    val sideViewState = remember { sidePanelUIState }

    Column(
        modifier = Modifier.fillMaxHeight().width(sideViewState.expandedSize)
            .background(PDFGadgetsTheme.extraColors.sidePanelBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(sidePanelUIState)
            DraggableVerticalSplitter(
                sideViewState,
                reverseDirection
            )
        }
    }
}

@Composable
fun DraggableVerticalSplitter(
    sideViewState: SidePanelUIState,
    reverseDirection: Boolean = false
) {
    val contentAlignment = if (reverseDirection) {
        Alignment.CenterStart
    } else {
        Alignment.CenterEnd
    }
    Box(
        modifier = Modifier.fillMaxSize().zIndex(1f),
        contentAlignment = contentAlignment
    ) {
        SidePanelVerticalSplitter(
            sideViewState,
            reverseDirection,
            onResize = sideViewState::calculateExpandSize
        )
    }
}

@Composable
fun SidePanelVerticalSplitter(
    sideViewState: SidePanelUIState,
    reverseDirection: Boolean = false,
    onResize: (delta: Dp) -> Unit,
) {
    val density = LocalDensity.current
    Box(
        Modifier.width(8.dp).fillMaxHeight().run {
            if (sideViewState.isResizeEnable) {
                this.draggable(
                    state = rememberDraggableState {
                        with(density) {
                            onResize(it.toDp())
                        }
                    },
                    orientation = Orientation.Horizontal,
                    startDragImmediately = true,
                    onDragStarted = { sideViewState.isResizing = true },
                    onDragStopped = { sideViewState.isResizing = false },
                    reverseDirection = reverseDirection
                ).pointerHoverIcon(PointerIcon(Cursor(Cursor.W_RESIZE_CURSOR)))
            } else {
                this
            }
        }
    )
    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(PDFGadgetsTheme.extraColors.border))
}