package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
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

    public static FooterNote extractMd(String inputPath, final String outputDir) throws IOException {

        File file = new File(outputDir, OUT_FB2_XML);
        LOG.d("MdContext", inputPath);
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath)));
            StringBuilder html = new StringBuilder();
            String line;

            HypenUtils.resetTokenizer();
            if (BookCSS.get().isAutoHypens) {
                HypenUtils.applyLanguage(AppSP.get().hypenLang);
            }
            boolean isBold;
            boolean isList;
            boolean isBlock;
            while ((line = input.readLine()) != null) {
                isBold = false;
                isList = false;
                isBlock = false;
                if (line.trim().isEmpty()) {
                    html.append("<empty-line/>");
                    continue;
                }

                if (line.startsWith("#") || line.startsWith("**")) {
                    isBold = true;
                }
                if (line.startsWith("* ")) {
                    line = line.replace("* ", "*" + TxtUtils.NON_BREAKE_SPACE);
                    isList = true;
                }
                if (line.startsWith("> ")) {
                    line = line.replace("> ", ">" + TxtUtils.NON_BREAKE_SPACE);

                    isBlock = true;
                }

                if (BookCSS.get().isAutoHypens) {
                    line = HypenUtils.applyHypnes(line);
                }

                html.append("<p>");

                if (isBold) {
                    html.append("<b>");
                }
                if (isList) {
                    html.append("<small>");
                }

                if (isBlock) {
                    html.append("<blockquote>");
                }

                html.append(line);


                if (isBlock) {
                    html.append("</blockquote>");
                }

                if (isList) {
                    html.append("</small>");
                }

                if (isBold) {
                    html.append("</b>");
                }

                html.append("</p>");
                html.append("\n");
            }

            input.close();

            FileOutputStream out = new FileOutputStream(file);


            line = "<html><head></head><body style='text-align:justify;'><br/>" + html.toString() + "</body></html>";

            out.write(line.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return new FooterNote(file.getPath(), null);
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
