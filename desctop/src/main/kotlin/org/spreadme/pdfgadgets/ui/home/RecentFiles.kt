package org.spreadme.pdfgadgets.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import org.spreadme.pdfgadgets.model.FileMetadata
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme
import org.spreadme.common.SizeUnit
import org.spreadme.common.format

@Composable
fun RecentFiles(
    recentFileViewModel: RecentFileViewModel,
    onFileOpen: (FileMetadata) -> Unit
) {
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        TableHeader(Modifier.background(MaterialTheme.colors.background).padding(horizontal = 8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(recentFileViewModel.fileMetadatas) { item ->
                TableLine(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fileMetadata = item,
                    onFileOpen,
                    onDelete = recentFileViewModel::delete
                )
            }
        }
    }
}

@Composable
fun TableHeader(modifier: Modifier = Modifier) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(0.6f), contentAlignment = Alignment.CenterStart) {
            TableHeaderText("文件名称")
        }
        Box(Modifier.weight(0.2f), contentAlignment = Alignment.CenterStart) {
            TableHeaderText("文件大小")
        }
        Box(Modifier.weight(0.2f), contentAlignment = Alignment.CenterStart) {
            TableHeaderText("打开时间")
        }
    }
    Divider(color = PDFGadgetsTheme.extraColors.border)
}

@Composable
fun TableHeaderText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.overline,
        color = MaterialTheme.colors.onBackground
    )
}

@Composable
fun TableLine(
    modifier: Modifier = Modifier,
    fileMetadata: FileMetadata,
    onFileOpen: (FileMetadata) -> Unit,
    onDelete: (FileMetadata) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        Modifier.fillMaxWidth().height(48.dp).selectable(true) {
            onFileOpen(fileMetadata)
            focusManager.clearFocus()
        }.then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(0.6f), contentAlignment = Alignment.CenterStart) {
            TableLineText(fileMetadata.name)
        }
        Box(Modifier.weight(0.2f), contentAlignment = Alignment.CenterStart) {
            TableLineText(SizeUnit.convert(fileMetadata.length))
        }
        Box(Modifier.weight(0.1f), contentAlignment = Alignment.CenterStart) {
            TableLineText(fileMetadata.openTime.format())
        }
        Box(Modifier.weight(0.1f), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = "",
                tint = PDFGadgetsTheme.extraColors.iconDisable,
                modifier = Modifier.size(16.dp).onClick {
                    onDelete(fileMetadata)
                }
            )
        }
    }
    Divider(color = PDFGadgetsTheme.extraColors.border)
}

@Composable
fun TableLineText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onBackground
    )
}