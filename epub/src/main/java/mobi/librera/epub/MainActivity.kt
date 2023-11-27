package mobi.librera.epub

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.Listener
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MediaSourceFactory
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import mobi.librera.epub.ui.theme.LibreraReaderTheme


class MainActivity : ComponentActivity() {
    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibreraReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //WebViewReader(url = "https://librera.mobi/")
                    val context = LocalContext.current
                    val player1 = remember { ExoPlayer.Builder(context).build() }


                    val trac1 =
                        MediaItem.fromUri("https://cdn.mp3xa.cc/ib4_X7o4MPWinjtoGf-BJg/1700794299/L21wMy8yMDIwLzAzL9Cb0LDQvNCwIC0g0JzQtdC90ZYg0KLQsNC6INCi0YDQtdCx0LAtMjAwNi5tcDM")

                    val trac2 =
                        MediaItem.fromUri("https://cdn.mp3xa.cc/ib4_X7o4MPWinjtoGf-BJg/1700794299/L21wMy8yMDIwLzAzL9Cb0LDQvNCwIC0g0JzQtdC90ZYg0KLQsNC6INCi0YDQtdCx0LAtMjAwNi5tcDM")

                    val trac3 =
                        MediaItem.fromUri("https://cdn.mp3xa.cc/ib4_X7o4MPWinjtoGf-BJg/1700794299/L21wMy8yMDIwLzAzL9Cb0LDQvNCwIC0g0JzQtdC90ZYg0KLQsNC6INCi0YDQtdCx0LAtMjAwNi5tcDM")


                    player1.setMediaItems(listOf(trac1, trac2, trac3))


                    var trackName by remember { mutableStateOf("") }

                    player1.addListener(object : Listener {
                        override fun onTracksChanged(tracks: Tracks) {
                            super.onTracksChanged(tracks)
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            super.onPlaybackStateChanged(playbackState)
                            if (playbackState == ExoPlayer.STATE_READY) {
                                trackName = "position: ${player1.currentPosition}"
                            }
                        }

                    })
                    player1.prepare()



                    Column {

                        VideoView(videoUri = "https://cdn.mp3xa.cc/ib4_X7o4MPWinjtoGf-BJg/1700794299/L21wMy8yMDIwLzAzL9Cb0LDQvNCwIC0g0JzQtdC90ZYg0KLQsNC6INCi0YDQtdCx0LAtMjAwNi5tcDM")

                        Text(text = "Track: ...$trackName")

                        Button(onClick = {
                            player1.prepare();
                            player1.play()
                        }) {
                            Text("play")
                        }
                        Button(onClick = { player1.pause() }) {
                            Text("pause")
                        }
                        Button(onClick = { player1.seekToNextMediaItem() }) {
                            Text("next")
                        }
                    }

                }
            }
        }
    }
}

@UnstableApi @Composable
fun VideoView(videoUri: String) {
    val context = LocalContext.current

    val exoPlayer = ExoPlayer.Builder(LocalContext.current)
        .setMediaSourceFactory(object : MediaSource.Factory{
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
                TODO("Not yet implemented")
            }

            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
                TODO("Not yet implemented")
            }

            override fun getSupportedTypes(): IntArray {
                TODO("Not yet implemented")
            }

            override fun createMediaSource(mediaItem: MediaItem): MediaSource {
                TODO("Not yet implemented")
            }

        })
        .build()
        .also { exoPlayer ->
            val mediaItem = MediaItem.Builder()
                .setUri(videoUri)
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

    DisposableEffect(

        AndroidView(factory = {
            PlayerControlView(context).apply {
                player = exoPlayer
            }
        })
    ) {
        onDispose { exoPlayer.release() }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LibreraReaderTheme {

    }
}