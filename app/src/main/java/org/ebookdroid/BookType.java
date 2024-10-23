package org.ebookdroid;

import android.content.Intent;

import com.foobnix.OpenerActivity;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.droids.CbrContext;
import org.ebookdroid.droids.DocContext;
import org.ebookdroid.droids.DocxContext;
import org.ebookdroid.droids.EpubContext;
import org.ebookdroid.droids.Fb2Context;
import org.ebookdroid.droids.FolderContext;
import org.ebookdroid.droids.HtmlContext;
import org.ebookdroid.droids.MdContext;
import org.ebookdroid.droids.MhtContext;
import org.ebookdroid.droids.MobiContext;
import org.ebookdroid.droids.OdtContext;
import org.ebookdroid.droids.RtfContext;
import org.ebookdroid.droids.TxtContext;
import org.ebookdroid.droids.ZipContext;
import org.ebookdroid.droids.djvu.codec.DjvuContext;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum BookType {

    PDF(PdfContext.class, false, false, list("pdf", "xps"), list("application/pdf","application/oxps","application/vnd.ms-xpsdocument")),
    TIFF(PdfContext.class, false, false, list("tiff", "tif"), list("image/tiff")),

    CBZ(PdfContext.class, false, true, list("cbz"), list("application/x-cbz","application/comicbook+zip")),
    CBR(CbrContext.class, false, true, list("cbr"), list("application/x-cbr","application/comicbook+rar")),

    ODT(OdtContext.class, true, true, list("odt"), list("application/vnd.oasis.opendocument.text")),

    FOLDER(FolderContext.class, false, false,list(FolderContext.LXML), list("application/lxml")),

    EPUB(EpubContext.class, true, true, list("epub"), list("application/epub+zip")),


    FB2(Fb2Context.class, true, true, list("fb2","fbd"),
            list("application/fb2", "application/x-fictionbook", "application/x-fictionbook+xml", "application/x-fb2", "application/fb2+zip", "application/fb2.zip", "application/x-zip-compressed-fb2")),

    MOBI(MobiContext.class, true, true, list("mobi", "azw", "azw3", "azw4", "pdb", "prc"), list("application/x-mobipocket-ebook", "application/x-palm-database","application/x-mobi8-ebook","application/x-kindle-application","application/vnd.amazon.mobi8-ebook")),

    TXT(TxtContext.class, true, true, list("txt", "playlist","log","ini"), list("text/plain","text/x-log")),

    JSON(TxtContext.class, true, true, list("json"), list("application/json")),

    HTML(HtmlContext.class, true, true, list("html", "htm", "xhtml", "xhtm", "xml"), list("text/html", "text/xml")),

    MHT(MhtContext.class, true, true, list("mht", "mhtml","xhtml","shtml"), list("message/rfc822")),

    DOCX(DocxContext.class, true, true, list(AppsConfig.isDOCXSupported ? "docx" : "xxx"), list("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),

    DOC(DocContext.class, true, true, list("doc"), list("application/msword")),

    MD(MdContext.class, true, true, list("md"), list("text/markdown","text/x-markdown")),

    RTF(RtfContext.class, true, true, list("rtf"), list("application/rtf", "application/x-rtf", "text/rtf", "text/richtext")),

    DJVU(DjvuContext.class, false, false, list("djvu"), list("image/vnd.djvu", "image/djvu", "image/x-djvu")),

    ZIP(ZipContext.class, true, true, list("zip"), list("application/zip", "application/x-compressed", "application/x-compressed-zip", "application/x-zip-compressed")),

    OKULAR(ZipContext.class, false, false, list("okular"), list("application/zip", "application/x-compressed", "application/x-compressed-zip", "application/x-zip-compressed")),

    NULL(PdfContext .class, false, false,list(""), list(""));

    private final static Map<String, BookType> extensionToActivity;

    private final static Map<String, BookType> mimeTypesToActivity;

    static {
        extensionToActivity = new HashMap<String, BookType>();
        for (final BookType a : values()) {
            for (final String ext : a.extensions) {
                extensionToActivity.put(ext.toLowerCase(Locale.US), a);
            }
        }
        mimeTypesToActivity = new HashMap<String, BookType>();
        for (final BookType a : values()) {
            for (final String type : a.mimeTypes) {
                mimeTypesToActivity.put(type.toLowerCase(Locale.US), a);
            }
        }
    }
    public static List<String> list(String ... items){
        return Arrays.asList(items);
    }

    private final Class<? extends CodecContext> contextClass;

    private final List<String> extensions;

    private final List<String> mimeTypes;
    private boolean isTextFormat;
    private boolean hasTitle;


    private BookType(final Class<? extends CodecContext> contextClass, boolean isTextFormat, boolean hasTitle, final List<String> extensions, final List<String> mimeTypes) {
        this.contextClass = contextClass;
        this.extensions = extensions;
        this.mimeTypes = mimeTypes;
        this.isTextFormat = isTextFormat;
        this.hasTitle = hasTitle;
    }

    public boolean is(String path) {
        if (path == null) {
            return false;
        }

        path = path.toLowerCase(Locale.US);
        for (final String ext : extensions) {
            if (path.endsWith(ext) || path.endsWith("." + ext + ".zip")) {
                return true;
            }
        }
        return false;
    }

    public String getExt() {
        return extensions.get(0);
    }

    public boolean is(Intent intent) {
        try {
            return is(intent.getData().getPath());
        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> getAllSupportedExtensions() {
        List<String> list = new ArrayList<String>();

        for (final BookType a : values()) {
            for (final String ext : a.extensions) {
                list.add(ext);
            }
        }
        list.remove(FolderContext.LXML);
        list.remove("");

        return list;
    }




    public static CodecContext getCodecContextByType(BookType activityType) {
        try {
            return activityType.contextClass.newInstance();
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;
    }

    public static CodecContext getCodecContextByPath(String path) {
        CodecContext ctx = null;
        try {
            ctx = getByUri(path).contextClass.newInstance();
        } catch (Exception e) {
            LOG.e(e);
        }
        return ctx;
    }

    public String getFirstMimeTime() {
        return mimeTypes.get(0);
    }

    public static boolean isSupportedExtByPath(String path) {
        if (path == null) {
            return false;
        }

        path = path.toLowerCase(Locale.US);

        if(AppsConfig.IS_FDROID && path.endsWith(".cbr")){
            return false;
        }

        for (final BookType a : values()) {
            for (final String ext : a.extensions) {
                if (TxtUtils.isEmpty(ext)) {
                    continue;
                }
                if (path.endsWith(ext)) {
                    return true;
                }

            }
        }
        return false;
    }

    public static BookType getByUri(String uri) {
        if (uri == null) {
            return null;
        }

        uri = uri.toLowerCase(Locale.US);

        for (final String ext : extensionToActivity.keySet()) {
            if (uri.endsWith("." + ext)) {
                return extensionToActivity.get(ext);
            }
        }
        return BookType.NULL;
    }

    public static BookType getByMimeType(final String type) {
        if (type == null) {
            return BookType.NULL;
        }
        return mimeTypesToActivity.get(type.toLowerCase(Locale.US));
    }

    public boolean hasTitle(){
        return hasTitle;
    }
    public boolean isTextFormat(){
        return isTextFormat;
    }

}
