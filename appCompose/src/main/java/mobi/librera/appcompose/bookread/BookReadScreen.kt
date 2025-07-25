package mobi.librera.appcompose.bookread

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mobi.librera.appcompose.OnString
import mobi.librera.appcompose.OnVoid
import mobi.librera.appcompose.bookgrid.BookGridViewModel
import mobi.librera.appcompose.components.NumberPickerDialog
import mobi.librera.appcompose.components.SelectedBooksBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReadBookScreen(
    dataModel: BookGridViewModel,
    book: String,
    onBookClose: OnVoid,
    onOpenBook: OnString
) {
    var hideShow by remember { mutableStateOf(true) }

    val readModel: BookReadViewModel = koinViewModel()

    Box(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
            .fillMaxWidth()
    ) {
        ReadBookScreenInner(
            readModel,
            hideShow,
            bookPath = book,
            page = 0,
            onBookClose = onBookClose,
            onPageChanged = { page ->

            },
            onHideShow = { hideShow = !hideShow })

        if (hideShow) {
            SelectedBooksBar(
                dataModel, true,
                onHomeClick = onBookClose,
                onOpenBook = onOpenBook
            )
        }

    }
    BackHandler(
        onBack = onBookClose
    )

}

@SuppressLint("WrongConstant")
@Composable
fun ReadBookScreenInner(
    readModel: BookReadViewModel,
    hideShow: Boolean,
    bookPath: String,
    page: Int,
    onBookClose: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onHideShow: () -> Unit
) {


    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var showNumberPicker by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }) {

    }

    val documentState by readModel.documentState.collectAsState()

    val density = LocalDensity.current
    LaunchedEffect(bookPath, readModel.fontSize) {
        val pixels = with(density) { readModel.fontSize.sp.toPx().toInt() }
        readModel.openDocument(bookPath, boxSize.width, boxSize.height, pixels)
    }


//    LaunchedEffect(sliderPosition.toInt()) {
//        listState.scrollToItem(sliderPosition.toInt())
//    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            sliderPosition = listState.firstVisibleItemIndex.toFloat()
        }
    }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        readModel.progresss = listState.firstVisibleItemIndex.toFloat() / readModel.getPagesCount()

    }

    when (val state = documentState) {
        is BookReadViewModel.DocumentState.Loading -> {
            Text("Loading....")
        }

        is BookReadViewModel.DocumentState.Error -> {
            Text("Loading Error....")
        }

        BookReadViewModel.DocumentState.Idle -> {
            Text("Loading Idle....")
        }

        is BookReadViewModel.DocumentState.Success -> {
            LaunchedEffect(Unit) {
                val pageToOpen = readModel.getPagesCount() * readModel.progresss
                listState.scrollToItem(pageToOpen.toInt())
                sliderPosition = pageToOpen
            }

            LaunchedEffect(readModel.progresss) {
                readModel.updateProgress(bookPath)
            }


            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()

                                    if (event.type == PointerEventType.Press) {

                                        val position = event.changes.first().position
                                        if (position.x < boxSize.width / 4) {
                                            if (listState.firstVisibleItemIndex > 0) {
                                                coroutineScope.launch {
                                                    listState.scrollToItem(listState.firstVisibleItemIndex - 1)
                                                    sliderPosition =
                                                        listState.firstVisibleItemIndex.toFloat()
                                                }
                                            }
                                        } else if (position.x > boxSize.width - boxSize.width / 4) {
                                            coroutineScope.launch {
                                                listState.scrollToItem(listState.firstVisibleItemIndex + 1)
                                                sliderPosition =
                                                    listState.firstVisibleItemIndex.toFloat()
                                            }
                                        } else {
                                            onHideShow()
                                        }
                                    }
                                }
                            }
                        },
                    state = listState,
                    userScrollEnabled = true,
                ) {
                    items(readModel.getPagesCount(), key = { index -> index }) { number ->
                        var image by remember(bookPath + number) {
                            mutableStateOf(
                                ImageBitmap(
                                    1, 1
                                )
                            )
                        }

                        remember(bookPath + number) {
                            coroutineScope.launch(Dispatchers.IO) {
                                image = readModel.renderPage(number, boxSize.width)
                            }
                        }


                        Image(
                            image,
                            modifier = Modifier.fillMaxWidth(),

                            contentScale = ContentScale.FillWidth,
                            contentDescription = "Image $number"
                        )

                    }
                }
                if (hideShow) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        if (bookPath.endsWith(".epub")) {
                            Row(
                                modifier = Modifier
                                    .height(42.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .fillMaxWidth()
                                    .background(Color.Blue.copy(alpha = 0.8f)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {

                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clickable {
                                            readModel.fontSize--
                                        },
                                    contentDescription = "",
                                    tint = Color.White,
                                )

                                Text(
                                    "${readModel.fontSize}",

                                    modifier = Modifier.clickable {
                                        showNumberPicker = true
                                    }, style = TextStyle(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                )
                                NumberPickerDialog(
                                    showDialog = showNumberPicker,
                                    onDismissRequest = { showNumberPicker = false },
                                    onNumberSelected = {
                                        readModel.fontSize = it
                                        showNumberPicker = false
                                    },
                                    initialNumber = readModel.fontSize,
                                    range = 10..99
                                )


                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clickable {
                                            readModel.fontSize++
                                        },
                                    contentDescription = "",
                                    tint = Color.White,
                                )
                            }
                        }


                        Row(
                            modifier = Modifier
                                .height(42.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .fillMaxWidth()

                                .background(Color.Blue.copy(alpha = 0.8f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${sliderPosition.toInt() + 1}",
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                                style = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Slider(
                                value = sliderPosition,

                                onValueChange = { newPosition ->
                                    sliderPosition = newPosition
                                    coroutineScope.launch {
                                        listState.scrollToItem(newPosition.toInt())
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White,
                                ),
                                valueRange = 0f..(state.pageCount).toFloat(),
                                steps = 0,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${state.pageCount}",
                                modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                                style = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                            )
                        }
                    }

                }

            }

        }


    }


}

@Preview
@Composable
fun ReadBookScreenPreview() {
    //ReadBookScreen("Book Title", 0, onBookClose = {}, {})
}