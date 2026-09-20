package com.foobnix.tts;

import com.foobnix.model.AppSP;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** A second reader must not change which book the live service is speaking. */
public class TTSReaderOwnershipTest {
    private static TTSService service() {
        java.util.concurrent.atomic.AtomicReference<TTSService> result =
                new java.util.concurrent.atomic.AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> result.set(new TTSService()));
        return result.get();
    }
    @Test public void pausedOrPlayingAThenOpeningBStartsBInsteadOfControllingA() {
        String a = "/books/a.epub";
        String b = "/books/b.epub";
        AppSP settings = AppSP.get();
        String oldPath = settings.lastBookPath;
        try {
            settings.lastBookPath = b; // DocumentController.setCurrentBook(B)
            assertEquals(TTSService.ReaderTtsAction.START_BOOK,
                    TTSService.readerAction(b, a, false, false));
            assertEquals(TTSService.ReaderTtsAction.START_BOOK,
                    TTSService.readerAction(b, a, true, false));
            assertEquals(TTSService.ReaderTtsAction.RESUME,
                    TTSService.readerAction(a, a, false, false));
            assertEquals(TTSService.ReaderTtsAction.PAUSE,
                    TTSService.readerAction(a, a, true, false));
        } finally {
            settings.lastBookPath = oldPath;
        }
    }

    @Test public void resumingARecoversServicePositionAfterReaderBChangedGlobalState() {
        AppSP settings = AppSP.get();
        String oldPath = settings.lastBookPath;
        int oldPage = settings.lastBookPage;
        int oldParagraph = settings.lastBookParagraph;
        try {
            TTSService live = service();
            live.activeBookPath = "/books/a.epub";
            live.activePage = 12;
            live.activeParagraph = 4;
            settings.lastBookPath = "/books/b.epub";
            settings.lastBookPage = 1;
            settings.lastBookParagraph = 0;
            live.restoreActivePosition();
            assertEquals("/books/a.epub", settings.lastBookPath);
            assertEquals(12, settings.lastBookPage);
            assertEquals(4, settings.lastBookParagraph);
        } finally {
            settings.lastBookPath = oldPath;
            settings.lastBookPage = oldPage;
            settings.lastBookParagraph = oldParagraph;
        }
    }

    @Test public void restartedServiceCanResumePersistedBookBeforeItHasAnActiveSnapshot() {
        AppSP settings = AppSP.get();
        String oldPath = settings.lastBookPath;
        int oldPage = settings.lastBookPage;
        int oldParagraph = settings.lastBookParagraph;
        try {
            settings.lastBookPath = "/books/persisted.epub";
            settings.lastBookPage = 7;
            settings.lastBookParagraph = 2;
            TTSService fresh = service();
            fresh.restoreActivePosition();
            assertEquals(settings.lastBookPath, fresh.activeBookPath);
            assertEquals(7, fresh.activePage);
            assertEquals(2, fresh.activeParagraph);
        } finally {
            settings.lastBookPath = oldPath;
            settings.lastBookPage = oldPage;
            settings.lastBookParagraph = oldParagraph;
        }
    }
}
