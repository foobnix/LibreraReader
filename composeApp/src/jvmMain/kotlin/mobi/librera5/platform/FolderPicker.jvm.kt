package mobi.librera5.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap

import javax.swing.JFileChooser
import org.jetbrains.skia.Image
@Composable
actual fun rememberFolderPicker(
    onFolderSelected: (String?) -> Unit
): () -> Unit {
    return remember {
        {
            val fileChooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Select Folder"
            }
            
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedFolder = fileChooser.selectedFile
                onFolderSelected(selectedFolder?.absolutePath)
            } else {
                onFolderSelected(null)
            }
        }
    }
}


actual fun decodeImage(byteArray: ByteArray): ImageBitmap {
    if(byteArray.size<=1) return ImageBitmap(1,1)
    return Image.makeFromEncoded(byteArray).toComposeImageBitmap()
}