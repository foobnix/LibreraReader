package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.droids.comic.codec.ComicContext;
import org.ebookdroid.droids.comic.codec.ComicDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.zip.CRC32;

/**
 * .cbz and .cbr read with libarchive ({@link NativeArchive}) by the comic engine, which opens
 * them in every build.
 *
 * The archives are not in the repository: they are pushed beside the app, into
 * {@code Android/data/<application id>/files/comic-test/}, each with a {@code <name>.manifest}
 * of its files in stored order - size, CRC-32 and name, one per line, tab-separated - made on a
 * computer by extracting the archive with another unpacker. Without them the tests are skipped.
 * Timings are logged under {@value #TAG}.
 */
@RunWith(AndroidJUnit4.class)
public class ComicArchiveDeviceTest {

    private static final String TAG = "ComicArchiveTest";
    private static final int PAGE_WIDTH = 1080;
    private static final int PAGE_HEIGHT = 1800;

    private File dir;

    private static final class Expected {
        final long size;
        final long crc;
        final String name;

        Expected(final long size, final long crc, final String name) {
            this.size = size;
            this.crc = crc;
            this.name = name;
        }
    }

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);

        dir = new File(context.getExternalFilesDir(null), "comic-test");
        final String[] files = dir.list();
        assumeTrue("no samples in " + dir, files != null && files.length > 0);
    }

    @Test
    public void everyFileReadsAsTheReferenceSaysInAnyOrder() throws Exception {
        int archives = 0;
        for (final File file : samples()) {
            final File manifest = new File(file.getPath() + ".manifest");
            if (!manifest.isFile()) {
                continue;
            }
            archives++;
            final List<Expected> expected = readManifest(manifest);

            long t = System.nanoTime();
            final NativeArchive archive = NativeArchive.open(file.getPath());
            final long openMs = ms(t);
            assertNotNull("opens: " + file.getName(), archive);
            try {
                final List<Integer> files = new ArrayList<Integer>();
                for (int i = 0; i < archive.getNames().size(); i++) {
                    if (archive.getNames().get(i) != null) {
                        files.add(i);
                    }
                }
                assertEquals("the files of " + file.getName(), expected.size(), files.size());

                t = System.nanoTime();
                for (int k = 0; k < files.size(); k++) {
                    check(archive, file, files, expected, k);
                }
                final long forwardMs = ms(t);

                t = System.nanoTime();
                for (int k = files.size() - 1; k >= 0; k--) {
                    check(archive, file, files, expected, k);
                }
                final long backwardMs = ms(t);

                final List<Integer> order = new ArrayList<Integer>();
                for (int k = 0; k < files.size(); k++) {
                    order.add(k);
                }
                Collections.shuffle(order, new Random(1));
                t = System.nanoTime();
                for (final int k : order) {
                    check(archive, file, files, expected, k);
                }
                final long randomMs = ms(t);

                Log.i(TAG, String.format(Locale.US,
                        "%-34s %3d files  open %5d ms  forward %6d ms  backward %6d ms  random %6d ms",
                        file.getName(), files.size(), openMs, forwardMs, backwardMs, randomMs));
            } finally {
                archive.close();
            }
        }
        assumeTrue("no manifests in " + dir, archives > 0);
    }

    /**
     * A solid RAR 1.5-4.x: libarchive reads only its first file, which is all this one holds, and
     * that file fails its CRC in the reference unpacker too. Whatever it makes of it, it answers
     * rather than crashing.
     */
    @Test
    public void aBrokenSolidRarAnswers() {
        final File file = new File(dir, "rar4-solid-aho.rar");
        assumeTrue(file.isFile());
        final long t = System.nanoTime();
        final NativeArchive archive = NativeArchive.open(file.getPath());
        if (archive == null) {
            Log.i(TAG, file.getName() + " does not open");
            return;
        }
        try {
            final byte[] bytes = archive.read(0);
            Log.i(TAG, file.getName() + " entry 0: " + (bytes == null ? "unreadable" : bytes.length + " bytes")
                    + " in " + ms(t) + " ms, expected " + archive.getSize(0));
        } finally {
            archive.close();
        }
    }

    @Test
    public void everyComicOpensInTheComicEngine() throws Exception {
        int comics = 0;
        for (final File file : samples()) {
            final String lower = file.getName().toLowerCase(Locale.US);
            if (!lower.endsWith(".cbr") && !lower.endsWith(".cbz")) {
                continue;
            }
            comics++;

            final CodecContext context = BookType.getCodecContextByPath(file.getPath());
            assertTrue("the comic engine reads " + file.getName() + ": " + context,
                    context instanceof ComicContext);

            int images = -1;
            final File manifest = new File(file.getPath() + ".manifest");
            if (manifest.isFile()) {
                images = 0;
                for (final Expected entry : readManifest(manifest)) {
                    if (ComicDocument.isImage(entry.name)) {
                        images++;
                    }
                }
            }

            long t = System.nanoTime();
            final CodecDocument document = context.openDocument(file.getPath(), "");
            final long openMs = ms(t);
            assertNotNull("opens: " + file.getName(), document);
            try {
                final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, 16);
                if (images >= 0) {
                    assertEquals("one page per image in " + file.getName(), images, pages);
                }
                assertTrue(pages > 0);

                t = System.nanoTime();
                render(document, 0, file);
                final long firstMs = ms(t);

                // the last page, then back to the middle and the first - the archive starts again
                t = System.nanoTime();
                render(document, pages - 1, file);
                render(document, pages / 2, file);
                render(document, 0, file);
                final long jumpsMs = ms(t);

                t = System.nanoTime();
                final byte[] cover = CbzCbrExtractor.getBookCover(file.getPath());
                final long coverMs = ms(t);
                final BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(cover, 0, cover.length, bounds);
                assertTrue("a cover for " + file.getName(), bounds.outWidth > 0 && bounds.outHeight > 0);

                Log.i(TAG, String.format(Locale.US,
                        "%-34s %3d pages  open %5d ms  page 1 %5d ms  last+middle+first %5d ms  cover %5d ms",
                        file.getName(), pages, openMs, firstMs, jumpsMs, coverMs));
            } finally {
                document.recycle();
            }
        }
        assumeTrue("no comics in " + dir, comics > 0);
    }

    private static void render(final CodecDocument document, final int index, final File file) {
        final CodecPage page = document.getPage(index);
        assertNotNull("page " + index + " of " + file.getName(), page);
        final BitmapRef ref = page.renderBitmap(PAGE_WIDTH, PAGE_HEIGHT, new RectF(0, 0, 1, 1), false);
        assertNotNull("rendered page " + index + " of " + file.getName(), ref);
        final Bitmap bitmap = ref.getBitmap();
        assertTrue("page " + index + " of " + file.getName() + " shows something", hasContent(bitmap));
        page.recycle();
    }

    /** Not one flat colour: a page that failed to decode comes out blank. */
    private static boolean hasContent(final Bitmap bitmap) {
        final int first = bitmap.getPixel(0, 0);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                final int pixel = bitmap.getPixel(bitmap.getWidth() * x / 10, bitmap.getHeight() * y / 10);
                if (pixel != first) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void check(final NativeArchive archive, final File file, final List<Integer> files,
                              final List<Expected> expected, final int k) {
        final Expected entry = expected.get(k);
        final byte[] bytes = archive.read(files.get(k));
        assertNotNull("reads " + entry.name + " of " + file.getName(), bytes);
        assertEquals("size of " + entry.name + " in " + file.getName(), entry.size, bytes.length);
        final CRC32 crc = new CRC32();
        crc.update(bytes);
        assertEquals("CRC of " + entry.name + " in " + file.getName(), entry.crc, crc.getValue());
    }

    private List<File> samples() {
        final List<File> files = new ArrayList<File>();
        final File[] all = dir.listFiles();
        if (all != null) {
            files.addAll(Arrays.asList(all));
        }
        final List<File> archives = new ArrayList<File>();
        for (final File file : files) {
            if (file.isFile() && !file.getName().endsWith(".manifest")) {
                archives.add(file);
            }
        }
        Collections.sort(archives);
        return archives;
    }

    private static List<Expected> readManifest(final File manifest) throws Exception {
        final List<Expected> entries = new ArrayList<Expected>();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(manifest), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                final String[] parts = line.split("\t", 3);
                entries.add(new Expected(Long.parseLong(parts[0]), Long.parseLong(parts[1], 16), parts[2]));
            }
        }
        return entries;
    }

    private static long ms(final long since) {
        return (System.nanoTime() - since) / 1000000;
    }
}
