package mobi.librera.appcompose.bookgrid

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import mobi.librera.appcompose.booksync.GoogleSignInScreen
import mobi.librera.appcompose.components.BookGrid
import mobi.librera.appcompose.components.BookSearchBar
import mobi.librera.appcompose.components.SelectedBooksBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun BookGridScreen(
    dataModel: BookGridViewModel,
    onOpenBook: (String) -> Unit,
    onHomeClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        BookSearchBar(dataModel)

        GoogleSignInScreen()

        SelectedBooksBar(
            dataModel, false,
            onOpenBook = onOpenBook,
            onHomeClick = onHomeClick
        )

        val books by dataModel.getAllBooks.collectAsState()

        BookGrid(
            books, dataModel.listGridStates,
            onStarClicked = { dataModel.updateStar(it, !it.isSelected) },
            onBookClicked = { onOpenBook(it.path) })
    }
}

@Preview
@Composable
fun Preview() {
    val dataModel: BookGridViewModel = koinViewModel()
    //BookListScreen(dataModel)
}