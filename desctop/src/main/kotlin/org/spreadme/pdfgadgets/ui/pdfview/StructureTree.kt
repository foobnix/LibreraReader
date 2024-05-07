package org.spreadme.pdfgadgets.ui.pdfview

import androidx.compose.animation.*
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.spreadme.pdfgadgets.model.StructureNode
import org.spreadme.pdfgadgets.resources.R
import org.spreadme.pdfgadgets.ui.common.TreeNodeIcon
import org.spreadme.pdfgadgets.ui.common.clickable
import org.spreadme.pdfgadgets.ui.sidepanel.SidePanelUIState

@Composable
fun BoxScope.StructureTree(
    structureRoot: StructureNode,
    sidePanelUIState: SidePanelUIState,
    onClick: (StructureNode) -> Unit
) {
    val lazyListState = rememberLazyListState(
        sidePanelUIState.verticalScroll,
        sidePanelUIState.verticalScrollOffset
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState
    ) {
        sidePanelUIState.verticalScroll = lazyListState.firstVisibleItemIndex
        sidePanelUIState.verticalScrollOffset = lazyListState.firstVisibleItemScrollOffset
        items(structureRoot.childs()) { child ->
            StructureNodeView(child, onClick)
        }
    }

    VerticalScrollbar(
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        adapter = rememberScrollbarAdapter(lazyListState)
    )
}

@Composable
private fun StructureNodeView(
    node: StructureNode,
    onClick: (StructureNode) -> Unit
) {
    var expanded by remember { node.expanded }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .selectable(true) { expanded = !expanded }
            .padding(start = (24 * node.level).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        TreeNodeIcon(node.hasChild(), node.expanded)
        StructureNodeName(node, onClick)
    }

    AnimatedVisibility(
        expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            node.childs().forEach {
                StructureNodeView(it, onClick)
            }
        }
    }
}

@Composable
private fun StructureNodeName(
    node: StructureNode,
    onClick: (StructureNode) -> Unit
) {
    Icon(
        painter = painterResource(node.type.icon),
        contentDescription = "",
        tint = MaterialTheme.colors.onBackground,
        modifier = Modifier.padding(horizontal = 8.dp).size(16.dp)
    )
    Text(
        text = node.toString(),
        style = MaterialTheme.typography.caption,
        color = if (node.isParseable()) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.onBackground
        },
        textAlign = TextAlign.Start,
        textDecoration = if (node.isParseable()) {
            TextDecoration.Underline
        } else {
            null
        },
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier.run {
            if (node.isParseable()) {
                this.clickable(true) {
                    onClick(node)
                }
            } else {
                this
            }
        }
    )

    node.pdfObject.indirectReference?.let {
        Icon(
            painter = painterResource(R.Icons.pdfindirect_reference),
            contentDescription = "",
            tint = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(horizontal = 8.dp).size(16.dp)
        )
        Text(
            it.toString(),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onBackground,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}