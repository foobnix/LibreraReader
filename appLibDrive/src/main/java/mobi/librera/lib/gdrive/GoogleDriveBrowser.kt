package mobi.librera.lib.gdrive

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun GoogleDriveBrowser(
    googleDriveHelper: GoogleDriveHelper,
    isSignedIn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val driveFiles = remember { mutableStateOf<List<DriveFileItem>>(emptyList()) }
    val currentFolderId = remember { mutableStateOf("root") }
    val folderStack =
        remember { mutableStateOf<List<Pair<String, String>>>(listOf("Drive" to "root")) }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    fun loadFiles(folderId: String = currentFolderId.value) {
        if (!isSignedIn) {
            Toast.makeText(context, "Please sign in to Google first", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isLoading.value = true
            errorMessage.value = null

            try {
                val result = googleDriveHelper.listFilesWithCovers(folderId)
                if (result.isSuccess) {
                    driveFiles.value = result.getOrNull() ?: emptyList()
                } else {
                    errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load files"
                    Toast.makeText(context, errorMessage.value, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Unknown error occurred"
                Toast.makeText(context, errorMessage.value, Toast.LENGTH_LONG).show()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun navigateToFolder(folderId: String, folderName: String) {
        currentFolderId.value = folderId
        folderStack.value += (folderName to folderId)
        loadFiles(folderId)
    }

    fun navigateBack() {
        if (folderStack.value.size > 1) {
            val newStack = folderStack.value.dropLast(1)
            folderStack.value = newStack
            currentFolderId.value = newStack.last().second
            loadFiles(newStack.last().second)
        }
    }

    // Load files when signed in status changes
    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            loadFiles()
        } else {
            driveFiles.value = emptyList()
            currentFolderId.value = "root"
            folderStack.value = listOf("Drive" to "root")
        }
    }

    Column(modifier = modifier) {
        // Navigation breadcrumb
        if (isSignedIn && folderStack.value.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (folderStack.value.size > 1) {
                        IconButton(
                            onClick = { navigateBack() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Go back",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = folderStack.value.joinToString(" > ") { it.first },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { loadFiles() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Content area
        Box(modifier = Modifier.weight(1f)) {
            when {
                !isSignedIn -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sign in to Google to browse your Drive",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                isLoading.value -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading files...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                driveFiles.value.isEmpty() && errorMessage.value == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "This folder is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn {
                        items(driveFiles.value) { file ->
                            DriveFileItemCard(
                                file = file,
                                onFolderClick = { navigateToFolder(file.id, file.name) },
                                onFileClick = {
                                    Toast.makeText(
                                        context,
                                        "File: ${file.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Here you could add download functionality
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriveFileItemCard(
    file: DriveFileItem,
    onFolderClick: () -> Unit,
    onFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                if (file.isFolder) {
                    onFolderClick()
                } else {
                    onFileClick()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File/Folder icon
            if (!file.isFolder && file.coverLink != null) {
                AsyncImage(
                    model = file.coverLink,
                    contentDescription = "Cover Image",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = when {
                        file.isFolder -> Icons.Default.Folder
                        file.mimeType.startsWith("image/") -> Icons.Default.Image
                        file.mimeType.startsWith("video/") -> Icons.Default.VideoFile
                        file.mimeType.startsWith("audio/") -> Icons.Default.AudioFile
                        file.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
                        file.mimeType.contains("document") -> Icons.Default.Description
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = when {
                        file.isFolder -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = file.getFileTypeDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!file.isFolder) {
                        Text(
                            text = file.getFormattedSize(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = file.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Navigate arrow for folders
            if (file.isFolder) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Enter folder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
