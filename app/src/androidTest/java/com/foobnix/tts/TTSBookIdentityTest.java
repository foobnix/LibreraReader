package com.foobnix.tts;

import com.foobnix.model.AppSP;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TTSBookIdentityTest {
    @Test public void liveServiceKeepsAIdentityWhenOpeningBChangesGlobalReaderFields() throws Exception {
        AppSP settings = AppSP.get();
        String oldPath = settings.lastBookPath;
        String oldOriginal = settings.lastBookOriginalUri;
        int oldPage = settings.lastBookPage;
        int oldParagraph = settings.lastBookParagraph;
        Field reference = TTSService.class.getDeclaredField("serviceRef");
        reference.setAccessible(true);
        Object oldService = reference.get(null);
        AtomicReference<TTSService> created = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> created.set(new TTSService()));
        TTSService live = created.get();
        live.activeBookPath = "/cache/a-v1.epub";
        live.activeBookIdentity = "content://books/document/a";
        try {
            reference.set(null, live);
            settings.lastBookPath = "/cache/b.epub";
            settings.lastBookOriginalUri = "content://books/document/b";
            assertEquals(live.activeBookIdentity, TTSService.bookIdentity());
            assertEquals(live.activeBookIdentity,
                    TTSNotification.originalUriForPath(live.activeBookPath));
            org.junit.Assert.assertNull(TTSNotification.originalUriForPath(settings.lastBookPath));
        } finally {
            reference.set(null, oldService);
            settings.lastBookPath = oldPath;
            settings.lastBookOriginalUri = oldOriginal;
            settings.lastBookPage = oldPage;
            settings.lastBookParagraph = oldParagraph;
        }
    }
    @Test public void progressUsesOriginalSafUriAcrossStagedFiles() {
        AppSP settings = AppSP.get();
        String oldPath = settings.lastBookPath;
        String oldOriginal = settings.lastBookOriginalUri;
        try {
            settings.lastBookPath = "/cache/staged-version-1.epub";
            settings.lastBookOriginalUri = "content://books/wandering-inn-9";
            assertEquals(settings.lastBookOriginalUri, TTSService.bookIdentity());
            settings.lastBookPath = "/cache/staged-version-2.epub";
            assertEquals(settings.lastBookOriginalUri, TTSService.bookIdentity());

            settings.lastBookOriginalUri = null;
            assertEquals(settings.lastBookPath, TTSService.bookIdentity());
        } finally {
            settings.lastBookPath = oldPath;
            settings.lastBookOriginalUri = oldOriginal;
        }
    }

    @Test public void notificationReopensPhysicalFileWithOriginalIdentity() {
        AppSP settings = AppSP.get();
        String oldPath = settings.lastBookPath;
        String oldOriginal = settings.lastBookOriginalUri;
        try {
            settings.lastBookPath = "/cache/staged.epub";
            settings.lastBookOriginalUri = "content://books/wandering-inn-9";
            assertEquals(settings.lastBookOriginalUri,
                    TTSNotification.originalUriForPath(settings.lastBookPath));
            var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            var intent = TTSNotification.readerIntent(context, settings.lastBookPath, 42);
            assertEquals("file:///cache/staged.epub", intent.getDataString());
            assertEquals(settings.lastBookOriginalUri, intent.getStringExtra("SAF_ORIGINAL_URI"));
            assertEquals(41, intent.getIntExtra("page", -1));

            var other = TTSNotification.readerIntent(context, "/books/local.epub", 1);
            org.junit.Assert.assertFalse(other.hasExtra("SAF_ORIGINAL_URI"));
            org.junit.Assert.assertNull(TTSNotification.originalUriForPath("/books/local.epub"));
        } finally {
            settings.lastBookPath = oldPath;
            settings.lastBookOriginalUri = oldOriginal;
        }
    }
}
