package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.sys.TempHolder;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// small EPUB and FB2 files for the footnote tests
final class TestBooks {

    private TestBooks() {
    }

    // LOG.d reads AppsConfig.IS_LOG, whose static init loads the MuPDF native library;
    // stubOnly keeps the mock from recording every call
    static MockedStatic<LOG> setUp() {
        // not a constant in android.jar, so it is null in JVM tests and kxml rejects it
        android.util.Xml.FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";
        TempHolder.get().loadingCancelled.set(false);
        return Mockito.mockStatic(LOG.class, Mockito.withSettings().stubOnly());
    }

    static String xhtml(String body) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>t</title></head><body>\n"
                + body + "\n</body></html>";
    }

    // name and content pairs; a String is the body of an XHTML file, byte[] is written as it is
    static File epub(File file, boolean stored, Object... nameAndContent) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            put(zip, "mimetype", "application/epub+zip".getBytes(StandardCharsets.US_ASCII), true);
            for (int i = 0; i < nameAndContent.length; i += 2) {
                Object content = nameAndContent[i + 1];
                byte[] data = content instanceof byte[] ? (byte[]) content : xhtml((String) content).getBytes(StandardCharsets.UTF_8);
                put(zip, (String) nameAndContent[i], data, stored);
            }
        }
        return file;
    }

    private static void put(ZipOutputStream zip, String name, byte[] data, boolean stored) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        if (stored) {
            CRC32 crc = new CRC32();
            crc.update(data);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(data.length);
            entry.setCompressedSize(data.length);
            entry.setCrc(crc.getValue());
        }
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }

    static File fb2(File file, String encoding, String bodies) throws IOException {
        String fb2 = "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n"
                + "<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\" xmlns:l=\"http://www.w3.org/1999/xlink\">\n"
                + bodies + "\n</FictionBook>";
        Files.write(file.toPath(), fb2.getBytes(Charset.forName(encoding)));
        return file;
    }
}
