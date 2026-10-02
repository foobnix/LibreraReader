package com.foobnix.work;

import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.SafDocumentIdentity;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.junit.Assert.*;

public class SafOverlappingGrantTest {
    @Test public void traversingParentAndChildGrantsPublishesOneBookAndTwoMemberships() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        String namespace = UUID.randomUUID().toString();
        String authority = "overlap.fixture";
        String childId = namespace + ":child";
        String bookId = childId + "/book.epub";
        Uri parentGrant = DocumentsContract.buildTreeDocumentUri(authority, namespace + ":");
        Uri childGrant = DocumentsContract.buildTreeDocumentUri(authority, childId);
        Uri parentFolder = DocumentsContract.buildDocumentUriUsingTree(parentGrant, childId);
        Uri parentBook = DocumentsContract.buildDocumentUriUsingTree(parentGrant, bookId);
        Uri childBook = DocumentsContract.buildDocumentUriUsingTree(childGrant, bookId);
        Map<Uri, List<SafDocuments.Document>> folders = new HashMap<>();
        folders.put(parentGrant, Collections.singletonList(
                new SafDocuments.Document("child", parentFolder, true, null, null)));
        folders.put(parentFolder, Collections.singletonList(
                new SafDocuments.Document("book.epub", parentBook, false, 10L, 20L)));
        folders.put(childGrant, Collections.singletonList(
                new SafDocuments.Document("book.epub", childBook, false, 10L, 20L)));
        List<FileMeta> found = new ArrayList<>();
        Set<String> retainedSelections = new HashSet<>(SearchAllBooksWorker.selectedRoots());
        boolean addedExtension = !ExtUtils.seachExts.contains(".epub");
        if (addedExtension) ExtUtils.seachExts.add(".epub");
        try {
            SafDiscovery.DirectoryListing listing = (folder, stopped) -> folders.get(folder);
            SafDiscovery.collect(parentGrant, found, new HashMap<>(), () -> false, listing);
            SafDiscovery.collect(childGrant, found, new HashMap<>(), () -> false, listing);
            assertEquals(2, found.size());
            String identity = SafDocumentIdentity.canonical(parentBook).toString();
            assertEquals(identity, found.get(0).getPath());
            assertEquals(identity, found.get(1).getPath());
            for (FileMeta row : found) row.setIsSearchBook(true);
            Map<String, Set<String>> membership = new HashMap<>();
            membership.put(parentGrant.toString(), Collections.singleton(identity));
            membership.put(childGrant.toString(), Collections.singleton(identity));
            AppDB.get().reconcileCompletedScan(found,
                    new HashSet<>(Arrays.asList(parentGrant.toString(), childGrant.toString())), membership);
            FileMeta row = AppDB.get().load(identity);
            row.setIsStar(true); row.setIsRecent(true); row.setIsRecentProgress(0.4f);
            AppDB.get().save(row);
            retainedSelections.add(childGrant.toString());
            AppDB.get().reconcileDeselectedRoots(retainedSelections,
                    Collections.singleton(parentGrant.toString()));
            row = AppDB.get().load(identity);
            assertTrue(Boolean.TRUE.equals(row.getIsSearchBook()));
            assertTrue(Boolean.TRUE.equals(row.getIsStar()));
            assertTrue(Boolean.TRUE.equals(row.getIsRecent()));
            assertEquals(0.4f, row.getIsRecentProgress(), 0.001f);
        } finally {
            if (addedExtension) ExtUtils.seachExts.remove(".epub");
            AppDB.get().reconcileCompletedScan(Collections.emptyList(), Collections.emptySet(),
                    Collections.singletonMap(childGrant.toString(), Collections.emptySet()));
            AppDB.get().deleteBy(SafDocumentIdentity.canonical(parentBook).toString());
        }
    }
}
