package com.foobnix.work;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataBackoffTest {
    @Test public void failedBookRetriesAfterBackoffOrSourceChange() {
        String failure = MetadataRefreshPolicy.failedRevision("old", 1000);
        assertFalse(MetadataRefreshPolicy.retryDue(failure, "old", 2000));
        assertTrue(MetadataRefreshPolicy.retryDue(failure, "new", 2000));
        assertTrue(MetadataRefreshPolicy.retryDue(failure, "old", 601000));
        assertTrue(MetadataRefreshPolicy.retryDue("corrupt", "old", 2000));
    }
    @Test public void unknownRevisionUsesTimeBackoffWithoutConfusingLiteralNull() {
        String failure = MetadataRefreshPolicy.failedRevision(null, 1000);
        assertFalse(MetadataRefreshPolicy.retryDue(failure, null, 2000));
        assertTrue(MetadataRefreshPolicy.retryDue(failure, null, 601000));
        assertTrue(MetadataRefreshPolicy.retryDue(failure, "null", 2000));
        assertTrue(MetadataRefreshPolicy.retryDue(failure, "known", 2000));
    }
    @Test public void repeatedFailuresIncreaseDelayAndChangedRevisionResetsIt() {
        String first = MetadataRefreshPolicy.failedRevision("old", 0);
        String second = MetadataRefreshPolicy.failedRevision(first, "old", 600000);
        assertFalse(MetadataRefreshPolicy.retryDue(second, "old", 1200000));
        assertTrue(MetadataRefreshPolicy.retryDue(second, "old", 1800000));
        String changed = MetadataRefreshPolicy.failedRevision(second, "new", 1200000);
        assertTrue(MetadataRefreshPolicy.retryDue(changed, "new", 1800000));
        String last = null;
        for (int i = 0; i < 30; i++) last = MetadataRefreshPolicy.failedRevision(last, null, 0);
        assertFalse(MetadataRefreshPolicy.retryDue(last, null, 24 * 60 * 60 * 1000L - 1));
        assertTrue(MetadataRefreshPolicy.retryDue(last, null, 24 * 60 * 60 * 1000L));
    }

}
