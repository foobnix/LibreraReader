package com.foobnix.pdf.info;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.foobnix.android.utils.LOG;

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

public class JsonHelperTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MockedStatic<LOG> log;

    @Before
    public void setUp() {
        log = Mockito.mockStatic(LOG.class, Mockito.withSettings().stubOnly());
    }

    @After
    public void tearDown() {
        log.close();
    }

    private static Map<String, String> notes() {
        Map<String, String> notes = new LinkedHashMap<>();
        notes.put("[1]#ch1.xhtml", "[1] The first note.");
        notes.put("[2]#ch1.xhtml", "[2] The second note.");
        return notes;
    }

    @Test
    public void mapToFileReportsSuccess() {
        File json = new File(folder.getRoot(), "notes.json");
        assertTrue(JsonHelper.mapToFile(json, notes()));
        assertEquals(notes(), JsonHelper.fileToMap(json));
    }

    @Test
    public void mapToFileReportsFailure() throws Exception {
        // a folder cannot be opened for writing
        assertFalse(JsonHelper.mapToFile(folder.newFolder("taken"), notes()));
    }

    @Test
    public void mapToFileIfAbsentPublishesTheMap() {
        File json = new File(folder.getRoot(), "notes.json");
        assertTrue(JsonHelper.mapToFileIfAbsent(json, notes()));
        assertTrue(json.isFile());
        assertEquals(notes(), JsonHelper.fileToMap(json));
    }

    @Test
    public void mapToFileIfAbsentKeepsWhatIsThere() throws Exception {
        File json = folder.newFile("notes.json");
        Map<String, String> first = notes();
        assertTrue(JsonHelper.mapToFile(json, first));

        Map<String, String> other = new LinkedHashMap<>();
        other.put("[9]#ch9.xhtml", "[9] A different extraction.");
        assertFalse(JsonHelper.mapToFileIfAbsent(json, other));
        assertEquals(first, JsonHelper.fileToMap(json));
    }

    @Test
    public void mapToFileIfAbsentKeepsAnExistingInvalidFile() throws Exception {
        File json = folder.newFile("notes.json");
        assertFalse(JsonHelper.mapToFileIfAbsent(json, notes()));
        assertEquals(0, json.length());
    }

    @Test
    public void mapToCacheFileRepairsInvalidJsonAndPreservesACompleteMap() throws Exception {
        File json = folder.newFile("notes.json");
        assertTrue(JsonHelper.mapToCacheFile(json, notes()));
        Map<String, String> other = new LinkedHashMap<>();
        other.put("other", "a later extraction");
        assertFalse(JsonHelper.mapToCacheFile(json, other));
        assertEquals(notes(), JsonHelper.fileToMap(json));
        assertEquals(1, folder.getRoot().listFiles().length);
    }

    @Test
    public void mapToFileCannotReplaceAnActiveCacheEntry() throws Exception {
        File json = new File(folder.getRoot(), "notes.json");
        assertTrue(JsonHelper.mapToFile(json, notes()));
        try (AutoCloseable lease = BookCacheLeases.acquire(json)) {
            Map<String, String> other = new LinkedHashMap<>();
            other.put("other", "a later extraction");
            assertFalse(JsonHelper.mapToFile(json, other));
            assertEquals(notes(), JsonHelper.fileToMap(json));
        }
        assertEquals(1, folder.getRoot().listFiles().length);
    }

    @Test
    public void mapToFileIfAbsentLeavesNoTemporaryFile() {
        File json = new File(folder.getRoot(), "notes.json");
        JsonHelper.mapToFileIfAbsent(json, notes());
        JsonHelper.mapToFileIfAbsent(json, notes());
        assertEquals(1, folder.getRoot().listFiles().length);
    }

    @Test
    public void mapToFileIfAbsentCleansUpWhenTheWriteFails() throws Exception {
        File dir = folder.newFolder("notes.json");
        // the parent of the target does not exist, so the temporary file cannot be written either
        assertFalse(JsonHelper.mapToFileIfAbsent(new File(dir, "missing/notes.json"), notes()));
        assertEquals(0, dir.listFiles().length);
    }
}
