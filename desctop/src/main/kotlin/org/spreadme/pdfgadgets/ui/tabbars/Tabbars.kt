package org.spreadme.pdfgadgets.ui.tabbars

import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.spreadme.pdfgadgets.common.AppComponent
import org.spreadme.pdfgadgets.ui.common.clickable

@Composable
fun Tabbars(
    components: List<AppComponent>,
    currentComponent: AppComponent?,
    modifier: Modifier = Modifier,
    tabWidthProvider: () -> Float,
    addIconSize: Int,
    onSelected: (AppComponent) -> Unit,
    onClose: (AppComponent) -> Unit,
    onAdd: () -> Unit
) {
    LazyRow(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(components) { index, item ->
            TabItem(
                tabWidthProvider,
                title = item.name,
                active = currentComponent == item,
                onSelected = {
                    onSelected(item)
                },
                onClose = {
                    onClose(item)
                }
            )
            // if item is last, show add icon
            if (index == components.size - 1) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(addIconSize.dp).padding(horizontal = 8.dp)
                        .clickable {
                            onAdd()
                        }
                )
            }
        }
    }
}

@Composable
fun TabItem(
    tabWidthProvider: () -> Float,
    title: String,
    active: Boolean,
    onSelected: () -> Unit,
    onClose: () -> Unit
) {

    val lineColor by rememberUpdatedState(
        when {
            active -> MaterialTheme.colors.primary
            else -> MaterialTheme.colors.background
        },
    )

    Row(
        Modifier.fillMaxHeight().width(tabWidthProvider().dp)
            .background(MaterialTheme.colors.background)
            .drawBehind {
                val strokeThickness = 4.0f
                val startY = size.height - (strokeThickness / 2f)
                val endX = size.width
                val capDxFix = strokeThickness / 2f

                drawLine(
                    brush = SolidColor(lineColor),
                    start = Offset(0 + capDxFix, startY),
                    end = Offset(endX - capDxFix, startY),
                    strokeWidth = strokeThickness,
                    cap = StrokeCap.Round,
                )
            }.clickable {
                onSelected()
            }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            Modifier.fillMaxSize().weight(0.8f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                title,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.caption,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (active) {
            Box(
                Modifier.fillMaxSize().weight(0.2f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp).clickable {
                        onClose()
                    }
                )
            }
        }
    }
}

@Composable
@Preview
fun TabItemPreview() {
    TabItem(
        tabWidthProvider = {
            168f
        },
        title = "测试Tab页",
        active = false,
        onSelected = {

        },
        onClose = {

        }
    )
}