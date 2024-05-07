package org.spreadme.pdfgadgets.ui.frame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.spreadme.pdfgadgets.common.ViewModel
import java.nio.file.Path

class LoadProgressViewModel : ViewModel() {

    var status by mutableStateOf(LoadProgressStatus.FINISHED)
    var loadPath by mutableStateOf<Path?>(null)
    var message by mutableStateOf("")

    var onFail: () -> Unit = {}

    fun start() {
        status = LoadProgressStatus.LOADING
    }

    fun fail(message: String) {
        status = LoadProgressStatus.FAILURE
        this.message = message
        onFail()
    }

    fun needPassword(path: Path, message: String) {
        this.loadPath = path
        this.message = message
        status = LoadProgressStatus.NEED_PASSWORD
    }

}

enum class LoadProgressStatus {
    FINISHED,
    LOADING,
    NEED_PASSWORD,
    FAILURE
}