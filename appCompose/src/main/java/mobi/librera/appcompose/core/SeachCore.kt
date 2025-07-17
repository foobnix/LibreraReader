package mobi.librera.appcompose.core

import android.os.Environment

fun searchBooks(): List<String> {
    val foundFiles = mutableListOf<String>()

    val downloadsDir =
        Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        ).resolve("Librera")

    println("Search Dir: $downloadsDir")

    downloadsDir.walkTopDown().forEach { file ->
        println(file)
        if (file.isFile) {
            when (file.extension.lowercase()) {
                "pdf", "epub" -> foundFiles.add(file.absolutePath)
            }
        }
    }
    return foundFiles
}
