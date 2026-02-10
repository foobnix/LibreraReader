package mobi.librera5.platform

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFolderPicker(
    onFolderSelected: (String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    
    // Launcher for folder picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistable permission
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            // Convert URI to path (for simplicity, using a default path)
            // In production, you'd work with the URI directly using DocumentFile
            val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                Environment.getExternalStorageDirectory().absolutePath
            }
            onFolderSelected(path)
        } ?: onFolderSelected(null)
    }
    
    // Launcher for storage permission (Android 11+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                folderPickerLauncher.launch(null)
            } else {
                onFolderSelected(null)
            }
        }
    }
    
    return remember {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - Check for MANAGE_EXTERNAL_STORAGE permission
                if (Environment.isExternalStorageManager()) {
                    folderPickerLauncher.launch(null)
                } else {
                    // Request permission
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    permissionLauncher.launch(intent)
                }
            } else {
                // Android 10 and below
                folderPickerLauncher.launch(null)
            }
        }
    }
}


actual fun decodeImage(byteArray: ByteArray): ImageBitmap {
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size).asImageBitmap()
}