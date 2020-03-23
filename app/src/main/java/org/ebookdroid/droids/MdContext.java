package org.ebookdroid.droids;

import android.text.TextUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.FooterNote;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.model.BookCSS;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class MdContext extends PdfContext {

    public static final String OUT_FB2_XML = "md.html";
    static String regTEXT = "([\\w\\s/ \\ -.#:&;]+)";

    public static FooterNote extractMd(String inputPath, final String outputDir) throws IOException {

        File file = new File(outputDir, OUT_FB2_XML);
        LOG.d("MdContext", inputPath);
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));
            StringBuilder html = new StringBuilder();


            HypenUtils.resetTokenizer();
            if (BookCSS.get().isAutoHypens) {
                HypenUtils.applyLanguage(AppSP.get().hypenLang);
            }


            String l;
            String line = "";
            while ((l = input.readLine()) != null) {


                if (!l.trim().isEmpty()) {
                    l = TextUtils.htmlEncode(l);
                    l = applyPre(l);
                    line = line + l + " ";
                    continue;
                }


                if (BookCSS.get().isAutoHypens) {
                    line = HypenUtils.applyHypnes(line);
                }

                html.append("<p>");
                html.append(applyRegexp(line));
                html.append("</p>");
                html.append("\n");
                line = "";
            }

            html.append(applyRegexp(line));

            input.close();

            FileOutputStream out = new FileOutputStream(file);


            String line2 = "<html><head></head><body style='text-align:justify;'><br/>" + html.toString() + "</body></html>";

            out.write(line2.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return new FooterNote(file.getPath(), null);
    }


    public static String ALL = "(.+)";
    public static String all(String ignore) {return "([^"+ignore+"]+)"; }

    public static String applyPre(String line){
        if (line.startsWith("- ")) line = "<small>" + line + "</small><br/>";
        if (line.startsWith(" - ")) line = "<small>" + line + "</small><br/>";

        if (line.startsWith("* ")) line = "<small>" + line + "</small><br/>";
        if (line.startsWith(" * ")) line = "<small>" + line + "</small><br/>";

        if (line.startsWith("+ ")) line = "<small>" + line + "</small><br/>";
        if (line.startsWith(" + ")) line = "<small>" + line + "</small><br/>";

        if (line.startsWith("    ")) line = "<blockquote>" + line + "</blockquote>";
        if (line.startsWith("&gt; ")) line = "<blockquote>" + line + "</blockquote>";
        if (line.startsWith("&gt;&gt; ")) line = "<blockquote>" + line + "</blockquote>";
        return  line;
    }

    public static String applyRegexp(String line) {
        LOG.d("md-before", line);
        line = line.replaceAll("\\*\\*" + all("\\*\\*") + "\\*\\*", "<b>$1</b>");//bold
        line = line.replaceAll("__" + all("__") + "__", "<b>$1</b>");//bold

        line = line.replaceAll("\\*" + all("\\*") + "\\*", "<em>$1</em>");//italic
        line = line.replaceAll("_" + all("_") + "_", "<em>$1</em>");//italic


        line = line.replaceAll("\\[" + all("\\]") + "\\]\\(" +  all("\\)") + "\\)", "<a href='$2'>$1</a>");//url
        //line = line.replaceAll("&lt;" + ALL + "&gt;", "<a href='$1'>$1</a>");//url


        if (line.startsWith("###")) line = "<h3>" + line.replace("#","") + "</h3>";
        if (line.startsWith("##")) line = "<h2>" + line.replace("#","") + "</h2>";
        if (line.startsWith("#")) line = "<h1>" + line.replace("#","") + "</h1>";

        if(line.endsWith("---- ")){//-------
            line = "<h2>" + line.replace("-","") + "</h2>";
        }
        if(line.endsWith("==== ")){//-------
            line = "<h1>" + line.replace("=","") + "</h1>";
        }

        LOG.d("md-after", line);


        return line;
    }


    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        Map<String, String> notes = null;
        try {
            FooterNote extract = extractMd(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath());
            fileName = extract.path;
            notes = extract.notes;
            LOG.d("new file name", fileName);
        } catch (Exception e) {
            LOG.e(e);
        }

        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);
        muPdfDocument.setFootNotes(notes);
        return muPdfDocument;
    }
}
