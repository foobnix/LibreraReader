package org.spreadme.pdfgadgets.ui.pdfview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itextpdf.kernel.pdf.PdfArray
import com.itextpdf.kernel.pdf.PdfObject
import com.itextpdf.kernel.pdf.PdfOutline
import com.itextpdf.kernel.pdf.PdfString
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination
import com.itextpdf.kernel.pdf.navigation.PdfStringDestination
import org.spreadme.pdfgadgets.model.Outlines
import org.spreadme.pdfgadgets.model.PageMetadata
import org.spreadme.pdfgadgets.model.Position
import org.spreadme.pdfgadgets.resources.R
import org.spreadme.pdfgadgets.ui.common.VerticalScrollable
import org.spreadme.pdfgadgets.ui.sidepanel.SidePanelUIState

@Composable
fun OutlinesTree(
    sidePanelUIState: SidePanelUIState,
    outlines: Outlines,
    pageMetadata: List<PageMetadata>,
    onScroll: (postion: Position, scrollFinish: () -> Unit) -> Unit,
) {
    if (outlines.content.isNotEmpty()) {
        VerticalScrollable(
            sidePanelUIState
        ){
            Column(modifier = Modifier.fillMaxSize()) {
                bookmarksTreeNode(
                    outlines,
                    pageMetadata,
                    outlines.destNames,
                    outlines.content,
                    onScroll
                )
            }
        }

    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                useResource(R.Drawables.outlines_empty) {
                    loadSvgPainter(it, LocalDensity.current)
                },
                contentDescription = "",
                modifier = Modifier.padding(horizontal = 32.dp).widthIn(128.dp, 160.dp)
            )
        }
    }
}

@Composable
private fun bookmarksTreeNode(
    outlines: Outlines,
    pageMetadata: List<PageMetadata>,
    names: Map<String, PdfObject>,
    pdfOutlines: List<PdfOutline>,
    onScroll: (postion: Position, scrollFinish: () -> Unit) -> Unit,
    level: Int = 1
) {
    pdfOutlines.forEach {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .selectable(true) {
                    val destination = it.destination
                    if (destination is PdfStringDestination) {
                        val value = names[(destination.pdfObject as PdfString).toUnicodeString()]
                        if (value is PdfArray) {
                            val position = outlines.getPosition(value, pageMetadata)
                            onScroll(position){
                                position.selected.value = true
                            }
                        }
                    } else if (destination is PdfExplicitDestination && destination.pdfObject is PdfArray) {
                        val position = outlines.getPosition(destination.pdfObject as PdfArray, pageMetadata)
                        onScroll(position){
                            position.selected.value = true
                        }
                    }
                }
                .padding(start = (32 * level).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                it.title ?: "",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Start,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (it.allChildren != null && it.allChildren.isNotEmpty()) {
            bookmarksTreeNode(
                outlines,
                pageMetadata,
                names,
                it.allChildren,
                onScroll,
                level + 1
            )
        }
    }
}