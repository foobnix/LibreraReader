package fi.iki.elonen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.sys.ImageExtractor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class SampleServer extends NanoHTTPD {

    private Activity a;

    public SampleServer(Activity a) throws IOException {
        super(8080);
        this.a = a;

    }

    public void run() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    InputStream bitmapToStream = null;

    @Override
    public Response serve(IHTTPSession session) {
        String queryParameterString = session.getQueryParameterString();
        LOG.d("queryParameterString", queryParameterString);

        if (queryParameterString != null && queryParameterString.startsWith("img=")) {

            String path = queryParameterString.replace("img=", "");

            Bitmap bitmap = IMG.loadCoverPageWithEffect(path, IMG.getImageSize());

            ByteArrayInputStream bitmapToStream = ImageExtractor.bitmapToStream(bitmap);

            try {
                return new Response(Response.Status.OK, "image/jpg", bitmapToStream, -1);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        if (queryParameterString != null && queryParameterString.startsWith("screenshot=")) {
            String xy[] = queryParameterString.replace("screenshot=", "").split(",");
            int x = Integer.parseInt(xy[0]);
            int y = Integer.parseInt(xy[1]);

            if (x != 0 && y != 0) {
                final ViewGroup v1 = (ViewGroup) a.getWindow().getDecorView().getRootView();

                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis() + 100;
                // List of meta states found here:
                // developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
                int metaState = 0;
                final MotionEvent motionEvent1 = MotionEvent.obtain(downTime - 500, eventTime - 500, MotionEvent.ACTION_DOWN, x, y, metaState);
                final MotionEvent motionEvent2 = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, metaState);

                // final View item = findViewAt(v1, x, y);
                a.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        v1.dispatchTouchEvent(motionEvent1);
                        v1.dispatchTouchEvent(motionEvent2);
                    }
                });
            }


            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bitmapToStream = takeScreenshot(a);

                }
            });
            try {
                return new Response(Response.Status.OK, "image/png", bitmapToStream, -1);
            } catch (Exception e) {
                LOG.e(e);
            }

        }
        String script1 = " <script>function reload() { var d = new Date(); document.getElementById(\"img1\").src='?screenshot=0,0,'+d.getTime(); }</script>";

        String script = " <script>\n" + //
                "            function showCoords(event) {\n" + //
                "                var x = event.clientX;\n" + //
                "                var y = event.clientY;\n" + //
                "                var image = document.getElementById(\"img1\");                \n" + //
                "                var coords = \"?screenshot=\" + (x-image.offsetLeft) + \",\" + (y-image.offsetTop+image.offsetParent.scrollTop);\n" + //
                "                image.src = document.getElementById(\"msg\").innerHTML = coords;\n" + //
                "                setTimeout(function(){ reload(); }, 1000);\n" + //
                "                setTimeout(function(){ reload(); }, 2000);\n" + //
                "                setTimeout(function(){ reload(); }, 5000);\n" + //
                "            }\n" + //
                "    </script>";//

        String msg = "<html><body>" + script1 + "<h1>Librera server</h1>";

        msg += "<a href='#' onclick=\"reload();\">Reload</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='?screenshot=0,0' target='blank' >Download</a><br/> ";

        String sceenshot = "<p id=\"msg\"></p><img id='img1'  onclick=\"showCoords(event)\"  src='?screenshot=0,0'  \"/><br/>";

        msg += sceenshot;

        List<FileMeta> all = Collections.EMPTY_LIST; // AppDB.get().getAll();
        for (FileMeta meta : all) {
            String info = TxtUtils.getFileMetaBookName(meta);

            String img = "<img width='80' src='?img=" + meta.getPath() + "'  \"/>";
            msg += img + info + " (" + meta.getPath() + ")" + "<br/>";

        }
        msg += script;

        return newFixedLengthResponse(msg + "</body></html>");
    }

    public synchronized InputStream takeScreenshot(Activity a) {

        try {

            View v1 = a.getWindow().getDecorView().getRootView();
            // v1.setDrawingCacheEnabled(true);
            // Bitmap drawingCache = Bitmap.createBitmap(v1.getDrawingCache());
            // v1.setDrawingCacheEnabled(false);
            Bitmap drawingCache = screenShot(v1);

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            CompressFormat format = CompressFormat.PNG;
            drawingCache.compress(format, 100, os);

            byte[] byteArray = os.toByteArray();
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

            return byteArrayInputStream;
        } catch (Throwable e) {
            LOG.e(e);
        }
        return null;
    }

    public Bitmap screenShot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private View findViewAt(ViewGroup viewGroup, int x, int y) {
        LOG.d("findViewAt", x, y);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                View foundView = findViewAt((ViewGroup) child, x, y);
                if (foundView != null && foundView.isShown()) {
                    return foundView;
                }
            } else {
                int[] location = new int[2];
                child.getLocationOnScreen(location);
                Rect rect = new Rect(location[0], location[1], location[0] + child.getWidth(), location[1] + child.getHeight());
                if (rect.contains(x, y)) {
                    return child;
                }
            }
        }

        return null;
    }
}
