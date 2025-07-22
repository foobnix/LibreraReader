package mobi.librera.appcompose.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mobi.librera.appcompose.components.SelectedBooksBar
import mobi.librera.appcompose.model.DataModel
import mobi.librera.appcompose.model.ReadBookModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReadBookScreen(dataModel: DataModel) {
    var hideShow by remember { mutableStateOf(true) }
    Box(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
            .fillMaxWidth()
    ) {
        ReadBookScreenInner(
            hideShow,
            bookPath = dataModel.currentBookPath,
            page = 0,
            onBookClose = {
                dataModel.currentBookPath = ""
            },
            onPageChanged = { page ->

            },
            onHideShow = { hideShow = !hideShow })

        if (hideShow) {
            SelectedBooksBar(dataModel, true)
        }

    }
}

@SuppressLint("WrongConstant")
@Composable
fun ReadBookScreenInner(
    hideShow: Boolean,
    bookPath: String,
    page: Int,
    onBookClose: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onHideShow: () -> Unit
) {

    val readModel: ReadBookModel = koinViewModel()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }) {

    }

    val documentState by readModel.documentState.collectAsState()

    LaunchedEffect(bookPath) {
        readModel.openDocument(bookPath, boxSize.width, boxSize.height, 50)
        listState.scrollToItem(0)
        sliderPosition = 0f
    }

//    LaunchedEffect(sliderPosition.toInt()) {
//        listState.scrollToItem(sliderPosition.toInt())
//    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            sliderPosition = listState.firstVisibleItemIndex.toFloat()
        }
    }

    when (val state = documentState) {
        is ReadBookModel.DocumentState.Loading -> {
            Text("Loading....")
        }

        is ReadBookModel.DocumentState.Error -> {
            Text("Loading Error....")
        }

        ReadBookModel.DocumentState.Idle -> {
            Text("Loading Idle....")
        }

        is ReadBookModel.DocumentState.Success -> {


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
                            modifier = Modifier.size(800.dp, 600.dp),
                            contentScale = ContentScale.FillWidth,
                            contentDescription = "Image $number"
                        )

                    }
                }
                if (hideShow) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
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
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
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
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                            )

                        )
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