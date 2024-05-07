package org.spreadme.pdfgadgets.ui.common

import java.awt.FileDialog
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFrame

fun FileDialog(
    parent: JFrame,
    title: String,
    mode: FileDialogMode = FileDialogMode.LOAD,
    exts: ArrayList<String> = arrayListOf(),
    onFileOpen: (Path) -> Unit = {},
    onFileSave: (Path) -> Unit = {}
) {
    val fileDialog = FileDialog(parent, title, mode.value())
    fileDialog.isVisible = true
    if (mode == FileDialogMode.LOAD) {
        fileDialog.filenameFilter = ApplicationFilenameFilter(exts)
    }
    if (fileDialog.directory != null && fileDialog.file != null) {
        val path = Paths.get(fileDialog.directory, fileDialog.file)
        if (Files.exists(path) && mode == FileDialogMode.LOAD) {
            onFileOpen(path)
        } else if (mode == FileDialogMode.SAVE) {
            onFileSave(path)
        }
    }
}

enum class FileDialogMode(private val mode: Int) {
    LOAD(FileDialog.LOAD),
    SAVE(FileDialog.SAVE);

    fun value(): Int {
        return mode
    }
}

class ApplicationFilenameFilter(
    private val exts: ArrayList<String>
) : FilenameFilter {

    override fun accept(dir: File?, name: String?): Boolean {
        if(exts.isEmpty()) {
            return true
        }
        val ext = name?.split(".")?.last() ?: ""
        return exts.contains(ext)
    }
}