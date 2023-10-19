package mobi.librera.epub

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView


@Composable
fun LibreraWebView(
    modifier: Modifier,
    url: String,
    value: Float,
    zoom: Int,
    onValueChange: (Float) -> Unit,
    onDelta: (Float) -> Unit,
    onClick: () -> Unit,
) {
    return AndroidView(
        modifier = modifier,
        factory = {
            var webView = MyWebView(
                it,
                onClick = onClick
            )
            webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                onValueChange(scrollY.toFloat() / webView.contentHeight)
                onDelta(webView.height.toFloat() / webView.contentHeight)
            }

            webView.apply {
                loadUrl(url)
            }

        }, update = {
            it.scrollTo(0, (value * it.contentHeight).toInt())
            it.settings.textZoom = zoom

        })
}

class MyWebView constructor(
    context: Context,
    var onClick: () -> Unit
) :
    WebView(context) {

    private var computeVerticalScrollRange = 0
    override fun getContentHeight(): Int {
        computeVerticalScrollRange = computeVerticalScrollRange()
        return computeVerticalScrollRange
    }


    private var isClicked = false;
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            isClicked = true;
        }
        if (event?.action == MotionEvent.ACTION_MOVE) {
            isClicked = false;
        }
        if (event?.action == MotionEvent.ACTION_UP) {
            if (isClicked) {
                onClick()
            }
        }
        return super.onTouchEvent(event)


    }

    init {
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.javaScriptEnabled = true

        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        webViewClient = WebViewClient()
    }
}
