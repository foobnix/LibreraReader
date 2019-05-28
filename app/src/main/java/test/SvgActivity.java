package test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ImageView;

import com.foobnix.android.utils.LOG;

public class SvgActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            ImageView img = new ImageView(this);

            String str = "<figure xmlns=\"http://www.w3.org/1999/xhtml\" style=\"position:relative;\">\n" +
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gb=\"https://gobooks.com\" version=\"1.1\" viewBox=\"0 0 456 456\">\n" +
                    "\t<defs>\n" +
                    "\t\t<circle id=\"b\" r=\"11.5\" fill=\"black\" stroke=\"black\" stroke-width=\"1\"/>\n" +
                    "\t\t<circle id=\"h\" r=\"3\" fill=\"black\"/>\n" +
                    "\t</defs>\n" +
                    "\t<style type=\"text/css\">\n" +
                    ".wt { font-size: 18px; font-family: Helvetica; font-weight: 500; fill: white; text-anchor: middle; alignment-baseline: central; letter-spacing: -0.05em; }</style>\n" +
                    "\t<path d=\"M12,11V445M444,11V445M11,12H445M11,444H445\" stroke=\"black\" stroke-width=\"2.0\"/>\n" +
                    "\t<path d=\"M36,11V445M60,11V445M84,11V445M108,11V445M132,11V445M156,11V445M180,11V445M204,11V445M228,11V445M252,11V445M276,11V445M300,11V445M324,11V445M348,11V445M372,11V445M396,11V445M420,11V445M11,36H445M11,60H445M11,84H445M11,108H445M11,132H445M11,156H445M11,180H445M11,204H445M11,228H445M11,252H445M11,276H445M11,300H445M11,324H445M11,348H445M11,372H445M11,396H445M11,420H445\" stroke=\"black\" stroke-width=\"1.0\"/>\n" +
                    "\t<use x=\"84\" y=\"84\" xlink:href=\"#h\"/>\n" +
                    "\t<use x=\"228\" y=\"84\" xlink:href=\"#h\"/>\n" +
                    "\t<use x=\"372\" y=\"84\" xlink:href=\"#h\"/>\n" +
                    "\t<g gb:v=\"1\" visibility=\"visible\">\n" +

                    "\t\t<use x=\"396\" y=\"84\" xlink:href=\"#b\"/>\n" +
                    "\t</g>\n" +
                    "\t<g gb:v=\"1\" visibility=\"visible\">\n" +
                    "\t\t<text x=\"396\" y=\"84\" class=\"wt\">1</text>\n" +
                    "\t</g>\n" +
                    "\t<use x=\"84\" y=\"228\" xlink:href=\"#h\"/>\n" +
                    "\t<use x=\"228\" y=\"228\" xlink:href=\"#h\"/>\n" +
                    "\t<use x=\"372\" y=\"228\" xlink:href=\"#h\"/>\n" +
                    "\t<use x=\"84\" y=\"372\" xlink:href=\"#h\"/>\n" +
                    "\t<use x=\"228\" y=\"372\" xlink:href=\"#h\"/>\n" +
                    "\t<use x=\"372\" y=\"372\" xlink:href=\"#h\"/>\n" +
                    "\t<rect x=\"0\" y=\"0\" width=\"456\" height=\"456\" fill=\"none\" pointer-events=\"visible\" cursor=\"pointer\" onmousedown=\"toggle(this); return false;\" onclick=\"return false;\" onmouseup=\"return false;\"/>\n" +
                    "</svg><figcaption style=\"visibility: visible;\">Move 1</figcaption>\n" +
                    "</figure>";


//            WebViewUtils.renterToBitmap(str, "1", new WebViewUtils.WebViewResponse() {
//                @Override
//                public void onResult(String id, Bitmap result) {
//                    img.setImageBitmap(result);
//                }
//            });

            img.setBackgroundColor(Color.GRAY);

            setContentView(img);

        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static Bitmap screenshot(WebView webview) {

        float scale = webview.getScale();
        int webViewHeight = Math.round(webview.getContentHeight() * scale);
        LOG.d("SvgActivity ", scale, webViewHeight);
        Bitmap bitmap = Bitmap.createBitmap(webview.getWidth(), webViewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        webview.draw(canvas);
        return bitmap;
    }

}
