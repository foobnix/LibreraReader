package com.foobnix.ext;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OdtExtractor extends BaseExtractor {

    final static OdtExtractor inst = new OdtExtractor();

    private OdtExtractor() {

    }

    public static OdtExtractor get() {
        return inst;
    }

    @Override
    public byte[] getBookCover(String path) {
        try {
            ZipFile zp = new ZipFile(path);
            List<FileHeader> headers = zp.getFileHeaders();
            for (FileHeader h : headers) {
                if (h.getFileName().toLowerCase(Locale.US).endsWith("thumbnail.png") && h.getUncompressedSize() > 1024) {
                    LOG.d("find thumbnail.png in ", path, h.getFileName(), h.getUncompressedSize());
                    ZipInputStream inputStream = zp.getInputStream(h);
                    try {
                        return BaseExtractor.getEntryAsByte(inputStream);
                    } finally {
                        inputStream.close();
                    }
                }
            }
            for (FileHeader h : headers) {
                String name = h.getFileName().toLowerCase(Locale.US);
                if ((name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) && h.getUncompressedSize() > 1024) {
                    LOG.d("find thumbnail.png in ", path, h.getFileName(), h.getUncompressedSize());
                    ZipInputStream inputStream = zp.getInputStream(h);
                    try {
                        return BaseExtractor.getEntryAsByte(inputStream);
                    } finally {
                        inputStream.close();
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;
    }

    @Override
    public EbookMeta getBookMetaInformation(String path) {
        return null;
    }

    @Override
    public Map<String, String> getFooterNotes(String path) {
        return null;
    }

    @Override
    public String getBookOverview(String path) {
        return null;
    }

}
