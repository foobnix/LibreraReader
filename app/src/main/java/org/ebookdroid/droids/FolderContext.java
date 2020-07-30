package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.XmlParser;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;

import org.ebookdroid.BookType;
import org.ebookdroid.LibreraApp;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class FolderContext extends PdfContext {

    public static String LXML = "ldir";

    public static File genarateXML(List<FileMeta> items, String base, boolean write) {
        List<FileMeta> res = new ArrayList<FileMeta>();
        for (FileMeta meta : items) {
            if (ExtUtils.isImagePath(meta.getPath()) || BookType.TIFF.is(meta.getPath())) {
                res.add(meta);
            }
        }

        File file;
        if (AppsConfig.MUPDF_1_11 == LibreraApp.MUPDF_VERSION) {
            file = new File(CacheZipUtils.ATTACHMENTS_CACHE_DIR, new File(base).getName());
            file.mkdirs();
            file = new File(file, "book.ldir");
        } else {
            file = new File(CacheZipUtils.ATTACHMENTS_CACHE_DIR, new File(base).getName() + "." + LXML);
        }

        if (write) {
            file.delete();

            try {
                LOG.d("genarateXML", file.getPath());
                PrintWriter pr = new PrintWriter(file);
                pr.println("<container count=\"" + res.size() + "\">");
                for (FileMeta meta : res) {
                    // String x = "<item path=\"" + ExtUtils.getFileName(meta.getPath()) + "\" />";
                    String x = "<item  path=\"" + meta.getPath() + "\" />";
                    pr.println(x);
                    LOG.d("genarateXML", x);
                }
                pr.println("</container>");
                pr.close();
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        return file;

    }

    public static boolean isFolderWithImage(List<FileMeta> items) {
        for (FileMeta meta : items) {
            if (meta == null || meta.getPath() == null) {
                continue;
            }
            String path = meta.getPath();

            if (new File(path).isFile()) {
                if (ExtUtils.isImagePath(path) || BookType.TIFF.is(path)) {
                    return true;
                }
            }
        }
        return false;

    }


    public static int getPageCount(String unZipPath) {
        try {
            final FileInputStream inputStream = new FileInputStream(unZipPath);
            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(inputStream, "utf-8");

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("container".equals(xpp.getName())) {
                        inputStream.close();
                        return Integer.parseInt(xpp.getAttributeValue(null, "count"));
                    }
                }
                eventType = xpp.next();
            }
            inputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return 0;

    }

    public static byte[] getBookCover(String unZipPath) {
        try {
            XmlPullParser xpp = XmlParser.buildPullParser();
            final FileInputStream inputStream = new FileInputStream(unZipPath);
            xpp.setInput(inputStream, "utf-8");

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("item".equals(xpp.getName())) {
                        File list = new File(unZipPath);
                        File parentFile = list.getParentFile();
                        String name = xpp.getAttributeValue(null, "path");
                        File file = null;
                        if (name.startsWith("/") || name.startsWith("file://")) {
                            file = new File(name);
                        } else {
                            file = new File(parentFile, name);
                        }
                        LOG.d("getBookCover-file", file);
                        inputStream.close();
                        return CacheZipUtils.getEntryAsByte(new FileInputStream(file));
                    }
                }
                eventType = xpp.next();
            }
            inputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return null;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);
        return muPdfDocument;
    }

}
