package mobi.librera.appcompose.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.util.Date

data class DownloadFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateModified: Date
)

fun fetchAllDownloadFiles(context: Context): List<DownloadFile> {
    Log.d("QueryDownloads", "Starting to query download files...")
    val downloadFiles = mutableListOf<DownloadFile>()

    // The columns to retrieve
    val projection = arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.Downloads.DISPLAY_NAME,
        MediaStore.Downloads.SIZE,
        MediaStore.Downloads.DATE_MODIFIED
    )

    // The query criteria (no specific selection, so we get all files)
    val selection = null
    val selectionArgs = null
    val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC" // Newest files first

    // Execute the query
    context.contentResolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor -> // '.use' ensures the cursor is closed automatically

        // Get column indices
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)

        Log.d("QueryDownloads", "Found ${cursor.count} files.")

        while (cursor.moveToNext()) {
            // Retrieve the data from the cursor
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getLong(sizeColumn)
            // Date is stored in seconds, so multiply by 1000 to get milliseconds
            val dateModified = Date(cursor.getLong(dateModifiedColumn) * 1000)

            // The Content URI for a specific file
            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                id
            )

            // Add the file details to our list
            downloadFiles.add(DownloadFile(contentUri, name, size, dateModified))
        }
    }


    // Now you have the list of files. You can display it in a RecyclerView, etc.
    if (downloadFiles.isEmpty()) {
        Log.d("QueryDownloads", "No files found in the Downloads directory.")
    } else {
        downloadFiles.forEach { file ->
            Log.d(
                "QueryDownloads",
                "File: ${file.name}, Size: ${file.size} bytes, URI: ${file.uri}"
            )
        }
    }
    return downloadFiles
}
