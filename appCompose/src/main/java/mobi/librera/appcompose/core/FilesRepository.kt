package mobi.librera.appcompose.core

import android.os.Environment

class FilesRepository {

    fun getAllBooks(isPDF: Boolean, isEPUB: Boolean): List<String> {
        val foundFiles = mutableListOf<String>()

        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        ).resolve("Librera")

        println("Search Dir: $downloadsDir")

        downloadsDir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val ext = file.extension.lowercase()
                if (isPDF && ext == "pdf") {
                    foundFiles.add(file.absolutePath)
                } else if (isEPUB && ext == "epub") {
                    foundFiles.add(file.absolutePath)
                }
            }
        return foundFiles
    }

}