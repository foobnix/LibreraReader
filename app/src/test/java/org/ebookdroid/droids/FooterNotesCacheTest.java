package org.ebookdroid.droids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.sys.TempHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class FooterNotesCacheTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MockedStatic<LOG> log;

    @Before
    public void setUp() {
        log = Mockito.mockStatic(LOG.class, Mockito.withSettings().stubOnly());
        TempHolder.get().loadingCancelled.set(false);
    }

    @After
    public void tearDown() {
        log.close();
        TempHolder.get().loadingCancelled.set(false);
    }

    private static Map<String, String> notes() {
        Map<String, String> notes = new LinkedHashMap<>();
        notes.put("[1]#ch1.xhtml", "[1] The first note.");
        return notes;
    }

    @Test
    public void theCacheSitsNextToTheBook() {
        assertEquals(new File("/cache/123.epub.json"), FooterNotesCache.fileFor(new File("/cache/123.epub")));
    }

    @Test
    public void savesAndReadsBack() {
        File json = new File(folder.getRoot(), "notes.json");
        assertTrue(FooterNotesCache.save(json, notes()));
        assertEquals(notes(), JsonHelper.fileToMap(json));
    }

    @Test
    public void savesNothingWithoutAMap() {
        File json = new File(folder.getRoot(), "notes.json");
        assertFalse(FooterNotesCache.save(json, null));
        assertFalse(json.exists());
    }

    // a cancelled extraction is empty or partial: caching it would serve a broken map for good
    @Test
    public void savesNothingWhenTheLoadingWasCancelled() {
        TempHolder.get().loadingCancelled.set(true);
        File json = new File(folder.getRoot(), "notes.json");
        assertFalse(FooterNotesCache.save(json, notes()));
        assertFalse(json.exists());
        assertEquals(0, folder.getRoot().listFiles().length);
    }

    @Test
    public void takesBackWhatItWroteWhenCancelledWhileWriting() {
        File json = new File(folder.getRoot(), "notes.json");
        // the cancellation lands while the map is being serialised, after the first lookup
        Map<String, String> cancelling = new LinkedHashMap<String, String>(notes()) {
            @Override
            public String get(Object key) {
                TempHolder.get().loadingCancelled.set(true);
                return super.get(key);
            }
        };
        assertFalse(FooterNotesCache.save(json, cancelling));
        assertFalse(json.exists());
        assertEquals(0, folder.getRoot().listFiles().length);
    }

    @Test
    public void keepsTheMapAnotherOpenAlreadyCached() throws Exception {
        File json = folder.newFile("notes.json");
        Map<String, String> cached = notes();
        assertTrue(JsonHelper.mapToFile(json, cached));

        Map<String, String> other = new LinkedHashMap<>();
        other.put("[7]#ch7.xhtml", "[7] Extracted a second time.");
        assertFalse(FooterNotesCache.save(json, other));
        assertEquals(cached, JsonHelper.fileToMap(json));
    }

    @Test
    public void repairsAnIncompleteCachedMap() throws Exception {
        File json = folder.newFile("notes.json");
        assertTrue(FooterNotesCache.save(json, notes()));
        assertEquals(notes(), JsonHelper.fileToMap(json));
    }

    @Test
    public void saveAsyncWritesTheCacheOffThread() throws Exception {
        File json = new File(folder.getRoot(), "notes.json");
        FooterNotesCache.saveAsync(json, notes());
        for (int i = 0; i < 100 && !json.isFile(); i++) {
            Thread.sleep(20);
        }
        assertTrue("the cache was not written within 2s", json.isFile());
        assertEquals(notes(), JsonHelper.fileToMap(json));
    }
}
