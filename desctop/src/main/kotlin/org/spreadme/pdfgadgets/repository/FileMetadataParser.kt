package org.spreadme.pdfgadgets.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.spreadme.pdfgadgets.model.FileMetadata
import org.spreadme.common.uuid
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

class FileMetadataParser {

    suspend fun parse(path: Path): FileMetadata {
        val attributes = withContext(Dispatchers.IO) {
            Files.readAttributes(path, BasicFileAttributes::class.java)
        }
        return FileMetadata(
            uuid(),
            path.toString(),
            path.fileName.toString(),
            length = attributes.size(),
            openTime = Date()
        )
    }
}