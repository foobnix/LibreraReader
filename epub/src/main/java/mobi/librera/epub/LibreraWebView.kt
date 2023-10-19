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
            var webView = MyWebView(it,
                onValueChange = { }, onClick = onClick
            )
            webView.setOnScrollChangeListener { p0, scrollX, scrollY, p3, p4 ->
                onValueChange(scrollY.toFloat() / webView.contentHeight)
                onDelta(webView.height.toFloat() / webView.contentHeight)
            }

            webView.apply {
                //settings.textZoom = (zoom * 100).toInt();
                loadUrl(url)
            }

        }, update = {
            it.scrollTo(0, (value * it.contentHeight).toInt())
            it.settings.textZoom = zoom

        })
}

class MyWebView constructor(
    context: Context,
    private val onValueChange: (Float) -> Unit,
    var onClick: () -> Unit
) :
    WebView(context) {

    private var computeVerticalScrollRange = 0
    override fun getContentHeight(): Int {
        //if (computeVerticalScrollRange == 0) {
        computeVerticalScrollRange = computeVerticalScrollRange()

        //}
        return computeVerticalScrollRange
    }


    var isClicked = false;
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
            Log.d("onTouchEvent", "onTouchEvent");
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
