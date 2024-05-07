package org.spreadme.pdfgadgets.ui.streamview

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.spreadme.pdfgadgets.model.ASN1Node
import org.spreadme.pdfgadgets.ui.common.TreeNodeIcon
import org.spreadme.pdfgadgets.ui.common.VerticalScrollable

@Composable
fun StreamASN1View(
    streamASN1UIState: StreamASN1UIState
) {
    VerticalScrollable(streamASN1UIState.sidePanelUIState) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val childs = streamASN1UIState.root.childs()
            if (childs.isNotEmpty()) {
                childs.forEach {
                    ASN1NodeView(it)
                }
            } else {
                ASN1NodeView(streamASN1UIState.root)
            }
        }
    }
}

@Composable
fun ASN1NodeView(node: ASN1Node) {
    var expanded by remember { node.expanded }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .selectable(true) { expanded = !expanded }
            .padding(start = (16 * node.level).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        TreeNodeIcon(node.type.hasChild, node.expanded)
        ASN1NodeName(node)
    }

    AnimatedVisibility(
        expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            node.childs().forEach {
                ASN1NodeView(it)
            }
        }
    }

}


@Composable
fun ASN1NodeName(node: ASN1Node) {
    Text(
        text = node.toString(),
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onBackground,
        textAlign = TextAlign.Start,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}