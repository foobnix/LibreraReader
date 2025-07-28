package mobi.librera.appcompose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import mobi.librera.appcompose.R
import mobi.librera.appcompose.bookgrid.BookGridViewModel


@Composable
fun SelectedBooksBar(
    dataModel: BookGridViewModel,
    isHomeVisible: Boolean,
    onHomeClick: () -> Unit,
    onOpenBook: (String) -> Unit,
) {

    val selectedBooks by dataModel.getAllSelectedBooks.collectAsState()
    val recentBooks by dataModel.getAllRecentBooks.collectAsState()
    val scrollState = rememberScrollState()
    val scrollState2 = rememberScrollState()

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

            if (isHomeVisible) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Blue.copy(alpha = 0.8f))
                        .clickable { onHomeClick() }) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }


            Row(modifier = Modifier.horizontalScroll(scrollState)) {


                selectedBooks.forEach { book ->

                    AsyncImage(
                        model =
                            ImageRequest.Builder(LocalContext.current)
                                .data(book.path)
                                .crossfade(true)
                                .diskCacheKey(book.path)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                        contentDescription = "",
                        modifier = Modifier
                            .height(60.dp)
                            .width(60.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                onOpenBook(book.path)
                            },
                        placeholder = painterResource(R.drawable.googleg_standard_color_18),

                        contentScale = ContentScale.Crop,
                    )
                    if (false) {
                        Row(
                            Modifier
                                .width(140.dp)
                                .height(42.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Blue.copy(alpha = 0.6f)),
                            verticalAlignment = Alignment.CenterVertically

                        ) {
                            AsyncImage(
                                model =
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(book.path)
                                        .crossfade(true)
                                        .memoryCacheKey(book.path)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCacheKey(book.path)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .build(),

                                contentDescription = "",
                                modifier = Modifier
                                    .height(42.dp)
                                    .width(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onOpenBook(book.path)
                                    },
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                book.path.substringAfterLast("/"),
                                maxLines = 1,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .weight(1f)
                                    .clickable {
                                        onOpenBook(book.path)

                                    },
                                style = TextStyle(
                                    color = Color.White, fontWeight = FontWeight.Bold
                                )

                            )
                            IconButton(
                                onClick = {
                                    dataModel.updateStar(book, !book.isSelected)

                                }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    tint = Color.White,
                                    contentDescription = "Close",
                                )
                            }

                        }
                    }

                }

            }


        }

        Row(modifier = Modifier.horizontalScroll(scrollState2)) {
            recentBooks.forEach { book ->
                AsyncImage(
                    model = book.path,
                    contentDescription = "",
                    modifier = Modifier
                        .height(60.dp)
                        .width(60.dp)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            onOpenBook(book.path)
                        },
                    contentScale = ContentScale.Crop,
                )
            }
        }

    }
}


