package mobi.librera5.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dshatz.pdfmp.compose.PdfView
import com.dshatz.pdfmp.compose.platformModifier.rememberScrollbarAdapter
import com.dshatz.pdfmp.compose.source.asyncPdfResource
import com.dshatz.pdfmp.compose.state.PdfLayoutInfo
import com.dshatz.pdfmp.compose.state.rememberPdfState
import com.dshatz.pdfmp.compose.state.zoomPercents
import com.dshatz.pdfmp.source.PdfSource
import kotlinx.coroutines.launch
import mobi.librera5.cover
import mobi.librera5.initPDF
import mobi.librera5.model.Book
import mobi.librera5.platform.decodeImage
import mobi.librera5.platform.rememberFolderPicker
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookLibraryScreen(viewModel: BookLibraryViewModel = viewModel { BookLibraryViewModel() }) {
    val uiState by viewModel.uiState.collectAsState()

    val folderPicker = rememberFolderPicker { folderPath ->
        folderPath?.let { viewModel.scanFolder(it) }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Book Library") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer))
    }, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = folderPicker,
            icon = { Text("📁") },
            text = { Text("Select Folder") })
    }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is BookLibraryUiState.Empty -> EmptyState()
                is BookLibraryUiState.Loading -> LoadingState()
               //is BookLibraryUiState.Success -> BookGrid(state.books)
                is BookLibraryUiState.Success -> FullDoc()
                is BookLibraryUiState.Error -> ErrorState(state.message)
            }
        }
    }
}

@Composable
 fun Scrollbar(info: PdfLayoutInfo, modifier: Modifier) {
    val adapter = info.rememberScrollbarAdapter()
    VerticalScrollbar(adapter, modifier = modifier, style = LocalScrollbarStyle.current.copy(
        unhoverColor = Color.DarkGray,
        hoverColor = Color.Black
                                                                                            ))
}

@Composable
private fun FullDoc() {
    val res by asyncPdfResource {
        initPDF()
        val path = ""

        File(path).readBytes()

    }

    res?.let {
        val pdf = rememberPdfState(it, pageSpacing = 100.dp)

        // This is how to observe current document state.
        val zoom by pdf.zoomPercents()
        val layoutInfo by pdf.layoutInfo()

        Box {
            PdfView(
                pdf,
                modifier = Modifier.fillMaxSize()
                   )

            val scope = rememberCoroutineScope()

            layoutInfo?.let { layoutInfo ->
                Scrollbar(layoutInfo,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                ElevatedCard(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                            ) {
                    val mostVisiblePageIdx by derivedStateOf {
                        layoutInfo.mostVisiblePage.value?.pageIdx ?: 0
                    }
                    Text("Page ${mostVisiblePageIdx + 1} / ${layoutInfo.totalPages.value} (Zoom $zoom%)", fontSize = 24.sp, modifier = Modifier.padding(5.dp))
                    Text("Vertical scroll ${layoutInfo.offsetY.roundToInt()}/${layoutInfo.documentHeight.value.roundToInt()}", fontSize = 20.sp)
                    Column {
                        OutlinedIconButton(
                            onClick = {
                                scope.launch {
                                    layoutInfo.animateScrollTo(layoutInfo.pageRange.value.first)
                                }
                            }
                                          ) {
                            Icon(Icons.Default.ArrowUpward, null)
                        }

                        OutlinedIconButton(
                            onClick = {
                                scope.launch {
                                    layoutInfo.animateScrollTo(layoutInfo.pageRange.value.last)
                                }
                            }
                                          ) {
                            Icon(Icons.Default.ArrowDownward, null)
                        }
                    }
                }
                Column(modifier = Modifier.padding(20.dp).align(Alignment.BottomEnd)) {
                    val zoom by layoutInfo.zoom
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                layoutInfo.animateSetZoom(zoom + 0.2f)
                            }
                        }
                                             ) {
                        Icon(Icons.Default.ZoomIn, null)
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                layoutInfo.animateSetZoom(zoom - 0.2f)
                            }
                        }
                                             ) {
                        Icon(Icons.Default.ZoomOut, null)
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                layoutInfo.animateSetZoom(1f)
                            }
                        }
                                             ) {
                        Icon(Icons.Outlined.Clear, null)
                    }
                }
            }
        }
    }
}
@Composable
fun asyncPdfResource(load: suspend () -> ByteArray): State<PdfSource.PdfBytes?> {
    return produceState<PdfSource.PdfBytes?>(null, load) {
        value = PdfSource.PdfBytes(load())
    }
}


@Composable
fun ShowPDF(){
    val res by asyncPdfResource {
        initPDF()
        val path = "/Users/ivanivanenko/Downloads/export/Slow.pdf"

        File(path).readBytes()

    }

    res?.let {
        val pdf = rememberPdfState(it)
        PdfView(
            pdf,
            modifier = Modifier.fillMaxSize()
               )
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "📚", style = MaterialTheme.typography.displayLarge)
            Text(text = "No books yet", style = MaterialTheme.typography.titleLarge)
            Text(text = "Select a folder to scan for books",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(text = "Scanning for books...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)) {
            Text(text = "⚠️", style = MaterialTheme.typography.displayLarge)
            Text(text = "Error", style = MaterialTheme.typography.titleLarge)
            Text(text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BookGrid(books: List<Book>) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(books, key = { it.path }) { book ->
            BookCard(book)
        }
    }
}

@Composable
private fun BookCard(book: Book) {
    Card(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {
            // Book cover image
            var image by remember { mutableStateOf<ImageBitmap>(ImageBitmap(1, 1)) }
            LaunchedEffect(Unit) {
                val bytes = cover(book.path)
                image = decodeImage(byteArray = bytes)

            }

            Box(
                modifier = Modifier
                    .weight(1f) // Takes up all remaining space above the text
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)) // Clean corners for the image
                    .background(Color.LightGray.copy(alpha = 0.2f)) // Placeholder color
               ) {
                Image(bitmap = image,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())

                Text(
                    text = book.path.substringAfterLast("."),

                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(60.dp)
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Blue.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(4.dp),

                    )
            }

            // Book title
            Text(text = book.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp))

        }
    }
}




