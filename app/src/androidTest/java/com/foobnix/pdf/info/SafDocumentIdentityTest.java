package com.foobnix.pdf.info;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppProfile;
import com.foobnix.ui2.AppDB;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class SafDocumentIdentityTest {
    @Test public void overlappingTreeGrantsMergeExistingReadingState() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        String id = "books:" + UUID.randomUUID() + "/same.epub";
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(
                DocumentsContract.buildTreeDocumentUri("identity.fixture", "books:"), id);
        Uri child = DocumentsContract.buildDocumentUriUsingTree(
                DocumentsContract.buildTreeDocumentUri("identity.fixture", "books:child"), id);
        String identity = SafDocumentIdentity.canonical(parent).toString();
        assertEquals(identity, SafDocumentIdentity.canonical(child).toString());
        FileMeta legacy = new FileMeta(parent.toString());
        legacy.setTitle("Book"); legacy.setIsStar(true); legacy.setIsRecent(true);
        legacy.setIsRecentProgress(0.6f); legacy.setIsRecentTime(123L);
        AppDB.get().migrateAllSafRows(); // A later import must invalidate this completed migration.
        AppDB.get().saveAll(java.util.Collections.singletonList(legacy));
        assertTrue(Boolean.TRUE.equals(AppDB.get().getAll().stream()
                .filter(row -> parent.toString().equals(row.getPath()))
                .findFirst().orElseThrow(AssertionError::new).getIsStar()));
        AppBook progress = new AppBook(parent.toString()); progress.p = 0.6f; progress.t = 123L;
        SharedBooks.cache.put(ExtUtils.getFileName(parent.toString()), progress);
        try {
            FileMeta merged = AppDB.get().getOrCreate(child.toString());
            assertEquals(identity, merged.getPath());
            assertEquals(identity, AppDB.get().load(parent.toString()).getPath());
            assertEquals(1, AppDB.get().getAll().stream().filter(row -> identity.equals(row.getPath())).count());
            assertFalse(AppDB.get().getAll().stream()
                    .anyMatch(row -> parent.toString().equals(row.getPath())));
            assertTrue(Boolean.TRUE.equals(merged.getIsStar()));
            assertTrue(Boolean.TRUE.equals(merged.getIsRecent()));
            assertEquals(0.6f, merged.getIsRecentProgress(), 0.001f);
            assertEquals(0.6f, SharedBooks.load(identity).p, 0.001f);
            AppBookmark legacyBookmark = new AppBookmark();
            legacyBookmark.path = parent.toString();
            assertEquals(identity, legacyBookmark.getPath());
            legacyBookmark.setPath(child.toString());
            assertEquals(identity, legacyBookmark.getPath());
            // Remembering an address does not create permission or retain an unbounded history.
            assertEquals(java.util.Collections.singletonList(Uri.parse(identity)),
                    SafDocumentIdentity.accessCandidates(context, Uri.parse(identity)));
        } finally {
            SharedBooks.cache.remove(ExtUtils.getFileName(identity));
            AppDB.get().deleteBy(parent.toString()); AppDB.get().deleteBy(child.toString());
            AppDB.get().deleteBy(identity);
        }
    }
}
