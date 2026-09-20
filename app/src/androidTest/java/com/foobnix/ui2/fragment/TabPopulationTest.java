package com.foobnix.ui2.fragment;

import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.ui2.MainTabs2;
import org.junit.Test;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class TabPopulationTest {
    public static class ControlledFragment extends InitialTabLoadTest.InitialLoadFragment {
        final CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), latest = new CountDownLatch(1);
        final AtomicInteger queries = new AtomicInteger(), publications = new AtomicInteger(), stale = new AtomicInteger();
        boolean failFirst;
        // This fixture tests explicit refreshes and view creation, not app-wide sync broadcasts.
        @Override public void notifyFragment() {}
        @Override public List<String> prepareDataInBackground() {
            int query = queries.incrementAndGet();
            if (query == 1) {
                started.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test did not release query"); }
                catch (InterruptedException stop) { Thread.currentThread().interrupt(); throw new RuntimeException(stop); }
                if (failFirst) throw new IllegalStateException("database temporarily unavailable");
            }
            return Collections.singletonList(query == 1 ? "old" : "new");
        }
        @Override public void populateDataInUI(List<String> items) {
            publications.incrementAndGet();
            if (items.contains("old")) stale.incrementAndGet();
            if (items.contains("new")) latest.countDown();
        }
    }
    private MainTabs2 attach(ControlledFragment fragment) throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        MainTabs2 activity = (MainTabs2) instrumentation.startActivitySync(new Intent(
                instrumentation.getTargetContext(), MainTabs2.class).putExtra(MainTabs2.EXTRA_SHOW_TABS, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        instrumentation.runOnMainSync(() -> activity.getSupportFragmentManager().beginTransaction()
                .add(fragment, "population-regression").commitNowAllowingStateLoss());
        assertTrue(fragment.started.await(5, TimeUnit.SECONDS)); return activity;
    }
    private void remove(MainTabs2 activity, ControlledFragment fragment) {
        fragment.release.countDown();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            if (fragment.isAdded()) activity.getSupportFragmentManager().beginTransaction().remove(fragment).commitNowAllowingStateLoss();
        });
    }
    @Test public void rapidRefreshesDiscardStaleResultsAndCoalesceToLatestQuery() throws Exception {
        ControlledFragment fragment = new ControlledFragment(); MainTabs2 activity = attach(fragment);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (int i=0; i<100; i++) fragment.populate();
            });
            fragment.release.countDown(); assertTrue(fragment.latest.await(5, TimeUnit.SECONDS));
            assertEquals(2, fragment.queries.get()); assertEquals(1, fragment.publications.get()); assertEquals(0, fragment.stale.get());
        } finally { remove(activity, fragment); }
    }
    @Test public void viewRecreationDuringQueryDoesNotPublishOldViewData() throws Exception {
        ControlledFragment fragment = new ControlledFragment(); MainTabs2 activity = attach(fragment);
        try {
            var instrumentation = InstrumentationRegistry.getInstrumentation();
            instrumentation.runOnMainSync(() -> activity.getSupportFragmentManager().beginTransaction().detach(fragment).commitNowAllowingStateLoss());
            instrumentation.runOnMainSync(() -> activity.getSupportFragmentManager().beginTransaction().attach(fragment).commitNowAllowingStateLoss());
            fragment.release.countDown(); assertTrue(fragment.latest.await(5, TimeUnit.SECONDS));
            assertEquals(0, fragment.stale.get()); assertEquals(1, fragment.publications.get());
        } finally { remove(activity, fragment); }
    }
    @Test public void failedQueryDoesNotLoseQueuedRefresh() throws Exception {
        ControlledFragment fragment = new ControlledFragment(); fragment.failFirst = true;
        MainTabs2 activity = attach(fragment);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(fragment::populate);
            fragment.release.countDown(); assertTrue(fragment.latest.await(5, TimeUnit.SECONDS));
            assertEquals(2, fragment.queries.get()); assertEquals(1, fragment.publications.get());
        } finally { remove(activity, fragment); }
    }
}
