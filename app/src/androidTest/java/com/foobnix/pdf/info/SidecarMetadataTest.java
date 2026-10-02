package com.foobnix.pdf.info;

import android.net.Uri;
import com.foobnix.ext.CalirbeExtractor;
import com.foobnix.ext.EbookMeta;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import static org.junit.Assert.*;

public class SidecarMetadataTest {
    @Test public void calibrePublicationDateSurvivesStreamParsing() {
        String opf = "<package xmlns:dc='http://purl.org/dc/elements/1.1/'><metadata>"
                + "<dc:title>Book</dc:title><dc:date>2020-05-14</dc:date></metadata></package>";
        EbookMeta meta = CalirbeExtractor.getBookMetaInformationFromStream(
                new ByteArrayInputStream(opf.getBytes(StandardCharsets.UTF_8)), null);
        assertEquals("2020-05-14", meta.getYear());
    }
    @Test public void calibreOpfKeepsFluentPythonTitleAuthorAndAbsenceOfSeriesNumber() {
        String opf = "<package xmlns:dc='http://purl.org/dc/elements/1.1/'><metadata>"
                + "<dc:title>Fluent Python</dc:title><dc:creator>Luciano Ramalho</dc:creator>"
                + "<dc:language>en</dc:language></metadata><guide>"
                + "<reference type='cover' href='images/COVER.JPG'/></guide></package>";
        byte[] cover = {1,2,3};
        EbookMeta meta = CalirbeExtractor.getBookMetaInformationFromStream(
                new ByteArrayInputStream(opf.getBytes(StandardCharsets.UTF_8)), href -> {
                    assertEquals("images/COVER.JPG", href); return cover;
                });
        assertEquals("Fluent Python", meta.getTitle()); assertEquals("Luciano Ramalho", meta.getAuthor());
        assertEquals("en", meta.getLang()); assertNull(meta.getsIndex()); assertArrayEquals(cover, meta.coverImage);
    }
    @Test public void persistentSidecarRoundTripRetainsCoverUriAndRevision() throws Exception {
        SafOpfRegistry.Entry entry = new SafOpfRegistry.Entry(Uri.parse("content://fixture/opf"),
                Collections.singletonMap("cover.jpg", Uri.parse("content://fixture/cover")), "revision-2");
        var restored = SafOpfRegistry.decode(SafOpfRegistry.encode(Collections.singletonMap("book", entry)));
        assertEquals(entry.opfUri, restored.get("book").opfUri);
        assertEquals(entry.siblingByLowerName, restored.get("book").siblingByLowerName);
        assertEquals("revision-2", restored.get("book").revision);
    }
    @Test public void corruptedPersistentSidecarsCannotProducePartialRegistry() {
        assertThrows(org.json.JSONException.class, () -> SafOpfRegistry.decode("{broken"));
        assertThrows(org.json.JSONException.class, () -> SafOpfRegistry.decode("{\"book\":{\"opf\":\"uri\"}}"));
    }
    @Test public void olderRegistryWithoutRevisionStillRestoresCover() throws Exception {
        String json = "{\"book\":{\"opf\":\"content://fixture/opf\",\"siblings\":{\"cover.jpg\":\"content://fixture/cover\"}}}";
        assertEquals("", SafOpfRegistry.decode(json).get("book").revision);
        assertEquals(Uri.parse("content://fixture/cover"), SafOpfRegistry.decode(json).get("book").siblingByLowerName.get("cover.jpg"));
    }

    @Test public void legacyGrantSidecarMatchesCanonicalBookIdentity() {
        String grant = "content://fixture/tree/root/document/book-7";
        String identity = "content://fixture/document/book-7";
        SafOpfRegistry.Entry old = new SafOpfRegistry.Entry(Uri.parse("content://fixture/old-opf"),
                Collections.emptyMap(), "old");
        SafOpfRegistry.Entry current = new SafOpfRegistry.Entry(Uri.parse("content://fixture/new-opf"),
                Collections.emptyMap(), "new");
        var saved = new HashMap<String, SafOpfRegistry.Entry>();
        saved.put(grant, old);
        assertSame(old, SafOpfRegistry.normalizedKeys(saved).get(identity));
        saved.put(identity, current);
        assertSame("A current document-key entry wins over its legacy alias", current,
                SafOpfRegistry.normalizedKeys(saved).get(identity));

        SafOpfRegistry.register(grant, old);
        try {
            SafOpfRegistry.Entry restored = SafOpfRegistry.get(identity);
            assertEquals(old.opfUri, restored.opfUri);
            assertEquals(old.revision, restored.revision);
            assertEquals(old.siblingByLowerName, restored.siblingByLowerName);
        }
        finally { SafOpfRegistry.unregister(identity); }
    }
    @Test public void coldCoverRegistryLookupDoesNotPerformMainThreadDiskIo() {
        android.content.Context context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
        SafOpfRegistry.clear();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            android.os.StrictMode.ThreadPolicy previous = android.os.StrictMode.getThreadPolicy();
            android.os.StrictMode.setThreadPolicy(new android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads().detectDiskWrites().penaltyDeath().build());
            try {
                SafOpfRegistry.restore(context);
                assertNull(SafOpfRegistry.get("content://cold.fixture/document/" + java.util.UUID.randomUUID()));
            } finally { android.os.StrictMode.setThreadPolicy(previous); }
        });
        SafOpfRegistry.clear();
    }
}
