package mobi.librera.appcompose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ObserveLifecycleEvents(
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onResume: () -> Unit = {},
    onDestroy: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> onPause()
                Lifecycle.Event.ON_STOP -> onStop()
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_DESTROY -> onDestroy()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}