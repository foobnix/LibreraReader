package com.foobnix.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.AppState;

public class HtmlExtractor {

    public static final String OUT_FB2_XML = "temp.html";

    public static FooterNote extract(String inputPath, final String outputDir) throws IOException {
        // File file = new File(new File(inputPath).getParent(), OUT_FB2_XML);
        File file = new File(outputDir, OUT_FB2_XML);

        try {

            String encoding = ExtUtils.determineHtmlEncoding(new FileInputStream(inputPath), new FileInputStream(inputPath));

            LOG.d("HtmlExtractor encoding: ", encoding, "");
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), encoding));

            StringBuilder html = new StringBuilder();
            String line;

            boolean accurate = !LOG.isEnable || AppState.get().isAccurateFontSize;
            if (accurate) {
                boolean isBody = false;
                while ((line = input.readLine()) != null) {

                    if (line.toLowerCase(Locale.US).contains("<body")) {
                        isBody = true;
                    }
                    if (isBody) {
                        html.append(line);
                    }
                    if (line.toLowerCase(Locale.US).contains("</html>")) {
                        break;
                    }
                }
            } else {
                while ((line = input.readLine()) != null) {
                    html.append(line);
                }
            }
            input.close();

            FileOutputStream out = new FileOutputStream(file);

            String string = null;
            if (accurate) {

                string = Jsoup.clean(html.toString(), Whitelist.basic());
            } else {
                string = html.toString();
            }

            if (BookCSS.get().isAutoHypens) {
                HypenUtils.applyLanguage(BookCSS.get().hypenLang);
                string = HypenUtils.applyHypnes(string);
                // string = Jsoup.clean(string, Whitelist.none());
            }
            // String string = html.toString();
            if (accurate) {
                string = "<html><head></head><body style='text-align:justify;'><br/>" + string + "</body></html>";
            }
            // string = string.replace("\">", "\"/>");
            string = string.replace("<br>", "<br/>");
            // string = string.replace("http://example.com/", "");

            out.write(string.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return new FooterNote(file.getPath(), null);
    }

    public static FooterNote extractMht(String inputPath, final String outputDir) throws IOException {
        // File file = new File(new File(inputPath).getParent(), OUT_FB2_XML);
        File file = new File(outputDir, OUT_FB2_XML);

        try {

            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));

            StringBuilder html = new StringBuilder();
            String line;

            boolean isFlag = false;
            while ((line = input.readLine()) != null) {

                if (line.contains("<ht") || line.contains("<HT")) {
                    isFlag = true;
                }

                if (isFlag) {
                    if (line.endsWith("=")) {
                        line = line.substring(0, line.length() - 1);
                    } else {
                        line = line + " ";
                    }

                    line = line.replace("<br>", "<br/>").replace("=20", " ").replace("=09", "<br/>");
                    html.append(line);
                }

                if (line.contains("</ht") || line.contains("</HT")) {
                    isFlag = false;
                    html.append("<br/>");
                }
            }
            input.close();

            FileOutputStream out = new FileOutputStream(file);

            String string = Jsoup.clean(html.toString(), Whitelist.basic());

            if (BookCSS.get().isAutoHypens) {
                HypenUtils.applyLanguage(BookCSS.get().hypenLang);
                string = HypenUtils.applyHypnes(string);
            }

            string = "<html><head></head><body style='text-align:justify;'><br/>" + string + "</body></html>";

            out.write(string.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return new FooterNote(file.getPath(), null);
    }

}
