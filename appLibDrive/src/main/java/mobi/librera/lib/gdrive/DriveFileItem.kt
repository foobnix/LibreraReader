package mobi.librera.lib.gdrive

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DriveFileItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedTime: Long,
    val isFolder: Boolean,
    val webViewLink: String?,
    val parentId: String,
    val coverLink: String?
) {
    fun getFormattedSize(): String {
        if (isFolder) return "Folder"

        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    fun getFormattedDate(): String {
        val date = Date(modifiedTime)
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

    fun getFileTypeDescription(): String {
        return when {
            isFolder -> "Folder"
            mimeType.startsWith("image/") -> "Image"
            mimeType.startsWith("video/") -> "Video"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType == "application/pdf" -> "PDF Document"
            mimeType.contains("document") -> "Document"
            mimeType.contains("spreadsheet") -> "Spreadsheet"
            mimeType.contains("presentation") -> "Presentation"
            mimeType.startsWith("text/") -> "Text File"
            else -> "File"
        }
    }
}
