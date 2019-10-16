package test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

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


            String mathml = "<html>\n" +
                    "       <head>\n" +
                    "          <script type=\"text/javascript\" \n" +
                    "          src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/MathJax.js?config=MML_CHTML\">\n" +
                    //"          src=\"**token**MathJax-2.7.5/MathJax.js?config=MML_CHTML\">\n" +
                    "        </script>\n" +
                    "<script type=\"text/javascript\"> MathJax.Hub.Register.StartupHook(\"End\",function () {  android.showToast(\"finish\"); }); </script>\n" +

                    "       </head>\n" +
                    "       <body>" +
                    "<math alttext=\"Alternative text not available\" xmlns=\"http://www.w3.org/1998/Math/MathML\">\n" +
                    "                                        <mi>T</mi>\n" +
                    "                                        <mo class=\"MathClass-punc\">:</mo>\n" +
                    "                                        <msup>\n" +
                    "                                            <mrow>\n" +
                    "                                                <mi>ℂ</mi>\n" +
                    "                                            </mrow>\n" +
                    "                                            <mrow>\n" +
                    "                                                <mn>5</mn>\n" +
                    "                                            </mrow>\n" +
                    "                                        </msup>\n" +
                    "                                        <mo class=\"MathClass-rel\">→</mo>\n" +
                    "                                        <msup>\n" +
                    "                                            <mrow>\n" +
                    "                                                <mi>ℂ</mi>\n" +
                    "                                            </mrow>\n" +
                    "                                            <mrow>\n" +
                    "                                                <mn>5</mn>\n" +
                    "                                            </mrow>\n" +
                    "                                        </msup>\n" +
                    "                                        <mo class=\"MathClass-punc\">,</mo>\n" +
                    "                                        <mspace width=\"1em\" class=\"quad\"></mspace>\n" +
                    "                                        <mi>T</mi>\n" +
                    "                                        <mfenced separators=\"\" open=\"(\" close=\")\">\n" +
                    "                                            <mrow>\n" +
                    "                                                <mfenced separators=\"\" open=\"[\" close=\"]\">\n" +
                    "                                                    <mrow>\n" +
                    "                                                        <mtable style=\"text-align:axis;\" equalrows=\"false\" columnlines=\"none none none none none none none none none none none\" equalcolumns=\"false\" class=\"array\">\n" +
                    "                                                            <mtr>\n" +
                    "                                                                <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                                    <msub>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mi>x</mi>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mn>1</mn>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                    </msub>\n" +
                    "                                                                </mtd>\n" +
                    "                                                            </mtr>\n" +
                    "                                                            <mtr>\n" +
                    "                                                                <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                                    <msub>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mi>x</mi>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mn>2</mn>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                    </msub>\n" +
                    "                                                                </mtd>\n" +
                    "                                                            </mtr>\n" +
                    "                                                            <mtr>\n" +
                    "                                                                <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                                    <msub>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mi>x</mi>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mn>3</mn>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                    </msub>\n" +
                    "                                                                </mtd>\n" +
                    "                                                            </mtr>\n" +
                    "                                                            <mtr>\n" +
                    "                                                                <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                                    <msub>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mi>x</mi>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mn>4</mn>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                    </msub>\n" +
                    "                                                                </mtd>\n" +
                    "                                                            </mtr>\n" +
                    "                                                            <mtr>\n" +
                    "                                                                <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                                    <msub>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mi>x</mi>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                        <mrow>\n" +
                    "                                                                            <mn>5</mn>\n" +
                    "                                                                        </mrow>\n" +
                    "                                                                    </msub>\n" +
                    "                                                                </mtd>\n" +
                    "                                                            </mtr>\n" +
                    "                                                        </mtable>\n" +
                    "                                                    </mrow>\n" +
                    "                                                </mfenced>\n" +
                    "                                            </mrow>\n" +
                    "                                        </mfenced>\n" +
                    "                                        <mo class=\"MathClass-rel\">=</mo>\n" +
                    "                                        <mfenced separators=\"\" open=\"[\" close=\"]\">\n" +
                    "                                            <mrow>\n" +
                    "                                                <mtable style=\"text-align:axis;\" equalrows=\"false\" columnlines=\"none none none none none none none none none none none\" equalcolumns=\"false\" class=\"array\">\n" +
                    "                                                    <mtr>\n" +
                    "                                                        <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>6</mn>\n" +
                    "                                                            <mn>5</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>1</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <mn>2</mn>\n" +
                    "                                                            <mn>8</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>2</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <mn>0</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>3</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>2</mn>\n" +
                    "                                                            <mn>6</mn>\n" +
                    "                                                            <mn>2</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>4</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>4</mn>\n" +
                    "                                                            <mn>0</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>5</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                        </mtd>\n" +
                    "                                                    </mtr>\n" +
                    "                                                    <mtr>\n" +
                    "                                                        <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                            <mn>3</mn>\n" +
                    "                                                            <mn>6</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>1</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>7</mn>\n" +
                    "                                                            <mn>3</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>2</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>3</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <mn>5</mn>\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>4</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <mn>6</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>5</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                        </mtd>\n" +
                    "                                                    </mtr>\n" +
                    "                                                    <mtr>\n" +
                    "                                                        <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>4</mn>\n" +
                    "                                                            <mn>4</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>1</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>8</mn>\n" +
                    "                                                            <mn>8</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>2</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>5</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>3</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <mn>8</mn>\n" +
                    "                                                            <mn>0</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>4</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>2</mn>\n" +
                    "                                                            <mn>4</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>5</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                        </mtd>\n" +
                    "                                                    </mtr>\n" +
                    "                                                    <mtr>\n" +
                    "                                                        <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                            <mn>3</mn>\n" +
                    "                                                            <mn>4</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>1</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>6</mn>\n" +
                    "                                                            <mn>8</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>2</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>3</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>3</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <mn>4</mn>\n" +
                    "                                                            <mn>0</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>4</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <mn>8</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>5</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                        </mtd>\n" +
                    "                                                    </mtr>\n" +
                    "                                                    <mtr>\n" +
                    "                                                        <mtd class=\"array\" columnalign=\"center\">\n" +
                    "                                                            <mn>1</mn>\n" +
                    "                                                            <mn>2</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>1</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>2</mn>\n" +
                    "                                                            <mn>4</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>2</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>3</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">+</mo>\n" +
                    "                                                            <mn>4</mn>\n" +
                    "                                                            <mn>9</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>4</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                            <mo class=\"MathClass-bin\">−</mo>\n" +
                    "                                                            <mn>5</mn>\n" +
                    "                                                            <msub>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mi>x</mi>\n" +
                    "                                                                </mrow>\n" +
                    "                                                                <mrow>\n" +
                    "                                                                    <mn>5</mn>\n" +
                    "                                                                </mrow>\n" +
                    "                                                            </msub>\n" +
                    "                                                        </mtd>\n" +
                    "                                                    </mtr>\n" +
                    "                                                </mtable>\n" +
                    "                                            </mrow>\n" +
                    "                                        </mfenced>\n" +
                    "                                    </math>" +
                    "</body>\n" +
                    "    </html>";


            final WebView web = WebViewUtils.web;

            class WebAppInterface {
                Context mContext;

                /** Instantiate the interface and set the context */
                WebAppInterface(Context c) {
                    mContext = c;
                }

                /** Show a toast from the web page */
                @JavascriptInterface
                public void showToast(String toast) {
                    Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
                }
            }
            web.addJavascriptInterface(new WebAppInterface(this), "android");


            web.setWebChromeClient(new WebChromeClient() {


                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    LOG.d("WebView", consoleMessage.message());
                    return true;
                }
            });

            LOG.d("mathml",mathml);
            web.loadData(mathml, "text/html", "utf-8");


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
