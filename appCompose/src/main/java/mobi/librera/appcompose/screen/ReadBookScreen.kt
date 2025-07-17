package mobi.librera.appcompose.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mobi.librera.mupdf.fz.lib.MupdfDocument
import mobi.librera.mupdf.fz.lib.openDocument

@Composable
fun ReadBookScreen(bookPath: String, onBookClose: () -> Unit) {

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    var doc by remember { mutableStateOf<MupdfDocument>(MupdfDocument.EmptyDoc) }
    
    LaunchedEffect(bookPath) {
        doc = openDocument(bookPath, ByteArray(0), 1200, 1000, 24)
    }




    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.Blue.copy(alpha = 0.8f)),
            verticalAlignment = Alignment.CenterVertically

        ) {
            Text(
                bookPath.substringAfterLast("/"),
                maxLines = 1,
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)

            )
            IconButton(
                onClick =
                    {
                        onBookClose.invoke()
                        coroutineScope.launch {
                            doc.close()
                        }

                    }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    tint = Color.White,
                    contentDescription = "Close",
                )
            }

        }


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            state = listState,
            userScrollEnabled = true,
        ) {
            items(doc.pageCount, key = { index -> index }) { number ->
                var image by remember(number) { mutableStateOf(ImageBitmap(1, 1)) }

                remember(number) {
                    coroutineScope.launch(Dispatchers.IO) {
                        image = doc.renderPage(number, 1200).asImageBitmap()
                    }
                }
                Image(
                    image,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp),
                    contentDescription = "Image $number"
                )

            }
        }
    }
}

@Preview
@Composable
fun ReadBookScreenPreview() {
    ReadBookScreen("Book Title", {})
}