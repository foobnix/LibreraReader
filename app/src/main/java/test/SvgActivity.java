package test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.webkit.WebView;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.WebViewUtils;

public class SvgActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            //ImageView img = new ImageView(this);

            String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gb=\"https://gobooks.com\" version=\"1.1\" viewBox=\"0 0 216 192\">\n" +
                    "\t<defs>\n" +
                    "\t\t<circle id=\"b\" r=\"11.5\" fill=\"black\" stroke=\"black\" stroke-width=\"1\"/>\n" +
                    "\t\t<circle id=\"w\" r=\"11.5\" fill=\"white\" stroke=\"black\" stroke-width=\"1\"/>\n" +
                    "\t\t<circle id=\"h\" r=\"3\" fill=\"black\"/>\n" +
                    "\t\t<polygon id=\"tb\" points=\"0,-11.04 -9.912,5.04 9.912,5.04\" stroke=\"white\" stroke-width=\"1.286\" fill=\"none\"/>\n" +
                    "\t\t<polygon id=\"tw\" points=\"0,-11.04 -9.912,5.04 9.912,5.04\" stroke=\"black\" stroke-width=\"1.286\" fill=\"none\"/>\n" +
                    "\t</defs>\n" +
                    "\t<style type=\"text/css\">\n" +
                    ".bt { font-size: 18px; font-family: Helvetica; font-weight: 500; fill: black; text-anchor: middle; alignment-baseline: central; letter-spacing: -0.05em; }</style>\n" +
                    "\t<path d=\"M12,0V192M36,0V192M60,0V192M84,0V192M108,0V192M132,0V122.88M132,141.12V192M156,0V192M180,0V192M204,0V192M0,12H216M0,36H216M0,60H216M0,84H216M0,108H216M0,132H122.88M141.12,132H216M0,156H216M0,180H216\" stroke=\"black\" stroke-width=\"1.0\"/>\n" +
                    "\t<use x=\"108\" y=\"36\" xlink:href=\"#b\"/>\n" +
                    "\t<use x=\"36\" y=\"60\" xlink:href=\"#w\"/>\n" +
                    "\t<use x=\"60\" y=\"60\" xlink:href=\"#w\"/>\n" +
                    "\t<use x=\"84\" y=\"60\" xlink:href=\"#w\"/>\n" +
                    "\t<use x=\"108\" y=\"60\" xlink:href=\"#b\"/>\n" +
                    "\t<use x=\"204\" y=\"60\" xlink:href=\"#h\"/>\n" +
                    "\t<use x=\"36\" y=\"84\" xlink:href=\"#w\"/>\n" +
                    "\t<use x=\"60\" y=\"84\" xlink:href=\"#b\"/>\n" +
                    "\t<use x=\"84\" y=\"84\" xlink:href=\"#b\"/>\n" +
                    "\t<use x=\"108\" y=\"84\" xlink:href=\"#w\"/>\n" +
                    "\t<use x=\"132\" y=\"84\" xlink:href=\"#b\"/>\n" +
                    "\t<use x=\"132\" y=\"84\" xlink:href=\"#tb\"/>\n" +
                    "\t<use x=\"108\" y=\"108\" xlink:href=\"#w\"/>\n" +
                    "\t<use x=\"60\" y=\"132\" xlink:href=\"#b\"/>\n" +
                    "\t<use x=\"84\" y=\"132\" xlink:href=\"#b\"/>\n" +
                    "\t<text x=\"132\" y=\"132\" class=\"bt\">a</text>\n" +
                    "</svg>";


            String str = "<html>\n" +
                    "       <head>\n" +
                    "          <meta charset = \"UTF-8\">\n" +
                    "          <title>MathML Examples</title>\n" +
                    "    \n" +
                    "          <script type=\"text/javascript\" async\n" +
                    "          src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/latest.js?config=TeX-MML-AM_CHTML\">\n" +
                    "        </script>\n" +
                    "    \n" +
                    "       </head>\n" +
                    "    \t\n" +
                    "       <body><math xmlns = \"http://www.w3.org/1998/Math/MathML\">\t\t         <mrow>            <mi>A</mi>            <mo>=</mo>\t\t\t            <mfenced open = \"[\" close=\"]\">\t\t\t               <mtable>                  <mtr>                     <mtd><mi>x</mi></mtd>                     <mtd><mi>y</mi></mtd>                  </mtr>\t\t\t\t\t                  <mtr>                     <mtd><mi>z</mi></mtd>                     <mtd><mi>w</mi></mtd>                  </mtr>               </mtable>                           </mfenced>         </mrow>      </math></body>\n" +
                    "    </html>";



            final WebView web = WebViewUtils.web;

            web.loadData(svg, "text/html", "utf-8");



            setContentView(web);

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
