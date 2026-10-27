package org.ebookdroid.droids.foliate.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;

public class FoliateIndexTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MockedStatic<LOG> log;
    private File bookDirBefore;

    @Before
    public void setUp() throws Exception {
        log = Mockito.mockStatic(LOG.class, Mockito.withSettings().stubOnly());
        bookDirBefore = CacheZipUtils.CACHE_BOOK_DIR;
        CacheZipUtils.CACHE_BOOK_DIR = folder.newFolder("Book");
    }

    @After
    public void tearDown() {
        CacheZipUtils.CACHE_BOOK_DIR = bookDirBefore;
        log.close();
    }

    @Test
    public void aConvertedBookKeepsItsIndexWhenItIsConvertedAgain() throws Exception {
        final File converted = write(new File(CacheZipUtils.CACHE_BOOK_DIR, "-119982865.epub"), 100);
        converted.setLastModified(1_000_000_000_000L);
        final String key = key(converted);

        // What opening another book and coming back does: the same conversion, written anew
        write(converted, 100);
        converted.setLastModified(1_500_000_000_000L);
        assertEquals(key, key(converted));
    }

    @Test
    public void aDifferentConversionIsADifferentKey() throws Exception {
        final File converted = write(new File(CacheZipUtils.CACHE_BOOK_DIR, "52627033.epub"), 100);
        final String key = key(converted);
        write(converted, 120);
        assertNotEquals(key, key(converted));
    }

    @Test
    public void theReadersOwnFileIsKeyedByItsDate() throws Exception {
        final File book = write(folder.newFile("book.epub"), 100);
        book.setLastModified(1_000_000_000_000L);
        final String key = key(book);
        book.setLastModified(1_500_000_000_000L);
        assertNotEquals(key, key(book));
    }

    @Test
    public void onlyTheBookCacheIsConverted() throws Exception {
        assertTrue(FoliateIndex.isConverted(new File(CacheZipUtils.CACHE_BOOK_DIR, "1.epub")));
        assertFalse(FoliateIndex.isConverted(folder.newFile("1.epub")));
        assertFalse(FoliateIndex.isConverted(new File(new File(CacheZipUtils.CACHE_BOOK_DIR, "sub"), "1.epub")));
    }

    private static String key(final File book) {
        return FoliateIndex.key(book.getPath(), 1080, 2200, 55, "css");
    }

    private static File write(final File file, final int length) throws Exception {
        final FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(new byte[length]);
        } finally {
            out.close();
        }
        return file;
    }
}
