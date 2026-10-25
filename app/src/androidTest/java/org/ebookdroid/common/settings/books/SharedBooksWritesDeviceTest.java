package org.ebookdroid.common.settings.books;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.android.utils.IO;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;

import org.ebookdroid.common.settings.SettingsManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TemporaryFolder;
import org.librera.LinkedJSONObject;

import java.io.File;

/**
 * Writing a book's position into this device's progress file, and reading it back when the
 * reader returns to the front: no save undoes another, and the page on screen is not turned
 * back to an older one read from the files.
 */
@RunWith(AndroidJUnit4.class)
public class SharedBooksWritesDeviceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File savedProfile;
    private File savedProgress;

    @Before
    public void pointTheProfileAtAFolderOfItsOwn() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);

        savedProfile = AppProfile.SYNC_FOLDER_PROFILE;
        savedProgress = AppProfile.syncProgress;

        AppProfile.SYNC_FOLDER_PROFILE = folder.getRoot();
        final File device = new File(folder.getRoot(), AppProfile.DEVICE_PREFIX + "test");
        device.mkdirs();
        AppProfile.syncProgress = new File(device, AppProfile.APP_PROGRESS_JSON);

        SharedBooks.written.clear();
        SharedBooks.cache.clear();
    }

    @After
    public void restoreTheProfile() {
        AppProfile.SYNC_FOLDER_PROFILE = savedProfile;
        AppProfile.syncProgress = savedProgress;
        SharedBooks.cache.clear();
    }

    private static AppBook book(String path, float p, long t) {
        final AppBook book = new AppBook(path);
        book.p = p;
        book.t = t;
        return book;
    }

    private static float savedP(String name) throws Exception {
        final LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncProgress);
        return (float) obj.getJSONObject(name).getDouble("p");
    }

    @Test
    public void twoBooksSavedTogetherAreBothWritten() throws Exception {
        SharedBooks.save(book("/books/a.epub", 0.25f, 1));
        SharedBooks.saveNow(book("/books/b.epub", 0.75f, 2));
        SharedBooks.flush();

        assertEquals(0.25f, savedP("a.epub"), 0.0001);
        assertEquals(0.75f, savedP("b.epub"), 0.0001);
    }

    @Test
    public void aLaterPositionIsNotUndoneByAnEarlierWrite() throws Exception {
        final AppBook a = book("/books/a.epub", 0.25f, 1);
        SharedBooks.save(a);
        a.p = 0.30f;
        a.t = 2;
        SharedBooks.saveNow(a);
        // Whatever the background write still had to do runs after
        SharedBooks.flush();

        assertEquals(0.30f, savedP("a.epub"), 0.0001);
    }

    @Test
    public void aResetBookIsNotPutBackByAWaitingWrite() throws Exception {
        SharedBooks.saveNow(book("/books/a.epub", 0.25f, 1));
        SharedBooks.save(book("/books/a.epub", 0.50f, 2));
        SharedBooks.deleteProgress("/books/a.epub");
        SharedBooks.flush();

        assertFalse(IO.readJsonObject(AppProfile.syncProgress).has("a.epub"));
    }

    @Test
    public void thePageOnScreenIsKeptWhenTheFilesAreOlder() throws Exception {
        SharedBooks.saveNow(book("/books/a.epub", 0.25f, 1));
        final AppBook open = SettingsManager.getBookSettings("/books/a.epub");

        // A page turned and not written yet, then sync empties the cache
        open.p = 0.30f;
        open.t = 2;
        SharedBooks.cache.clear();

        final AppBook refreshed = SettingsManager.refreshBookSettings("/books/a.epub");
        assertSame(open, refreshed);
        assertEquals(0.30f, refreshed.p, 0.0001);
        assertSame(open, SharedBooks.load("/books/a.epub"));
    }

    @Test
    public void aNewerPositionInTheFilesIsTakenUp() throws Exception {
        SharedBooks.saveNow(book("/books/a.epub", 0.25f, 1));
        SettingsManager.getBookSettings("/books/a.epub");

        // Read aloud in the background, written by a record of its own
        SharedBooks.cache.clear();
        SharedBooks.saveNow(book("/books/a.epub", 0.60f, 5));
        SharedBooks.cache.clear();

        final AppBook refreshed = SettingsManager.refreshBookSettings("/books/a.epub");
        assertEquals(0.60f, refreshed.p, 0.0001);
        assertTrue(refreshed.t >= 5);
    }
}
