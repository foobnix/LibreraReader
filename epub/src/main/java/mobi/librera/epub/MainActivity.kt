package mobi.librera.epub

import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import mobi.librera.epub.ui.theme.LibreraReaderTheme
import androidx.compose.material3.IconButton as IconButton


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibreraReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var sliderPosition by remember { mutableFloatStateOf(0f) }
                    var delta by remember { mutableFloatStateOf(0f) }
                    val mUrl = "https://librera.mobi/"
                    var zoom by remember { mutableIntStateOf(125) }
                    var uiVisible by remember {
                        mutableStateOf(true)
                    }

                    Box(
                    ) {
                        LibreraWebView(
                            modifier = Modifier.align(Alignment.Center),
                            url = mUrl,
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            onDelta = { delta = it },
                            zoom = zoom,
                            onClick = { uiVisible = !uiVisible }
                        )

                        if (uiVisible) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .background(
                                        Color.LightGray
                                    )
                                    .padding(10.dp),
                            ) {
                                IconButton(onClick = { zoom -= 10 }) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        tint = Color.Blue,
                                        contentDescription = ""
                                    )
                                }
                                IconButton(onClick = { zoom += 10 }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        tint = Color.Blue,
                                        contentDescription = ""
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        tint = Color.Blue,
                                        contentDescription = ""
                                    )
                                }
                            }

                            Slider(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(
                                        Color.LightGray
                                    )
                                    .padding(10.dp),
                                value = sliderPosition,
                                valueRange = 0f..(1 - delta),
                                onValueChange = { sliderPosition = it })
                        }
                    }

                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LibreraReaderTheme {

    }
}