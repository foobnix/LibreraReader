package com.foobnix.android.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * What the progress files rely on from IO: a file read is that file, as it is on disk now,
 * and a write leaves nothing half done beside it.
 */
public class IOWriteReadTest {

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

    @Test
    public void aFileIsReadBackAsWritten() throws Exception {
        final File file = new File(folder.getRoot(), "progress.json");
        IO.writeString(file, "{\"a.epub\":{\"p\":0.5}}");
        assertEquals("{\"a.epub\":{\"p\":0.5}}", IO.readString(file));
    }

    @Test
    public void aWriteLeavesNoTemporaryFileBehind() throws Exception {
        final File file = new File(folder.getRoot(), "progress.json");
        IO.writeString(file, "{}");
        assertFalse(new File(file.getPath() + ".tmp").exists());
    }

    @Test
    public void anotherFileIsNeverReadAsTheLastOne() throws Exception {
        final File first = new File(folder.getRoot(), "first.json");
        final File second = new File(folder.getRoot(), "second.json");
        IO.writeString(first, "first");
        IO.writeString(second, "second");
        assertEquals("first", IO.readString(first));
        assertEquals("second", IO.readString(second));
    }

    @Test
    public void aFileChangedBehindTheCacheIsReadAgain() throws Exception {
        // Sync downloads a device's progress file without going through IO.
        final File file = new File(folder.getRoot(), "progress.json");
        IO.writeString(file, "old");
        assertEquals("old", IO.readString(file));

        try (OutputStream out = new FileOutputStream(file)) {
            out.write("newer content".getBytes());
        }
        assertEquals("newer content", IO.readString(file));
    }

    @Test
    public void aReadWithLineBreaksIsNotHandedToAReadWithout() throws Exception {
        final File file = new File(folder.getRoot(), "lines.txt");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("one\ntwo".getBytes());
        }
        IO.readString(file, true);
        assertEquals("onetwo", IO.readString(file));
    }
}
