package com.foobnix.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.AppState;

import android.text.TextUtils;

public class TxtExtract {

    public static final String OUT_FB2_XML = "txt.html";

    static char[] endChars = new char[] { '.', '!', '?', ';' };

    public static String foramtUB(String line) {
        if (line != null && line.trim().startsWith("(*)") && TxtUtils.isLastCharEq(line, endChars)) {
            line = "<b><u>" + line + "</u></b>";
        }
        return line;
    }

    public static FooterNote extract(String inputPath, String outputDir) throws IOException {
        File file = new File(outputDir, AppState.get().isPreText + OUT_FB2_XML);

        String encoding = ExtUtils.determineTxtEncoding(new FileInputStream(inputPath));

        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), encoding));
        PrintWriter writer = new PrintWriter(file);
        String line;

        writer.println("<!DOCTYPE html>");
        writer.println("<html>");
        if (AppState.get().isPreText) {
            writer.println("<head><style>@page{margin:0px 0.5em} pre{margin:0px} {body:margin:0px;}</style></head>");
        } else {
            writer.println("<head><style>p,p+p{margin:0;}</style></head>");
        }
        writer.println("<body>");

        if (AppState.get().isPreText) {
            writer.println("<pre>");
        }

        if (AppState.get().isLineBreaksText) {
            writer.println("<p>");
        }

        if (BookCSS.get().isAutoHypens) {
            HypenUtils.applyLanguage(BookCSS.get().hypenLang);
        }

        while ((line = input.readLine()) != null) {
            String outLn = null;
            if (AppState.get().isPreText) {

                outLn = retab(line, 8);
                outLn = TextUtils.htmlEncode(outLn);

                if (TxtUtils.isLineStartEndUpperCase(outLn)) {
                    outLn = "<b>" + outLn + "</b>";
                }

            } else {

                if (AppState.get().isLineBreaksText) {
                    if (line.trim().length() == 0) {
                        outLn = "<br/>";
                    } else {
                        outLn = format(line);
                    }

                } else {
                    if (line.trim().length() == 0) {
                        outLn = "<br/>";
                    } else if (TxtUtils.isLineStartEndUpperCase(line)) {
                        outLn = "<b>" + format(line) + "</b>";
                    } else if (line.contains("Title:")) {
                        outLn = "<b>" + format(line) + "</b>";
                    } else {
                        outLn = "<p>" + format(line) + "</p>";
                    }
                }

            }
            // LOG.d("LINE", outLn);
            writer.println(outLn);
        }
        if (AppState.get().isLineBreaksText) {
            writer.println("</p>");
        }

        if (AppState.get().isPreText)

        {
            writer.println("</pre>");
        }
        writer.println("</body></html>");

        input.close();
        writer.close();

        return new FooterNote(file.getPath(), null);
    }

    public static String retab(final String text, final int tabstop) {
        final char[] input = text.toCharArray();
        final StringBuilder sb = new StringBuilder();

        int linepos = 0;
        for (int i = 0; i < input.length; i++) {
            // treat the character
            final char ch = input[i];
            if (ch == '\t') {
                // expand the tab
                do {
                    sb.append(' ');
                    linepos++;
                } while (linepos % tabstop != 0);
            } else {
                sb.append(ch);
                linepos++;
            }

            // end of line. Reset the lineposition to zero.
            // if (ch == '\n' || ch == '\r' || (ch | 1) == '\u2029' || ch ==
            // '\u0085')
            // linepos = 0;

        }

        return sb.toString();
    }

    public static String format(String line) {
        try {
            line = line.replace("\n", "");
            line = line.replace("\r", "");
            line = TextUtils.htmlEncode(line);
            if (BookCSS.get().isAutoHypens) {
                line = HypenUtils.applyHypnes(line);
            }
            line = line.trim();

        } catch (Exception e) {
            LOG.e(e);
        }
        return line;
    }

}
