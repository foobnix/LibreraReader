package mobi.librera5.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mobi.librera5.cover

import mobi.librera5.model.Book
import mobi.librera5.platform.decodeImage
import mobi.librera5.platform.rememberFolderPicker

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
                is BookLibraryUiState.Success -> BookGrid(state.books)
                is BookLibraryUiState.Error -> ErrorState(state.message)
            }
        }
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




