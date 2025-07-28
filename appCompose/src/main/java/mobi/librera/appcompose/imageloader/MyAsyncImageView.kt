package mobi.librera.appcompose.imageloader

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
fun MyAsyncImageView(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null
) {
    var loadedImage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageUrl) {
        try {
            loadedImage = AppImageLoader.get().loadImage(imageUrl)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (loadedImage == null) {
        Image(
            bitmap = ImageBitmap(1, 1),
            contentDescription = contentDescription,
            modifier = modifier,
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