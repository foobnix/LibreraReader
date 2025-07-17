package mobi.librera.appcompose

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.components.AppNavigationBar
import mobi.librera.appcompose.components.BookGrid
import mobi.librera.appcompose.components.BookSearchBar
import mobi.librera.appcompose.core.searchBooks
import mobi.librera.appcompose.room.AppDatabase
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.screen.ManageStoragePermissionScreen
import mobi.librera.appcompose.ui.theme.LibreraTheme

class MainActivity : ComponentActivity() {
    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext, AppDatabase::class.java, "database"
        ).build()


        enableEdgeToEdge()
        setContent {
            val foundFiles = remember { mutableStateListOf<String>() }
            var title by remember { mutableStateOf("no") }

            LibreraTheme(darkTheme = false) {
                Scaffold(
                    topBar = { }, bottomBar = {
                        AppNavigationBar()
                    }, modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Surface(tonalElevation = 1.dp, modifier = Modifier.padding(innerPadding)) {

                        val isManageStorageGranted = Environment.isExternalStorageManager()
                        if (!isManageStorageGranted) {
                            ManageStoragePermissionScreen()
                        } else {


                            Column {
                                val scope = rememberCoroutineScope()
                                var searchText by remember { mutableStateOf("") }
                                BookSearchBar(
                                    foundFiles.size, onTextChanged = { text ->
                                        searchText = text
                                        title = searchText
                                    },
                                    onSearchBook = {
                                        foundFiles.clear()
                                        val elements = searchBooks()
                                        foundFiles.addAll(elements)

                                        val users = elements.map { Book(it, "", "") }

                                        scope.launch(Dispatchers.IO) {
                                            db.userDao().deleteAllBooks()
                                            db.userDao().insertAll(users)
                                        }

                                        title = "DISK"
                                    }

                                )

                                LaunchedEffect(searchText) {
                                    snapshotFlow { searchText }
                                        .debounce(500L)
                                        .filterNotNull()
                                        .collectLatest { query ->

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


                                Text("Source: $title")

                                LaunchedEffect(Unit) {
                                    val res = withContext(Dispatchers.IO) {
                                        db.userDao().getAll()
                                    }
                                    foundFiles.clear()
                                    if (res.isEmpty()) {
                                        val elements = searchBooks()
                                        foundFiles.addAll(elements)

                                        val users = elements.map { Book(it, "", "") }
                                        db.userDao().deleteAllBooks()
                                        db.userDao().insertAll(users)

                                        title = "DISK"
                                    } else {
                                        foundFiles.addAll(res.map { it.path })
                                        title = "ROOM"

                                    }

                                }

                                BookGrid(foundFiles)
                            }
                        }
                    }
                }
            }
        }
    }
}

