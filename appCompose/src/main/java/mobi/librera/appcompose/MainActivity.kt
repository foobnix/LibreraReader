package mobi.librera.appcompose

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mobi.librera.appcompose.core.firstOrDefault
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
                            var cache = remember {
                                mutableStateMapOf<String, Int>()
                            }



                            if (openBookPath.isEmpty()) {
                                BookListScreen(onBookOpen = {
                                    openBookPath = it.path
                                })
                            } else {

                                cache[openBookPath] = cache.getOrDefault(openBookPath, 0)
                                Column(
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 4.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {


                                        Box(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color.Blue.copy(alpha = 0.8f))
                                                .clickable { openBookPath = "" }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Home,
                                                contentDescription = "",
                                                tint = Color.White,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                        val scrollState = rememberScrollState()

                                        Row(modifier = Modifier.horizontalScroll(scrollState)) {


                                            cache.forEach { (bookPath, page) ->
                                                Row(
                                                    Modifier
                                                        .width(140.dp)
                                                        .height(42.dp)
                                                        .padding(2.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Blue.copy(alpha = 0.8f)),
                                                    verticalAlignment = Alignment.CenterVertically

                                                ) {
                                                    AsyncImage(
                                                        model = bookPath,

                                                        contentDescription = "",
                                                        modifier = Modifier
                                                            .height(42.dp)
                                                            .width(42.dp)
                                                            .clip(CircleShape)
                                                            .clickable {
                                                                openBookPath = bookPath
                                                            },
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                    Text(
                                                        bookPath.substringAfterLast("/"),
                                                        maxLines = 1,
                                                        modifier = Modifier
                                                            .padding(start = 16.dp)
                                                            .weight(1f)
                                                            .clickable {
                                                                openBookPath = bookPath

                                                            },
                                                        style = TextStyle(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )

                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            cache.remove(bookPath)
                                                            openBookPath =
                                                                cache.keys.firstOrDefault("")
                                                        }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            tint = Color.White,
                                                            contentDescription = "Close",
                                                        )
                                                    }

                                                }

                                            }
                                        }

                                    }

                                    ReadBookScreen(
                                        bookPath = openBookPath,
                                        page = cache.getOrDefault(openBookPath, 0),
                                        onBookClose = {
                                            openBookPath = ""
                                        },
                                        onPageChanged = { page ->
                                            cache[openBookPath] = page
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
