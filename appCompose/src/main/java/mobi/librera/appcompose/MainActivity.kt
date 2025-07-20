package mobi.librera.appcompose

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mobi.librera.appcompose.components.SelectedBooksBar
import mobi.librera.appcompose.screen.BookListScreen
import mobi.librera.appcompose.screen.ManageStoragePermissionScreen
import mobi.librera.appcompose.screen.ReadBookScreen
import mobi.librera.appcompose.ui.theme.LibreraTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("MutableCollectionMutableState")
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
                                    openBookPath = it.path
                                })
                            } else {

                                Column(
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 4.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {


                                        SelectedBooksBar(true)

                                        ReadBookScreen(
                                            bookPath = openBookPath,
                                            page = 0,
                                            onBookClose = {
                                                openBookPath = ""
                                            },
                                            onPageChanged = { page ->

                                            })
                                    }
                                }

                            }

                        }

                    }

                }
            }
        }
    }
}
