package mobi.librera.appcompose

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mobi.librera.appcompose.bookgrid.BookGridViewModel
import mobi.librera.appcompose.bookgrid.BookListScreen
import mobi.librera.appcompose.bookread.ReadBookScreen
import mobi.librera.appcompose.bookgrid.ManageStoragePermissionScreen
import mobi.librera.appcompose.ui.theme.LibreraTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("MutableCollectionMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dataModel: BookGridViewModel = koinViewModel()
            val isDark by dataModel.isDarkModeEnabled.collectAsState()

            LibreraTheme(darkTheme = isDark) {
                Scaffold(
                    topBar = { },

                    bottomBar = {//AppNavigationBar()
                    }, modifier = Modifier.fillMaxSize()
                ) { innerPadding ->


                    MainScreen(dataModel, innerPadding)


                }
            }
        }
    }

    @Composable
    private fun MainScreen(dataModel: BookGridViewModel, innerPadding: PaddingValues) {
        Surface(
            tonalElevation = 1.dp, modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isManageStorageGranted = Environment.isExternalStorageManager()
            if (!isManageStorageGranted) {
                ManageStoragePermissionScreen()
            } else {


                if (dataModel.currentBookPath.isEmpty()) {
                    BookListScreen(dataModel)
                } else {
                    ReadBookScreen(dataModel)
                }

                BackHandler(
                    enabled = dataModel.currentBookPath.isNotEmpty(),
                    onBack = { dataModel.currentBookPath = "" })
            }
        }
    }
}
