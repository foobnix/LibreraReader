package com.foobnix.work;

import android.net.Uri;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.SafOpfRegistry;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class SafSidecarDiscoveryTest {
    @Before public void enableDiscoveryFormats() { ExtUtils.updateSearchExts(); }
    private static SafDocuments.Document file(String name, String id, long size, long modified) {
        return new SafDocuments.Document(name, Uri.parse("content://fixture/" + id),
                false, size, modified);
    }

    @Test public void siblingOpfAndCoverAreMatchedToBookAndChangeRevision() {
        SafDocuments.Document book = file("Fluent Python.epub", "book", 100, 10);
        SafDocuments.Document opf = file("metadata.opf", "opf", 20, 10);
        SafDocuments.Document cover = file("cover.jpg", "cover", 30, 10);
        Map<String, SafOpfRegistry.Entry> first = new HashMap<>();
        SafDiscovery.recordSidecars(Arrays.asList(book, opf, cover), first);
        SafOpfRegistry.Entry entry = first.get(book.uri.toString());
        assertNotNull(entry);
        assertEquals(opf.uri, entry.opfUri);
        assertEquals(cover.uri, entry.siblingByLowerName.get("cover.jpg"));
        Map<String, SafOpfRegistry.Entry> changed = new HashMap<>();
        SafDiscovery.recordSidecars(Arrays.asList(book, opf,
                file("cover.jpg", "cover", 31, 11)), changed);
        assertNotEquals(entry.revision, changed.get(book.uri.toString()).revision);
    }

    @Test public void basenameOpfWinsOverFolderMetadata() {
        SafDocuments.Document book = file("Other.epub", "book", 100, 10);
        SafDocuments.Document named = file("Other.opf", "named", 20, 10);
        Map<String, SafOpfRegistry.Entry> sidecars = new HashMap<>();
        SafDiscovery.recordSidecars(Arrays.asList(book, file("metadata.opf", "default", 20, 10), named), sidecars);
        assertEquals(named.uri, sidecars.get(book.uri.toString()).opfUri);
    }
}
