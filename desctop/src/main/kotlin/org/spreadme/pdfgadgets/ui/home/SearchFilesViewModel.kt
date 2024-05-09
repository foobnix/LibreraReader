package org.spreadme.pdfgadgets.ui.home

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.spreadme.pdfgadgets.common.ViewModel
import org.spreadme.pdfgadgets.common.viewModelScope
import org.spreadme.pdfgadgets.model.FileMetadata
import org.spreadme.pdfgadgets.repository.FileMetadataParser
import org.spreadme.pdfgadgets.repository.FileMetadataRepository
import java.io.File

class SearchFilesViewModel(
) : ViewModel() {
    private val logger = KotlinLogging.logger { }

    val fileMetadatas = mutableStateListOf<FileMetadata>()

    fun load() {
        viewModelScope.launch {
            fileMetadatas.clear()
            //fileMetadatas.addAll(fileMetadataRepository.query())
            var root =
                File("/Users/dev/Library/CloudStorage/Dropbox/Projects/BookTestingDB/UserBooks")

            logger.debug("add root can read " + root.canRead())
            logger.debug("add root " + root.listFiles().size)
            root.listFiles().forEach {
                if (it.name.endsWith(".pdf")
                    || it.name.endsWith(".epub")
                ) {
                    logger.debug("add file " + it.name)
                    fileMetadatas.add(FileMetadataParser().parse(it.toPath()))

                }
                if (fileMetadatas.size > 10) {
                    return@forEach
                }
            }
        }
    }

    fun delete(fileMetadata: FileMetadata) {
        fileMetadatas.remove(fileMetadata)
        viewModelScope.launch {
        }
    }

    fun reacquire() {
        fileMetadatas.clear()
        viewModelScope.launch {
        }
    }

}