package mobi.librera.appcompose.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mobi.librera.appcompose.components.SelectedBooksBar
import mobi.librera.appcompose.model.DataModel
import mobi.librera.mupdf.fz.lib.MupdfDocument
import mobi.librera.mupdf.fz.lib.openDocument

@Composable
fun ReadBookScreen(dataModel: DataModel) {

    Column(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
            .fillMaxWidth()
    ) {

        SelectedBooksBar(dataModel, true)

        ReadBookScreenInner(
            bookPath = dataModel.currentBookPath,
            page = 0,
            onBookClose = {
                dataModel.currentBookPath = ""
            },
            onPageChanged = { page ->

            })

    }
}

@SuppressLint("WrongConstant")
@Composable
fun ReadBookScreenInner(
    bookPath: String, page: Int, onBookClose: () -> Unit, onPageChanged: (Int) -> Unit
) {

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    var doc by remember(bookPath) { mutableStateOf<MupdfDocument>(MupdfDocument.EmptyDoc) }
    val context = LocalContext.current

    LaunchedEffect(bookPath) {
        doc = openDocument(bookPath, ByteArray(0), 1200, 1000, 44)

        listState.scrollToItem(page)

    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                onPageChanged(index)
            }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            state = listState,
            userScrollEnabled = true,
        ) {
            items(doc.pageCount, key = { index -> index }) { number ->
                var image by remember(bookPath + number) { mutableStateOf(ImageBitmap(1, 1)) }

                remember(bookPath + number) {
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
    //ReadBookScreen("Book Title", 0, onBookClose = {}, {})
}