package mobi.librera.appcompose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobi.librera.appcompose.bookgrid.BookGridViewModel
import mobi.librera.appcompose.core.ifOr
import mobi.librera.appcompose.imageloader.MyAsyncImageView
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.toBookState

@Composable
fun BookGrid(
    dataModel: BookGridViewModel,
    books: List<Book>,
    state: LazyGridState,
    onStarClicked: (Book) -> Unit,
    onBookClicked: (Book) -> Unit,
) {
    //val books by dataModel.getAllBooks.collectAsState()

    //val state = listGridState

//    val state = rememberLazyGridState(
//        dataModel.initialFirstVisibleItemIndex,
//        dataModel.initialFirstVisibleItemScrollOffset
//    )

//    LaunchedEffect(state.firstVisibleItemIndex, dataModel.initialFirstVisibleItemScrollOffset) {
//        dataModel.initialFirstVisibleItemIndex = state.firstVisibleItemIndex
//        dataModel.initialFirstVisibleItemScrollOffset = state.firstVisibleItemScrollOffset
//    }

    LazyVerticalGrid(
        state = state, columns = GridCells.Adaptive(minSize = 110.dp)
    ) {

        item(span = { GridItemSpan(maxLineSpan) }) {
            SelectedBooksBar(
                dataModel, false,
                onOpenBook = { onBookClicked(Book(it)) },
                onHomeClick = { }
            )
        }

        items(books.size) {
            val book = books[it]

            val roundShapeRadius = 20.dp

            Card(
                onClick = {
                    onBookClicked(book)
                    //dataModel.currentBookPath = book.path
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(roundShapeRadius)

            ) {
                Column {
                    Box {
                        MyAsyncImageView(
                            bookState = book.toBookState(),
                            contentDescription = "",
                            modifier = Modifier
                                .height(150.dp)
                                .fillMaxWidth()
                                .background(Color.White),
                            contentScale = ContentScale.Crop,
                        )

                        IconButton(
                            onClick = {
                                onStarClicked(book)
                                //dataModel.updateStar(book, !book.isSelected)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(bottomEnd = roundShapeRadius))
                                .background(color = Color.Green.copy(alpha = 0.8f))

                        ) {
                            Icon(
                                imageVector = book.isSelected.ifOr(
                                    Icons.Default.Star, Icons.Default.StarBorder
                                ),
                                contentDescription = "Star",
                            )
                        }
                    }
                }

                val progress = book.progress

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth(progress)
                            .background(color = Color.Blue.copy(alpha = 0.3f))
                    )
                    Text(
                        book.path.substringAfterLast("/"),
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1,
                        style = TextStyle(fontSize = 14.sp)
                    )


                }

            }
        }
    }
}

