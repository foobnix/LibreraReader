package mobi.librera.appcompose.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.components.BookGrid
import mobi.librera.appcompose.components.BookSearchBar
import mobi.librera.appcompose.core.searchBooks
import mobi.librera.appcompose.room.AppDatabase
import mobi.librera.appcompose.room.Book

@Composable
fun BookListScreen(onBookOpen: (String) -> Unit) {

    val context = LocalContext.current
    val foundFiles = remember { mutableStateListOf<String>() }
    var title by remember { mutableStateOf("no") }

    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }

    val db = remember {
        Room.databaseBuilder(
            context, AppDatabase::class.java, "database1"
        ).build()
    }


    Column {

        BookSearchBar(foundFiles.size, onTextChanged = { text ->
            searchText = text
            title = searchText
        }, onSearchBook = {
            foundFiles.clear()
            val elements = searchBooks()
            foundFiles.addAll(elements)

            val users = elements.map { Book(it, "", "") }

            scope.launch(Dispatchers.Main) {
                //db.userDao().deleteAllBooks()
                // db.userDao().insertAll(users)
            }

            title = "DISK"
        }

        )

        LaunchedEffect(searchText) {
            snapshotFlow { searchText }.debounce(500L).filterNotNull().collectLatest { query ->

                    scope.launch {
                        val filteredResults = withContext(Dispatchers.IO) {
                            if (query.isNotBlank()) {
                                db.userDao().getAll().filter {
                                    it.path.contains(
                                        query, ignoreCase = true
                                    )
                                }
                            } else {
                                db.userDao().getAll()
                            }
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
            val res = withContext(Dispatchers.IO) {
                db.userDao().getAll()
            }
            foundFiles.clear()
            if (res.isEmpty()) {
                val elements = searchBooks()
                foundFiles.addAll(elements)

                val users = elements.map { Book(it, "", "") }
                withContext(Dispatchers.IO) {
                    //db.userDao().deleteAllBooks()
                    db.userDao().insertAll(users)
                }

                title = "DISK"
            } else {
                foundFiles.addAll(res.map { it.path })
                title = "ROOM"

            }

        }

        BookGrid(foundFiles, onBookOpen = onBookOpen)
    }
}