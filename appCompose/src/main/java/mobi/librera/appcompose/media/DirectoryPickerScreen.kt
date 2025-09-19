package mobi.librera.appcompose.media

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryPickerScreen() {
    // This state will hold the URI of the selected directory
    var directoryUri by remember { mutableStateOf<Uri?>(null) }
    // This state will hold the list of file names in the selected directory
    var fileList by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current

    // Set up the activity result launcher for opening a document tree
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            // The result is the URI of the picked directory
            directoryUri = uri
            uri?.let {
                // **Crucial Step**: Take persistent permission to access the directory
                val contentResolver = context.contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            }
        }
    )

    // When the directoryUri state changes, this effect will re-run
    LaunchedEffect(directoryUri) {
        fileList = emptyList() // Clear previous list
        directoryUri?.let { uri ->
            // Use DocumentFile to easily list files
            val directory = DocumentFile.fromTreeUri(context, uri)

            directory?.listFiles()?.let { files ->
                // Update the file list state, triggering a recomposition
                fileList = files.mapNotNull { it.name }
            }
        }
    }


    Scaffold(
        topBar = { TopAppBar(title = { Text("📁 Directory Picker") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                // Launch the directory picker
                openDocumentTreeLauncher.launch(null)
            }) {
                Text("Select Directory")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display the selected directory's path
            if (directoryUri != null) {
                Text(
                    text = "Selected Path:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = directoryUri?.path ?: "N/A",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("No directory selected.")
            }

            Spacer(modifier = Modifier.padding(vertical = 16.dp))

            // Display the list of files in the selected directory
            if (fileList.isNotEmpty()) {
                Text(
                    "Files in Directory:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(fileList) { fileName ->
                        Text(fileName, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}