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

import com.foobnix.android.utils.LOG;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.BookCSS;

public class HtmlExtractor {

    public static final String OUT_FB2_XML = "temp.html";

    public static FooterNote extract(String inputPath, final String outputDir) throws IOException {
        // File file = new File(new File(inputPath).getParent(), OUT_FB2_XML);
        File file = new File(outputDir, OUT_FB2_XML);

        try {

            String encoding = ExtUtils.determineEncoding(new FileInputStream(inputPath));

            LOG.d("HtmlExtractor encoding: ", encoding);
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), encoding));

            StringBuilder html = new StringBuilder();
            String line;

            if (BookCSS.get().isAutoHypens) {
                HypenUtils.applyLanguage(BookCSS.get().hypenLang);
            }

            boolean isBody = false;
            while ((line = input.readLine()) != null) {

                LOG.d(line);

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
            input.close();

            FileOutputStream out = new FileOutputStream(file);


            String string = Jsoup.clean(html.toString(), Whitelist.relaxed().removeTags("img"));


            if (BookCSS.get().isAutoHypens) {
                string = HypenUtils.applyHypnes(string);
                string = Jsoup.clean(string, Whitelist.relaxed());
            }
            // String string = html.toString();
            string = "<html><head></head><body style='text-align:justify;'><br/>" + string + "</body></html>";
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

}
