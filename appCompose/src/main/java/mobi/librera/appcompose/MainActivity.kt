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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mobi.librera.appcompose.components.AppNavigationBar
import mobi.librera.appcompose.components.BookGrid
import mobi.librera.appcompose.components.BookSearchBar
import mobi.librera.appcompose.core.searchBooks
import mobi.librera.appcompose.screen.ManageStoragePermissionScreen
import mobi.librera.appcompose.ui.theme.LibreraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val foundFiles = remember { mutableStateListOf<String>() }

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
                                BookSearchBar(foundFiles.size)

                                LaunchedEffect(Unit) {
                                    foundFiles.clear()
                                    foundFiles.addAll(searchBooks())
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

