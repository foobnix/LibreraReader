package mobi.librera.appcompose

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mobi.librera.appcompose.screen.BookListScreen
import mobi.librera.appcompose.screen.ManageStoragePermissionScreen
import mobi.librera.appcompose.screen.ReadBookScreen
import mobi.librera.appcompose.ui.theme.LibreraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibreraTheme(darkTheme = false) {
                Scaffold(
                    topBar = { }, bottomBar = {
                        //AppNavigationBar()
                    }, modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Surface(tonalElevation = 1.dp, modifier = Modifier.padding(innerPadding)) {
                        val isManageStorageGranted = Environment.isExternalStorageManager()
                        if (!isManageStorageGranted) {
                            ManageStoragePermissionScreen()
                        } else {
                            var openBookPath by remember { mutableStateOf("") }

                            if (openBookPath.isEmpty()) {
                                BookListScreen(onBookOpen = {
                                    openBookPath = it
                                })
                            } else {
                                ReadBookScreen(
                                    bookPath = openBookPath,
                                    onBookClose = { openBookPath = "" })
                            }

                        }
                    }
                }
            }
        }

    }
}
