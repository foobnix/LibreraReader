package org.spreadme.pdfgadgets.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color = MaterialTheme.colors.onPrimary,
    background: Color = MaterialTheme.colors.primary,
    onClick: () -> Unit
) {

    Box(
        modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
            .background(background)
            .selectable(true) {
                onClick()
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = "",
            tint = tint
        )
    }
}