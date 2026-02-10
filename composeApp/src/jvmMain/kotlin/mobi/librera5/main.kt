package mobi.librera5

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import mobi.librera5.ui.BookLibraryScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Book Library",
    ) {

        
        BookLibraryScreen()
    }
}