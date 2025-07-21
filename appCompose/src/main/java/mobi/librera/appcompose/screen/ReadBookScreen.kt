package mobi.librera.appcompose.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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

    Box(
        modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
            .fillMaxWidth()
    ) {
        ReadBookScreenInner(bookPath = dataModel.currentBookPath, page = 0, onBookClose = {
            dataModel.currentBookPath = ""
        }, onPageChanged = { page ->

        })

        SelectedBooksBar(dataModel, true)

    }
}

@SuppressLint("WrongConstant")
@Composable
fun ReadBookScreenInner(
    bookPath: String, page: Int, onBookClose: () -> Unit, onPageChanged: (Int) -> Unit
) {
    val readModel: ReadBookModel = koinViewModel()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var sliderPosition by remember { mutableFloatStateOf(0f) }


    val documentState by readModel.documentState.collectAsState()

    LaunchedEffect(bookPath) {
        readModel.openDocument(bookPath, 0, 0, 0)
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

            LaunchedEffect(sliderPosition) {
                listState.scrollToItem(sliderPosition.toInt())
            }


            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
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
                                image = readModel.renderPage(number, 1200)
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
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp
                        )
                    )
                    Slider(
                        value = sliderPosition,

                        onValueChange = { newValue ->
                            sliderPosition = newValue
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White,
                        ),
                        valueRange = 1f..state.pageCount.toFloat(),
                        steps = 0,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${state.pageCount}",
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        style = TextStyle(
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp
                        )

                    )
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