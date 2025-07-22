package mobi.librera.appcompose.core

import android.os.Environment
import java.io.File

val DEFAULT_SEARCH_DIR = Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOWNLOADS
).resolve("")

class FilesRepository() {

    fun getAllBooks(isPDF: Boolean, isEPUB: Boolean, searchDir: String): List<String> {
        val foundFiles = mutableListOf<String>()


        val downloadsDir =
            if (searchDir.toFile().isDirectory) searchDir.toFile() else DEFAULT_SEARCH_DIR

        println("Search Dir: $downloadsDir")

        downloadsDir.walkTopDown().filter { it.isFile }.forEach { file ->
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

fun String.toFile(): File = File(this)


