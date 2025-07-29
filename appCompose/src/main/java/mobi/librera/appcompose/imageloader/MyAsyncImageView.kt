package mobi.librera.appcompose.imageloader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.App
import mobi.librera.appcompose.room.BookState
import okhttp3.OkHttpClient
import okhttp3.Request

val client = OkHttpClient()

@Composable
fun MyAsyncImageView(
    bookState: BookState,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null
) {
    var loadedImage by remember { mutableStateOf<ImageBitmap?>(null) }

    assert(bookState.fileName.isNotBlank())

    val bookPath = bookState.bookPaths[App.DEVICE_ID].orEmpty()

    //println("MyAsyncImageView -> ${bookState.fileName} $bookPath")
    println("MyAsyncImageView -> ${bookState.imageUrl} ")

    LaunchedEffect(bookState.fileName) {
        withContext(Dispatchers.IO) {
            if (bookPath.isEmpty() && bookState.imageUrl.isNotEmpty()) {
                println("MyAsyncImageView ${bookState.fileName} ${bookState.imageUrl}")
                loadedImage = AppImageLoader.get().getCacheImageBitmap(bookState.fileName) ?: run {
                    val request = Request.Builder()
                        .url(bookState.imageUrl)
                        .build();
                    val response = client.newCall(request).execute()
                    val out = AppImageLoader.get().getCacheFile(bookState.fileName)
                    out.writeBytes(response.body.bytes())
                    AppImageLoader.get().getCacheImageBitmap(bookState.fileName)
                }
                println("MyAsyncImageView loaded}")
            } else {
                withContext(Dispatchers.Default) {
                    loadedImage = AppImageLoader.get().loadImage(bookPath)
                }
            }

        }
    }

    if (loadedImage == null) {
        Image(
            bitmap = ImageBitmap(1, 1),
            modifier = modifier.background(Color.Red),
            contentDescription = contentDescription,
            contentScale = contentScale
        )
    } else {
        Image(
            bitmap = loadedImage!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )

    }
}