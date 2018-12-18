package at.stefl.opendocument.java.translator.image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import at.stefl.svm.tosvg.SVGTranslator;

public class SVM2SVGConverter implements ImageConverter {
    
    // TODO: out-source
    public static final String FROM_MIMETYPE = "application/x-openoffice-gdimetafile;windows_formatname=\"GDIMetaFile\"";
    public static final String TO_MIMETYPE = "image/svg+xml";
    
    @Override
    public String getFromMimetype() {
        return FROM_MIMETYPE;
    }
    
    @Override
    public String getToMimetype() {
        return TO_MIMETYPE;
    }
    
    @Override
    public void convert(InputStream in, OutputStream out) throws IOException {
        SVGTranslator.TRANSLATOR.translate(in, out);
    }
    
    @Override
    public String convertName(String from) {
        from = new File(from).getName();
        if (from.endsWith(".svm")) from = from.substring(0, from.length()
                - ".svm".length());
        return from + ".svg";
    }
    
}