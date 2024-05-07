package org.spreadme.pdfgadgets.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.spreadme.common.choose

@Composable
fun TreeNodeIcon(
    hasChild: Boolean,
    expanded: MutableState<Boolean>
){
    if (hasChild) {
        Icon(
            expanded.value.choose(Icons.Default.ArrowDropDown, Icons.Default.ArrowRight),
            contentDescription = "",
            tint = MaterialTheme.colors.onBackground,
            modifier = Modifier.size(16.dp).clickable {
                expanded.value = !expanded.value
            }
        )
    } else {
        Box(modifier = Modifier.padding(start = 16.dp))
    }
}