package com.foobnix.pdf.info;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.sys.TempHolder;
import com.foobnix.sys.VerticalModeController;
import com.foobnix.ui2.AppDB;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.util.UUID;
import static org.junit.Assert.*;

public class ReaderLifecycleTest {
    private String original, priorPath, priorOriginal;
    private boolean priorCancellation;
    private AppBook priorSettings;
    private int priorTtsParagraph;
    private int priorTtsPage;
    private Controller controller;
    private static class Controller extends VerticalModeController {
        int page = 1;
        Controller(Activity activity) { super(activity, null); }
        @Override public int getPageCount() { return 100; }
        @Override public int getCurentPage() { return page; }
        @Override public int getCurentPageFirst1() { return page; }
        @Override public void onGoToPage(int page) { this.page = page; }
    }
    @Before public void setUp() {
        AppProfile.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        priorPath = AppSP.get().lastBookPath; priorOriginal = AppSP.get().lastBookOriginalUri;
        priorTtsParagraph = AppSP.get().lastBookParagraph; priorTtsPage = AppSP.get().tempBookPage;
        priorCancellation = TempHolder.get().loadingCancelled.get(); priorSettings = SettingsManager.getBookSettings();
        original = "content://reader-lifecycle/document/" + UUID.randomUUID();
        FileMeta meta = AppDB.get().getOrCreate(original); meta.setTitle("Fluent Python"); meta.setPathTxt("Fluent Python.pdf"); AppDB.get().save(meta);
        AppBook saved = new AppBook(original); saved.p = 0.75f;
        SharedBooks.cache.put(ExtUtils.getFileName(original), saved);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Activity activity = new Activity();
            Intent launch = new Intent().setData(android.net.Uri.fromFile(new File("/fixture/source-version-1.pdf")));
            SafReaderLaunch.attach(InstrumentationRegistry.getInstrumentation().getTargetContext(), launch, original);
            activity.setIntent(launch);
            controller = new Controller(activity); controller.setCurrentBook(new File("/fixture/source-version-1.pdf"));
        });
    }
    @After public void tearDown() {
        SharedBooks.cache.remove(ExtUtils.getFileName(original)); AppDB.get().deleteBy(original);
        AppSP.get().lastBookPath = priorPath; AppSP.get().lastBookOriginalUri = priorOriginal;
        AppSP.get().lastBookParagraph = priorTtsParagraph; AppSP.get().tempBookPage = priorTtsPage;
        TempHolder.get().loadingCancelled.set(priorCancellation);
        if (priorSettings != null) SettingsManager.getBookSettings(priorSettings.path);
    }
    @Test public void resumeRestoresOriginalProgressRatherThanStagedFileProgress() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(controller::onResume);
        assertEquals(75, controller.page); assertEquals(original, SettingsManager.getBookSettings().path);
        assertEquals("/fixture/source-version-1.pdf", controller.getCurrentBook().getPath());
    }
    @Test public void newStagedVersionKeepsProgressAndBookmarkIdentity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            controller.setCurrentBook(new File("/fixture/source-version-2.pdf")); controller.onResume();
        });
        assertEquals(75, controller.page);
        var bookmark = controller.createBookmark("Useful passage");
        assertEquals(original, bookmark.getPath()); assertEquals(0.75f, bookmark.p, 0.001f);
        assertEquals("Useful passage", bookmark.text);
    }
    @Test public void displayAndMetadataUseOriginalBookWhileRestartRetainsCodecFile() {
        assertEquals("Fluent Python.pdf", controller.getBookDisplayName());
        assertEquals("Fluent Python", controller.getBookFileMeta().getTitle());
        assertEquals(original, controller.getBookIdentity());
        assertEquals(original, AppSP.get().lastBookOriginalUri);
        assertEquals(controller.getCurrentBook().getPath(), AppSP.get().lastBookPath);
    }
    @Test public void openingLocalBookClearsPreviousSafIdentity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            controller.getActivity().setIntent(new Intent()); controller.setCurrentBook(new File("/fixture/Local.pdf"));
        });
        assertNull(AppSP.get().lastBookOriginalUri); assertEquals("/fixture/Local.pdf", controller.getBookIdentity());
    }
    @Test public void switchingBooksClearsOldTtsParagraph() {
        AppSP.get().lastBookParagraph = 7;
        AppSP.get().tempBookPage = 12;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                controller.setCurrentBook(new File("/fixture/source-version-1.pdf")));
        assertEquals(7, AppSP.get().lastBookParagraph);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                controller.setCurrentBook(new File("/fixture/source-version-2.pdf")));
        assertEquals(0, AppSP.get().lastBookParagraph);
        assertEquals(-1, AppSP.get().tempBookPage);
    }
}
