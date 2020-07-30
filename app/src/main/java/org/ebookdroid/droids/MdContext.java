package org.ebookdroid.droids;

import android.text.TextUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.ext.FooterNote;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.model.BookCSS;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

public class MdContext extends PdfContext {

    public static final String OUT_FB2_XML = "md.html";
    public static final String SUMMARY_MD = "SUMMARY.md";
    public static String ALL = "(.+)";
    static String regTEXT = "([\\w\\s/ \\ -.#:&;]+)";

    private static void process(ZipOutputStream zos, File root, String prefix) throws IOException {
        LOG.d("MdContext process dir", root);
        for (File it : root.listFiles()) {
            if (it.isDirectory()) {
                if (it.getName().startsWith(".")) {
                    continue;
                }
                process(zos, it, prefix);
                //Fb2Extractor.writeToZipDir(zos, it.getName());
                continue;
            }
            String path = root.getPath().replace(prefix, "");
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            String dirName = "OEBPS" + path + "/";
            dirName = dirName.replace("//", "/");

            if (it.getName().endsWith(".md")) {
                String name = dirName + it.getName().replace(".md", ".html");
                Fb2Extractor.writeToZip(zos, name, convertMdToHtml(it.getPath()));
                LOG.d("MdContext process file 1", name);
            } else {
                String name = dirName + it.getName();
                Fb2Extractor.writeToZip(zos, name, new FileInputStream(it));
                LOG.d("MdContext process file 2", name);
            }


        }
    }

    public static FooterNote extractMd(String inputPath, final String outputDir) throws IOException {

        LOG.d("extractMd", inputPath, outputDir);
        if (inputPath.endsWith(SUMMARY_MD)) {
            LOG.d("extractMd SUMMARY.md");
            File file = new File(outputDir, "md.epub");
            try {
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
                zos.setLevel(0);

                Fb2Extractor.writeToZip(zos, "mimetype", "application/epub+zip");
                Fb2Extractor.writeToZip(zos, "META-INF/container.xml", Fb2Extractor.container_xml);


                File root = new File(inputPath).getParentFile();
                BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(new File(root.getPath(), SUMMARY_MD))));

                String l;
                List<OutlineLink> titles = new ArrayList<>();
                while ((l = input.readLine()) != null) {
                    int i = l.indexOf("*");
                    if (i >= 0) {
                        //* [Introduction](README.md)
                        try {
                            String title = l.substring(l.indexOf("[") + 1, l.indexOf("]"));
                            String uri = l.substring(l.indexOf("(") + 1, l.indexOf(")"));
                            OutlineLink link = new OutlineLink(title, uri, i, 0, uri);
                            link.contentSrc = uri.replace(".md", ".html");
                            titles.add(link);
                        } catch (Exception e) {
                            //OutlineLink link = new OutlineLink(l, "", i, 0, "");
                            //titles.add(link);
                        }
                    }
                }

                Fb2Extractor.writeToZip(zos, "OEBPS/fb2.ncx", Fb2Extractor.genetateNCXbyOutlineMd(titles));

                StringBuilder opf = new StringBuilder();
                opf.append("<?xml version=\"1.0\"  encoding=\"UTF-8\"?>");
                opf.append("<package version=\"2.0\" unique-identifier=\"uid\" xmlns=\"http://www.idpf.org/2007/opf\">");
                opf.append("<metadata xmlns:opf=\"http://www.idpf.org/2007/opf\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
                opf.append("<dc:title>%title%</dc:title>");
                opf.append("<dc:creator>%creator%</dc:creator>");
                opf.append("<meta name=\"cover\" content=\"cover.jpg\" />");
                opf.append("</metadata>");


                opf.append("<manifest>");
                opf.append("<item id=\"idResourceFb2\" href=\"fb2.ncx\" media-type=\"application/x-dtbncx+xml\"/>");
                for (OutlineLink link : titles) {
                    opf.append(" <item href=\"" + link.contentSrc + "\" id=\"" + link.contentSrc.hashCode() + "\" media-type=\"application/xhtml+xml\"/>");
                }

                opf.append("</manifest>");

                opf.append("<spine toc=\"idResourceFb2\">");
                for (OutlineLink link : titles) {
                    opf.append("<itemref idref=\"" + link.contentSrc.hashCode() + "\"/>");
                }

                opf.append("</spine>");
                opf.append("</package>");


                Fb2Extractor.writeToZip(zos, "OEBPS/content.opf", opf.toString().replace(">", ">\n"));


                process(zos, root, root.getPath());


                LOG.d("Fb2Context convert true");
                zos.close();

                return new FooterNote(file.getPath(), null);
            } catch (Exception e) {
                LOG.d("Fb2Context convert false error");
                LOG.e(e);
                return new FooterNote(null, null);
            }

        }

        File file = new File(outputDir, OUT_FB2_XML);
        LOG.d("MdContext", inputPath);
        try {
            String line2 = convertMdToHtml(inputPath);


            FileOutputStream out = new FileOutputStream(file);
            out.write(line2.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return new FooterNote(file.getPath(), null);
    }


    private static String convertMdToHtml(String inputPath) throws IOException {
        try {
            LOG.d("convertMdToHtml", inputPath);
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


            return "<html><head></head><body style='text-align:justify;'><br/>" + html.toString() + "</body></html>";
        } catch (Exception e) {
            LOG.e(e);
            return "<html><head></head><body style='text-align:justify;'><br/>" + e.toString() + "</body></html>";
        }
    }

    public static String all(String ignore) {
        return "([^" + ignore + "]+)";
    }

    public static String applyPre(String line) {
        if (line.startsWith("- ")) line = "<small>" + line + "</small><br/>";
        if (line.startsWith(" - ")) line = "<small>" + line + "</small><br/>";

        if (line.startsWith("* ")) line = "<small>" + line + "</small><br/>";
        if (line.startsWith(" * ")) line = "<small>" + line + "</small><br/>";

        if (line.startsWith("+ ")) line = "<small>" + line + "</small><br/>";
        if (line.startsWith(" + ")) line = "<small>" + line + "</small><br/>";

        if (line.startsWith("    ")) line = "<blockquote>" + line + "</blockquote>";
        if (line.startsWith("&gt; ")) line = "<blockquote>" + line + "</blockquote>";
        if (line.startsWith("&gt;&gt; ")) line = "<blockquote>" + line + "</blockquote>";
        return line;
    }

    public static String applyRegexp(String line) {
        LOG.d("md-before", line);
        line = line.replaceAll("\\*\\*" + all("\\*\\*") + "\\*\\*", "<b>$1</b>");//bold
        line = line.replaceAll("__" + all("__") + "__", "<b>$1</b>");//bold

        line = line.replaceAll("\\*" + all("\\*") + "\\*", "<em>$1</em>");//italic
        line = line.replaceAll("_" + all("_") + "_", "<em>$1</em>");//italic


        line = line.replaceAll("\\!\\[" + all("\\]") + "\\]\\(" + all("\\)") + "\\)", "<img src='$2' alt='$1'/>");//img
        line = line.replaceAll("\\[" + all("\\]") + "\\]\\(" + all("\\)") + "\\)", "<a href='$2'>$1</a>");//url
        //line = line.replaceAll("&lt;" + ALL + "&gt;", "<a href='$1'>$1</a>");//url


        if (line.startsWith("###")) line = "<h3>" + line.replace("#", "") + "</h3>";
        if (line.startsWith("##")) line = "<h2>" + line.replace("#", "") + "</h2>";
        if (line.startsWith("#")) line = "<h1>" + line.replace("#", "") + "</h1>";

        if (line.endsWith("---- ")) {//-------
            line = "<h2>" + line.replace("-", "") + "</h2>";
        }
        if (line.endsWith("==== ")) {//-------
            line = "<h1>" + line.replace("=", "") + "</h1>";
        }

        line = line.replace("'./", "'");
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
