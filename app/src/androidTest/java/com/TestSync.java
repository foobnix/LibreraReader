package com;

import android.os.Environment;
import com.foobnix.model.MyPath;
import com.foobnix.model.SimpleMeta;
import org.junit.Test;
import static org.junit.Assert.*;

/** MyPath uses Android storage APIs, so these tests belong on Android rather than the JVM. */
public class TestSync {
    @Test public void relativeExternalStoragePathRoundTrips() {
        String path = Environment.getExternalStorageDirectory() + "/Books/Book.epub";
        assertEquals("internal-storage:/Books/Book.epub", MyPath.toRelative(path));
        assertEquals(path, MyPath.toAbsolute(MyPath.toRelative(path)));
    }
    @Test public void unrelatedAndNullPathsRemainUnchanged() {
        assertEquals("1", MyPath.toAbsolute("1"));
        assertNull(MyPath.toRelative(null)); assertNull(MyPath.toAbsolute(null));
    }
    @Test public void simpleMetadataPreservesPathAndTimestamp() {
        String path = Environment.getExternalStorageDirectory() + "/Books/Book.epub";
        SimpleMeta meta = new SimpleMeta(path, 1234);
        assertEquals(path, meta.getPath()); assertEquals(1234, meta.time);
        assertTrue(meta.path.startsWith(MyPath.INTERNAL_PREFIX));
    }
    @Test public void metadataIdentityDependsOnPathRatherThanSyncTimestamp() {
        SimpleMeta older = new SimpleMeta("book.epub", 2), newer = new SimpleMeta("book.epub", 3);
        assertEquals(older, newer); assertEquals(older.hashCode(), newer.hashCode());
        assertNotEquals(older, new SimpleMeta("different.epub", 2));
    }
}
