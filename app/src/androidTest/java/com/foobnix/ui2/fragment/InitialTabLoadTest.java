package com.foobnix.ui2.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.core.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.pdf.info.R;
import com.foobnix.ui2.MainTabs2;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class InitialTabLoadTest {
    /** Recents and other tabs request their first population before onCreateView returns. */
    @Test public void initialRequestSurvivesViewCreation() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), MainTabs2.class)
                .putExtra(MainTabs2.EXTRA_SHOW_TABS, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        MainTabs2 activity = (MainTabs2) instrumentation.startActivitySync(intent);
        InitialLoadFragment fragment = new InitialLoadFragment();
        try {
            instrumentation.runOnMainSync(() -> activity.getSupportFragmentManager()
                    .beginTransaction().add(fragment, "initial-load-regression").commitNowAllowingStateLoss());
            assertTrue("Initial population was discarded", fragment.loaded.await(5, TimeUnit.SECONDS));
        } finally {
            instrumentation.runOnMainSync(() -> activity.getSupportFragmentManager()
                    .beginTransaction().remove(fragment).commitNowAllowingStateLoss());
        }
    }

    public static class InitialLoadFragment extends UIFragment<String> {
        final CountDownLatch loaded = new CountDownLatch(1);
        @Override public void notifyFragment() { populate(); }
        @Override public void resetFragment() {}
        @Override public Pair<Integer, Integer> getNameAndIconRes() {
            return new Pair<>(R.string.recent, R.drawable.glyphicons_422_book_library);
        }
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
            View view = new FrameLayout(requireContext());
            populate();
            return view;
        }
        @Override public List<String> prepareDataInBackground() {
            return Collections.singletonList("ready");
        }
        @Override public void populateDataInUI(List<String> items) {
            if (items.equals(Collections.singletonList("ready"))) loaded.countDown();
        }
    }
}
