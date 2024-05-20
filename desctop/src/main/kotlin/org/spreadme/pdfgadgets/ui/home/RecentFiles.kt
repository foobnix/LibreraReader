package org.spreadme.pdfgadgets.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.DrawDevice
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.spreadme.common.SizeUnit
import org.spreadme.common.format
import org.spreadme.pdfgadgets.common.viewModelScope
import org.spreadme.pdfgadgets.model.FileMetadata
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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

fun toBufferedImage(width: Int, height: Int, pixels: IntArray): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB)
    image.setRGB(0, 0, width, height, pixels, 0, width)
    return image
}

suspend fun getOrCreateTempImage(item: FileMetadata): BufferedImage {
    val logger = KotlinLogging.logger { }

    val name = File(item.path).name

    val tmpdir = System.getProperty("java.io.tmpdir")
    var tmp = File(tmpdir, name)
    if (tmp.isFile && tmp.length() > 0) {
        logger.debug("get from cache $name")
        return ImageIO.read(tmp)
    }


    var image = BufferedImage(1, 1, 1)
    try {
        val pageImageRender: Document = Document.openDocument(item.path)
        val loadPage: Page = pageImageRender.loadPage(0)

        val scale = Matrix().scale(1.0f)
        val pixmap = loadPage.toPixmap(scale, ColorSpace.DeviceBGR, true, true)
        pixmap.clear(255)

        val drawDevice = DrawDevice(pixmap)
        loadPage.run(drawDevice, scale)

        image = toBufferedImage(pixmap.width, pixmap.height, pixmap.pixels)
        loadPage.destroy()
        pageImageRender.destroy()

        ImageIO.write(image, "bmp", tmp)

        logger.debug("write to cache $name $tmp")

    } catch (ex: RuntimeException) {
        ex.printStackTrace()
    }
    return image

}

@Composable
fun SearchFiles(
    recentFileViewModel: SearchFilesViewModel,
    onFileOpen: (FileMetadata) -> Unit
) {

    val verticalScrollState = rememberScrollState(0)

        //TableHeader(Modifier.background(MaterialTheme.colors.background).padding(horizontal = 8.dp))

        Box {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                Modifier.padding(8.dp)

            ) {


                items(recentFileViewModel.fileMetadatas.size) { i ->
                    val item = recentFileViewModel.fileMetadatas[i]
                    var image: Painter by remember {
                        mutableStateOf(
                            BufferedImage(
                                1,
                                1,
                                1
                            ).toPainter()
                        )
                    }

                    LaunchedEffect(item.path) {
                        image = getOrCreateTempImage(item).toPainter()
                    }

                    Card(
                        modifier = Modifier.onClick { onFileOpen(item) }.padding(10.dp)
                    ) {

                        Column {
                            Image(
                                painter = image,
                                contentDescription = "",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(200.dp).height(300.dp)
                            )

                            Text(
                                item.name,
                                modifier = Modifier
                                    .padding(16.dp),
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

//                TableLine(
//                    modifier = Modifier.padding(horizontal = 8.dp),
//                    fileMetadata = item,
//                    onFileOpen,
//                    onDelete = recentFileViewModel::delete
//                )
                }
            }
//            VerticalScrollbar(
//                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(20.dp).height(80.dp),
//                adapter = rememberScrollbarAdapter(verticalScrollState)
//            )
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