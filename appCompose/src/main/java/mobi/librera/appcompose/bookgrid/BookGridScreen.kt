package mobi.librera.appcompose.bookgrid

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import mobi.librera.appcompose.booksync.GoogleSingInViewModel
import mobi.librera.appcompose.components.BookGrid
import mobi.librera.appcompose.components.BookSearchBar
import mobi.librera.appcompose.components.GoogleSignInButton
import mobi.librera.appcompose.components.SelectedBooksBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun BookListScreen(dataModel: BookGridViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val googleModel: GoogleSingInViewModel = koinViewModel()
        val context1 = LocalContext.current
        val scope = rememberCoroutineScope()

        BookSearchBar(dataModel)

        GoogleSignInButton(onClick = {
            scope.launch {
                googleModel.signInWithGoogle(context = context1)
            }
        })

        SelectedBooksBar(dataModel, false)

        val books by dataModel.getAllBooks.collectAsState()

        BookGrid(
            books, dataModel.listGridStates,
            onStarClicked = { dataModel.updateStar(it, !it.isSelected) },
            onBookClicked = { dataModel.currentBookPath = it.path })
    }
}

@Preview
@Composable
fun Preview() {
    val dataModel: BookGridViewModel = koinViewModel()
    BookListScreen(dataModel)
}