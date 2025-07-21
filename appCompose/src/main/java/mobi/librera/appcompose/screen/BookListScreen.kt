package mobi.librera.appcompose.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import mobi.librera.appcompose.components.BookGrid
import mobi.librera.appcompose.components.BookSearchBar
import mobi.librera.appcompose.components.SelectedBooksBar
import mobi.librera.appcompose.model.DataModel

@Composable
fun BookListScreen(dataModel: DataModel) {
    Column {
        BookSearchBar(dataModel)
        SelectedBooksBar(dataModel, false)
        BookGrid(dataModel)
    }
}