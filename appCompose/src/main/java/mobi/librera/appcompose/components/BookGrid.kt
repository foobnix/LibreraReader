package mobi.librera.appcompose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun BookGrid(foundFiles: List<String>, onBookOpen: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp)
    ) {
        items(foundFiles.size) {
            val bookPath = foundFiles[it]

            Card(
                onClick = { onBookOpen.invoke(bookPath) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column {
                    AsyncImage(
                        model = bookPath,

                        contentDescription = "",
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth()
                            .background(Color.White)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,

                        )

                    Text(
                        bookPath.substringAfterLast("/"),
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1

                    )
                }
            }
        }
    }

}