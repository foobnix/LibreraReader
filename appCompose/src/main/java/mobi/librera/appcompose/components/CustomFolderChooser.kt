package mobi.librera.appcompose.components

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun CustomFolderChooser(
    onFolderSelected: (File) -> Unit,
    initialPath: File = Environment.getExternalStorageDirectory()
) {
    var currentDirectory by remember { mutableStateOf(initialPath) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "${currentDirectory.absolutePath}",
            style = TextStyle(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Navigate up button (if not at the root or external storage root)
        if (currentDirectory.parentFile != null && !currentDirectory.absolutePath.equals(
                Environment.getExternalStorageDirectory().absolutePath,
                ignoreCase = true
            )
        ) {
            Button(
                onClick = { currentDirectory = currentDirectory.parentFile!! },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go Up (..)")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val folders = remember(currentDirectory) {
            currentDirectory.listFiles()
                ?.filter { it.isDirectory && it.canRead() }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        }

        LazyColumn(modifier = Modifier.weight(1f)) {


            if (folders.isEmpty() && currentDirectory.isDirectory) {
                item {
                    Text(
                        "No subfolders found in this directory.",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else if (!currentDirectory.isDirectory) {
                item {
                    Text("Cannot read this directory.", modifier = Modifier.padding(8.dp))
                }
            }

            items(folders) { folder ->
                Text(
                    text = "📁 ${folder.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currentDirectory = folder }
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onFolderSelected(currentDirectory) },
            modifier = Modifier.fillMaxWidth(),
            enabled = currentDirectory.isDirectory && currentDirectory.canRead() // Ensure it's a valid folder
        ) {
            Text("Select This Folder")
        }
    }
}