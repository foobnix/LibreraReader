package mobi.librera.appcompose.bookgrid

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import mobi.librera.appcompose.booksync.GoogleSignInScreen
import mobi.librera.appcompose.components.BookGrid
import mobi.librera.appcompose.components.BookSearchBar

@Composable
fun BookGridScreen(
    dataModel: BookGridViewModel,
    onOpenBook: (String) -> Unit,
    onHomeClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BookSearchBar(dataModel)

        var isManageStorageGranted by remember { mutableStateOf(Environment.isExternalStorageManager()) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            isManageStorageGranted = Environment.isExternalStorageManager()
            if (isManageStorageGranted) {
                dataModel.loadInitialBooks()
            }
        }

        if (!isManageStorageGranted) {
            ManageStoragePermissionScreen(launcher)
        } else {
            GoogleSignInScreen()

//            SelectedBooksBar(
//                dataModel, false,
//                onOpenBook = onOpenBook,
//                onHomeClick = onHomeClick
//            )

            val books by dataModel.getAllBooks.collectAsState()

            BookGrid(
                dataModel,
                books, dataModel.listGridStates,
                onStarClicked = { dataModel.updateStar(it, !it.isSelected) },
                onBookClicked = { onOpenBook(it.path) })
        }
    }
}