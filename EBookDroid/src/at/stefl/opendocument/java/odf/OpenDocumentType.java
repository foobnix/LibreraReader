package at.stefl.opendocument.java.odf;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import at.stefl.commons.util.array.ArrayUtil;

// TODO: provide "application/vnd.oasis.opendocument" check
public enum OpenDocumentType {
    
    TEXT(new String[] { "odt", "fodt" },
            "application/vnd.oasis.opendocument.text", OpenDocumentText.class) {
        @Override
        OpenDocumentText getDocument(OpenDocumentFile documentFile) {
            return new OpenDocumentText(documentFile);
        }
    },
    SPREADSHEET(new String[] { "ods", "fods" },
            "application/vnd.oasis.opendocument.spreadsheet",
            OpenDocumentSpreadsheet.class) {
        @Override
        OpenDocumentSpreadsheet getDocument(OpenDocumentFile documentFile) {
            return new OpenDocumentSpreadsheet(documentFile);
        }
    },
    PRESENTATION(new String[] { "odp", "fodp" },
            "application/vnd.oasis.opendocument.presentation",
            OpenDocumentPresentation.class) {
        @Override
        OpenDocumentPresentation getDocument(OpenDocumentFile documentFile) {
            return new OpenDocumentPresentation(documentFile);
        }
    };
    
    private static final String MIME_TYPE_PARENT = "application/vnd.oasis.opendocument";
    
    private static final Map<Class<? extends OpenDocument>, OpenDocumentType> BY_CLASS_MAP = new HashMap<Class<? extends OpenDocument>, OpenDocumentType>();
    private static final Map<String, OpenDocumentType> BY_EXTESNION = new HashMap<String, OpenDocumentType>();
    
    static {
        for (OpenDocumentType type : values()) {
            BY_CLASS_MAP.put(type.documentClass, type);
            
            for (String extension : type.extensions) {
                if (BY_EXTESNION.put(extension, type) != null) throw new IllegalStateException(
                        "extension was overwritten!");
            }
        }
    }
    
    public static OpenDocumentType getByClass(
            Class<? extends OpenDocument> clazz) {
        return BY_CLASS_MAP.get(clazz);
    }
    
    public static OpenDocumentType getByMimeType(String mimeType) {
        if (!isOpenDocumentFile(mimeType)) throw new IllegalMimeTypeException(
                mimeType);
        
        for (OpenDocumentType type : values()) {
            if (type.validMimeType(mimeType)) return type;
        }
        
        throw new UnsupportedMimeTypeException(mimeType);
    }
    
    public static boolean isOpenDocumentFile(String mimeType) {
        return mimeType.startsWith(MIME_TYPE_PARENT);
    }
    
    public static OpenDocumentType getByExtension(String extension) {
        return BY_EXTESNION.get(extension.toLowerCase());
    }
    
    public static OpenDocument getSuitableDocument(OpenDocumentFile documentFile)
            throws IOException {
        return getByMimeType(documentFile.getMimetype()).getDocument(
                documentFile);
    }
    
    public static Set<String> getExtensions() {
        return Collections.unmodifiableSet(BY_EXTESNION.keySet());
    }
    
    public static String[] getExtensionsArray() {
        return BY_EXTESNION.keySet().toArray(new String[BY_EXTESNION.size()]);
    }
    
    private final Set<String> extensions;
    private final String mimeType;
    private final Class<? extends OpenDocument> documentClass;
    
    private OpenDocumentType(String[] extensions, String mimetype,
            Class<? extends OpenDocument> documentClass) {
        this.extensions = Collections.unmodifiableSet(ArrayUtil.toCollection(
                new LinkedHashSet<String>(extensions.length), extensions));
        this.mimeType = mimetype;
        this.documentClass = documentClass;
    }
    
    public Set<String> getExtension() {
        return extensions;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public Class<? extends OpenDocument> getDocumentClass() {
        return documentClass;
    }
    
    public boolean validMimeType(String mimeType) {
        return mimeType.startsWith(this.mimeType);
    }
    
    abstract OpenDocument getDocument(OpenDocumentFile documentFile);
    
}