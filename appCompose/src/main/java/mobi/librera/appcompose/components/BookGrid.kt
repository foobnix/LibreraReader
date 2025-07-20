package mobi.librera.appcompose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mobi.librera.appcompose.model.DataModel
import mobi.librera.appcompose.room.Book
import org.koin.androidx.compose.koinViewModel

@Composable
fun BookGrid(onBookOpen: (Book) -> Unit) {

    val dataModel: DataModel = koinViewModel()
    val books by dataModel.allBooks.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp)
    ) {
        items(books.size) {
            val book = books[it]

            val roundShapeRadius = 20.dp

            Card(
                onClick = { onBookOpen.invoke(book) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(roundShapeRadius)

            ) {
                Column {
                    Box {
                        AsyncImage(
                            model = book.path,

                            contentDescription = "",
                            modifier = Modifier
                                .height(180.dp)
                                .fillMaxWidth()
                                .background(Color.White)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,

                            )

                        IconButton(
                            onClick = {
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(bottomEnd = roundShapeRadius))
                                .background(color = Color.Green.copy(alpha = 0.8f))

                        ) {
                            Icon(
                                imageVector = Icons.Default.StarBorder,
                                contentDescription = "Star",
                            )
                        }
                    }

                    Text(
                        book.path.substringAfterLast("/"),
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1

                    )
                }
            }
        }
    }

}