package at.stefl.opendocument.java.translator.content;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import at.stefl.commons.codec.Base64OutputStream;
import at.stefl.commons.codec.Base64Settings;
import at.stefl.commons.io.ByteStreamUtil;
import at.stefl.commons.io.CloseableWriter;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.translator.context.TranslationContext;
import at.stefl.opendocument.java.translator.image.ImageConverter;
import at.stefl.opendocument.java.translator.image.SVM2SVGConverter;
import at.stefl.opendocument.java.util.FileCache;

// TODO: skip empty images
public class ImageTranslator extends
        DefaultElementTranslator<TranslationContext> {
    
    private static final String PATH_ATTRIBUTE_NAME = "xlink:href";
    
    private static final String ALT_FAILED = "Image not found or unsupported: ";
    
    private final ByteStreamUtil streamUtil = new ByteStreamUtil();
    
    // TODO: use mimetype class
    private Set<String> supportedMimetypes = new HashSet<String>();
    private Map<String, ImageConverter> imageConverterMap = new HashMap<String, ImageConverter>();
    
    public ImageTranslator() {
        super("img");
        
        // TODO: out-source
        addSupportedMimetype("image/gif");
        addSupportedMimetype("image/jpeg");
        addSupportedMimetype("image/png");
        addSupportedMimetype("image/svg+xml");
        
        addImageConverter(new SVM2SVGConverter());
    }
    
    public boolean isMimetypeSupported(String mimetype) {
        return supportedMimetypes.contains(mimetype);
    }
    
    public void addSupportedMimetype(String mimetype) {
        supportedMimetypes.add(mimetype);
    }
    
    public void addImageConverter(ImageConverter converter) {
        if (!isMimetypeSupported(converter.getToMimetype())) throw new IllegalArgumentException(
                "destination mimetype not supported");
        
        imageConverterMap.put(converter.getFromMimetype(), converter);
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        String path = LWXMLUtil.parseSingleAttribute(in, PATH_ATTRIBUTE_NAME);
        // TODO: log

        if (path == null) return;
        // TODO: move to OpenDocumentFile and improve
        path = path.replaceAll("\\./", "");
        

        // out.writeAttribute("style", "width: 100%; heigth: 100%");
        // out.writeAttribute("alt", ALT_FAILED + path);
        out.writeAttribute("src", "");
        if (context.getDocumentFile().isFile(path)) {
            writeSource(path, out, context);
        } else {
            out.write(path);
        }
    }
    
    @Override
    public void translateChildren(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        LWXMLUtil.flushBranch(in);
        
        out.writeEndEmptyElement();
    }
    
    @Override
    public void translateEndElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        // TODO: log
    }
    
    // TODO: check supported mimetypes?
    private void writeSource(String path, Writer out, TranslationContext context)
            throws IOException {
        OutputStream imageOut;
        
        String mimetype = context.getDocumentFile().getFileMimetype(path);
        String name;
        ImageConverter converter = imageConverterMap.get(mimetype);
        if (converter == null) {
            name = new File(path).getName();
        } else {
            mimetype = converter.getToMimetype();
            name = converter.convertName(path);
        }
        
        switch (context.getSettings().getImageStoreMode()) {
        case CACHE:
            imageOut = writeSourceCached(name, out, context);
            break;
        case INLINE:
            imageOut = writeSourceInline(mimetype, out);
            break;
        default:
            throw new UnsupportedOperationException();
        }
        
        if (imageOut == null) return;
        InputStream imageIn = context.getDocumentFile().getFileStream(path);
        
        try {
            if (converter == null) streamUtil.writeStream(imageIn, imageOut);
            else converter.convert(imageIn, imageOut);
        } catch (Exception e) {
//            if (e instanceof IOException) throw (IOException) e;
            // TODO: log conversion fail
        } finally {
            imageIn.close();
            imageOut.close();
        }
    }
    
    private OutputStream writeSourceCached(String name, Writer out,
            TranslationContext context) throws IOException {
        OutputStream result = null;
        
        FileCache cache = context.getSettings().getCache();
        if (!cache.exists(name)) result = new FileOutputStream(
                cache.create(name));
        
        out.write(name);
        
        return result;
    }
    
    private OutputStream writeSourceInline(String mimetype, Writer out)
            throws IOException {
        if (mimetype == null) {
            // TODO: log
            // TODO: "null" ?
            out.write("null");
            return null;
        }
        
        out.write("data:");
        out.write(mimetype);
        out.write(";base64,");
        
        return new Base64OutputStream(new CloseableWriter(out),
                Base64Settings.ORIGINAL);
    }
    
}