package mobi.librera.appcompose

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mobi.librera.appcompose.bookgrid.BookGridViewModel
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

                    Surface(
                        tonalElevation = 1.dp, modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavigationRoot()
                        //DirectoryPickerScreen()
                    }

                }
            }
        }
    }
}
