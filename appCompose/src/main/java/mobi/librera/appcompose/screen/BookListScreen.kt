package mobi.librera.appcompose.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.components.BookGrid
import mobi.librera.appcompose.components.BookSearchBar
import mobi.librera.appcompose.components.SelectedBooksBar
import mobi.librera.appcompose.core.searchBooks
import mobi.librera.appcompose.model.DataModel
import mobi.librera.appcompose.room.Book

@Composable
fun BookListScreen(dataModel: DataModel) {

    val books by dataModel.allBooks.collectAsState()

    val context = LocalContext.current
    val foundFiles = remember { mutableStateListOf<String>() }
    var title by remember { mutableStateOf("no") }

    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }



    Column {
        Text("Source: $title")

        BookSearchBar(foundFiles.size, onTextChanged = { text ->
            searchText = text
            title = searchText
        }, onSearchBook = {
            foundFiles.clear()
            val elements = searchBooks()
            foundFiles.addAll(elements)

            val users = elements.map { Book(it) }

            scope.launch(Dispatchers.Main) {
                //db.userDao().deleteAllBooks()
                // db.userDao().insertAll(users)
            }

            title = "DISK"
        }

        )

        SelectedBooksBar(dataModel, false)

        LaunchedEffect(searchText) {
            snapshotFlow { searchText }.debounce(500L).filterNotNull().collectLatest { query ->

                scope.launch {
                    val filteredResults = if (query.isNotBlank()) {
                        books.filter {
                            it.path.contains(
                                query, ignoreCase = true
                            )
                        }
                    } else {
                        books
                    }


                    withContext(Dispatchers.Main) {
                        foundFiles.clear()
                        foundFiles.addAll(filteredResults.map { it.path })
                    }
                }

            }
        }


        //Text("Source: $title")

        LaunchedEffect(Unit) {

            foundFiles.clear()
            if (books.isEmpty()) {
                val elements = searchBooks()
                foundFiles.addAll(elements)

                val users = elements.map { Book(it) }
                dataModel.insertAll(users)

                title = "DISK"
            } else {
                foundFiles.addAll(books.map { it.path })
                title = "ROOM"

            }

        }

        BookGrid(dataModel)
    }
}