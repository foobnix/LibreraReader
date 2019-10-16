package at.stefl.opendocument.java.odf;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import at.stefl.commons.io.CharStreamUtil;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.reader.LWXMLStreamReader;
import at.stefl.commons.util.array.ArrayUtil;

// TODO: rename "name" to "path" (context: file)
public abstract class OpenDocumentFile implements Closeable {
    
    private static final String MIMETYPE_PATH = "mimetype";
    private static final String MANIFEST_PATH = "META-INF/manifest.xml";
    
    private static final Set<String> UNENCRYPTED_FILES = ArrayUtil
            .toHashSet(new String[] { MIMETYPE_PATH, MANIFEST_PATH });
    
    private String mimetype;
    private Map<String, String> mimetypeMap;
    
    private Map<String, EncryptionParameter> encryptionParameterMap;
    private String password;
    
    public boolean isEncrypted() throws IOException {
        if (encryptionParameterMap == null) encryptionParameterMap = Collections
                .unmodifiableMap(EncryptionParameter
                        .parseEncryptionParameters(this));
        
        return !encryptionParameterMap.isEmpty();
    }
    
    public boolean isFileEncrypted(String path) throws IOException {
        if (UNENCRYPTED_FILES.contains(path)) return false;
        if (!isEncrypted()) return false;
        
        return encryptionParameterMap.containsKey(path);
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isPasswordValid() throws IOException {
        if (!isEncrypted()) return true;
        
        return OpenDocumentCryptoUtil.validatePassword(password, this);
    }
    
    public EncryptionParameter getEncryptionParameter(String path)
            throws IOException {
        if (!isEncrypted()) return null;
        
        return encryptionParameterMap.get(path);
    }
    
    public Map<String, EncryptionParameter> getEncryptionParameterMap()
            throws IOException {
        if (!isEncrypted()) return null;
        
        return encryptionParameterMap;
    }
    
    public abstract boolean isFile(String name) throws IOException;
    
    public abstract Set<String> getFileNames() throws IOException;
    
    protected abstract InputStream getRawFileStream(String name)
            throws IOException;
    
    public InputStream getFileStream(String name) throws IOException {
        InputStream in = getRawFileStream(name);
        if (!isFileEncrypted(name)) return in;
        
        if (password == null) throw new NullPointerException(
                "password cannot be null");
        EncryptionParameter encryptionParameter = getEncryptionParameter(name);
        in = OpenDocumentCryptoUtil.getDecryptedInputStream(in,
                encryptionParameter, password);
        in = new InflaterInputStream(in, new Inflater(true));
        return in;
    }
    
    public String getFileMimetype(String name) throws IOException {
        if (mimetypeMap == null) mimetypeMap = getFileMimetypeImpl();
        
        return mimetypeMap.get(name);
    }
    
    public abstract long getFileSize(String name);
    
    // TODO: out-source
    // TODO: use mimetype class
    private String mimetypeFromExtension(String name) {
        if (name.endsWith(".xml")) return "text/xml";
        else if (name.endsWith(".gif")) return "image/gif";
        else if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".jpe") || name.endsWith(".jif")
                || name.endsWith(".jfif") || name.endsWith(".jfi")) return "image/jpeg";
        else if (name.endsWith(".png")) return "image/png";
        else if (name.endsWith(".svg") || name.endsWith(".svgz")) return "image/svg+xml";
        else if (name.endsWith(".tiff") || name.endsWith(".tif")) return "image/tiff";
        else if (name.endsWith(".wmf") || name.endsWith(".wmz")
                || name.endsWith(".emf") || name.endsWith(".emz")) return "image/x-wmf";
        // TODO: improve
        else if (name.contains("ObjectReplacement")) return "application/x-openoffice-gdimetafile;windows_formatname=\"GDIMetaFile\"";
        else return null;
    }
    
    private Map<String, String> getFileMimetypeImpl() throws IOException {
        Map<String, String> result = new HashMap<String, String>();
        
        for (String name : getFileNames())
            result.put(name, mimetypeFromExtension(name));
        
        try {
            LWXMLReader in = new LWXMLStreamReader(getManifest());
            
            String mimetype = null;
            String name = null;
            
            while (true) {
                LWXMLEvent event = in.readEvent();
                if (event == LWXMLEvent.END_DOCUMENT) break;
                
                switch (event) {
                case ATTRIBUTE_NAME:
                    String attributeName = in.readValue();
                    
                    if (attributeName.equals("manifest:media-type")) {
                        mimetype = in.readFollowingValue();
                        // TODO: remove quickfix
                        mimetype = mimetype.replaceAll("&quot;", "\"");
                    } else if (attributeName.equals("manifest:full-path")) {
                        name = in.readFollowingValue();
                    }
                    
                    break;
                case END_ATTRIBUTE_LIST:
                    if ((mimetype != null) && (mimetype.trim().length() != 0)) result
                            .put(name, mimetype);
                    
                    mimetype = null;
                    name = null;
                    break;
                default:
                    break;
                }
            }
            
            in.close();
        } catch (ZipEntryNotFoundException e) {
            // TODO: log
        }
        
        return result;
    }
    
    public String getMimetype() throws IOException {
        if (mimetype == null) mimetype = getMimetypeImpl();
        
        return mimetype;
    }
    
    private String getMimetypeImpl() throws IOException {
        if (isFile(MIMETYPE_PATH)) {
            InputStream in = getRawFileStream(MIMETYPE_PATH);
            return CharStreamUtil.readString(new InputStreamReader(in, Charset
                    .forName("UTF-8")));
        } else {
            return getFileMimetype("/");
        }
    }
    
    public InputStream getManifest() throws IOException {
        return getRawFileStream(MANIFEST_PATH);
    }
    
    public OpenDocument getAsDocument() throws IOException {
        return OpenDocumentType.getSuitableDocument(this);
    }
    
}