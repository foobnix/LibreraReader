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

            web.loadData(str, "text/html", "utf-8");



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
