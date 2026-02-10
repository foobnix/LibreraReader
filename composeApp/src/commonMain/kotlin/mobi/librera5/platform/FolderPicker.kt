package mobi.librera5.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun rememberFolderPicker(
    onFolderSelected: (String?) -> Unit
): () -> Unit


expect fun decodeImage(byteArray: ByteArray): ImageBitmap