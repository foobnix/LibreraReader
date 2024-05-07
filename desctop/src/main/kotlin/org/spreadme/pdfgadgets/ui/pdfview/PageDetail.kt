package org.spreadme.pdfgadgets.ui.pdfview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.spreadme.pdfgadgets.model.PageMetadata
import org.spreadme.pdfgadgets.model.Position
import org.spreadme.pdfgadgets.model.Signature
import org.spreadme.pdfgadgets.model.TextBlock
import org.spreadme.pdfgadgets.ui.common.awaitEventFirstDown
import org.spreadme.pdfgadgets.ui.common.clickable
import org.spreadme.pdfgadgets.ui.common.gesture.dragMotionEvent
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme
import java.awt.Cursor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.math.abs


@Composable
fun PageDetail(
    pageViewModel: PageViewModel,
    pdfViewModel: PdfViewModel
) {
    if (pageViewModel.enabled) {
        Box(modifier = Modifier.padding(start = 0.dp, 24.dp).fillMaxSize()) {
            // mediabox
            mediabox(pageViewModel.page, pdfViewModel.scale) {
                // page view
                AsyncPage(
                    pageViewModel,
                    pdfViewModel.viewType,
                    pdfViewModel.scale
                )
                // pdf signature
                signature(pageViewModel.page, pdfViewModel.scale)
                // searched text
                searchedText(
                    pageViewModel.page,
                    pageViewModel.searchPosition,
                    pdfViewModel.scale
                )
            }
        }
    }
}

@Composable
fun mediabox(
    page: PageMetadata,
    scale: Float,
    content: @Composable BoxScope.() -> Unit = {}
) {
    // mediabox size
    Rectangle(
        modifier = Modifier.background(MaterialTheme.colors.primary.copy(0.45f))
            .border(1.dp, color = PDFGadgetsTheme.extraColors.border),
        page.mediabox,
        page.mediabox,
        scale = scale,
        content = content
    )
}

@Composable
fun AsyncPage(
    pageViewModel: PageViewModel,
    viewType: PdfViewType,
    scale: Float
) {
    DisposableEffect(scale) {
        pageViewModel.onRender(scale)
        onDispose {
            pageViewModel.clearPage()
        }
    }

    pageViewModel.pageRenderInfo?.let { pageRenderInfo ->
        // page size
        Rectangle(
            modifier = Modifier.background(MaterialTheme.colors.secondary.copy(0.45f)),
            pageViewModel.page.pageSize,
            pageViewModel.page.mediabox,
            scale = scale
        ) {
            Image(
                pageRenderInfo.bufferedImage.toPainter(),
                contentDescription = "",
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize()
            )

            if (viewType == PdfViewType.TEXT_SELECT) {
                TextBlocks(
                    pageRenderInfo.textBlocks,
                    scale
                )
            } else if (viewType == PdfViewType.DRAW) {
                // draw area
                drawArea()
            }
        }
    }
}

@Composable
fun TextBlocks(
    textBlocks: List<TextBlock>,
    scale: Float
) {
    textBlocks.forEach { block ->
        TextBlock(block, scale)
    }
}

@Composable
fun TextBlock(
    textBlock: TextBlock,
    scale: Float
) {
    val rectangle = textBlock.position.rectangle
    var contextMenuEnabled by remember { mutableStateOf(false) }

    Rectangle(
        Modifier.border(1.dp, color = MaterialTheme.colors.secondary)
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.TEXT_CURSOR)))
            .pointerInput(textBlock) {
                forEachGesture {
                    awaitPointerEventScope {
                        val event = awaitEventFirstDown()
                        if (event.buttons.isSecondaryPressed) {
                            event.changes.forEach { it.consumeDownChange() }
                            contextMenuEnabled = true
                        }
                    }
                }
            },
        scale,
        rectangle
    ) {
        DropdownMenu(
            expanded = contextMenuEnabled,
            onDismissRequest = {
                contextMenuEnabled = false
            }
        ) {
            DropdownMenuItem(onClick = {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(textBlock.toString()), null)
                contextMenuEnabled = false
            }) {
                Text(String(textBlock.chars.toCharArray()), style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
fun signature(
    page: PageMetadata,
    scale: Float
) {
    val showSignature = remember { mutableStateOf(false) }
    val currentSignInfo = remember { mutableStateOf<Signature?>(null) }
    page.signatures.forEach {
        it.position?.let { position ->
            Rectangle(
                modifier = Modifier.zIndex(1f).clickable {
                    showSignature.value = true
                    currentSignInfo.value = it
                },
                position.rectangle,
                page.mediabox,
                selectable = position.selected,
                scale = scale
            ) {
                val stroke = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                val color = MaterialTheme.colors.secondary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(color = color, style = stroke)
                }
            }
        }
    }
    currentSignInfo.value?.let {
        SignatureDetail(it, showSignature)
    }
}

@Composable
fun searchedText(
    page: PageMetadata,
    positiones: List<Position>,
    scale: Float
) {
    val positionesState = remember { positiones }
    positionesState.forEach {
        Rectangle(
            modifier = Modifier.zIndex(2f),
            it.rectangle,
            page.mediabox,
            initColor = MaterialTheme.colors.primary.copy(0.2f),
            selectable = it.selected,
            selectColor = MaterialTheme.colors.secondary,
            scale = scale
        )
    }
}

@Composable
fun drawArea() {
    var offset by remember { mutableStateOf(Offset.Infinite) }
    var size by remember { mutableStateOf(Size.Zero) }
    val color = MaterialTheme.colors.secondary
    Canvas(
        modifier = Modifier.fillMaxSize()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.CROSSHAIR_CURSOR)))
            .dragMotionEvent(
                onDragStart = { pointerInputChange ->
                    size = Size.Zero
                    offset = pointerInputChange.position
                    pointerInputChange.consumeDownChange()

                },
                onDrag = { pointerInputChange ->
                    val position = pointerInputChange.position
                    size = Size(
                        abs(position.x - offset.x),
                        abs(position.y - offset.y)
                    )
                    pointerInputChange.consumePositionChange()

                },
                onDragEnd = { pointerInputChange ->
                    pointerInputChange.consumeDownChange()
                }
            )
    ) {
        if (!size.isEmpty()) {
            drawRect(
                color = color.copy(0.4f),
                topLeft = offset,
                size = size
            )
        }

    }
}