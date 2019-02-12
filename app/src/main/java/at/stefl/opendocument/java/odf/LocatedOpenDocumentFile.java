package at.stefl.opendocument.java.odf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import at.stefl.commons.util.EnumerationUtil;

public class LocatedOpenDocumentFile extends OpenDocumentFile {
    
    private File file;
    private ZipFile zipFile;
    
    private Map<String, ZipEntry> entryMap;
    
    public LocatedOpenDocumentFile(File file) throws IOException {
        init(file);
    }
    
    public LocatedOpenDocumentFile(String path) throws IOException {
        this(new File(path));
    }
    
    protected void init(File file) throws IOException {
        this.file = file;
        this.zipFile = new ZipFile(file);
    }
    
    public File getFile() {
        return file;
    }
    
    @Override
    public long getFileSize(String name) {
        return zipFile.getEntry(name).getSize();
    }
    
    @Override
    public boolean isFile(String name) throws IOException {
        if (entryMap == null) getFileNames();
        return entryMap.containsKey(name);
    }
    
    @Override
    public Set<String> getFileNames() throws IOException {
        if (entryMap == null) {
            entryMap = new HashMap<String, ZipEntry>();
            
            for (ZipEntry entry : EnumerationUtil.iterable(zipFile.entries())) {
                entryMap.put(entry.getName(), entry);
            }
        }
        
        return entryMap.keySet();
    }
    
    @Override
    protected InputStream getRawFileStream(String name) throws IOException {
        if (entryMap == null) getFileNames();
        
        ZipEntry entry = entryMap.get(name);
        if (entry == null) throw new ZipEntryNotFoundException(
                "entry does not exist: " + name);
        
        return zipFile.getInputStream(entry);
    }
    
    @Override
    public void close() throws IOException {
        zipFile.close();
    }
    
}