package com.foobnix.pdf.info.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;

import org.ebookdroid.droids.foliate.FoliateHostActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Issue #1439: changing the brightness (left edge swipe or the slider) used to overwrite the
 * bluelight filter - a positive value unchecked it, a negative one replaced its strength.
 */
@RunWith(AndroidJUnit4.class)
public class BrightnessFilterDeviceTest {

    private ActivityScenario<FoliateHostActivity> host;
    private AppState saved;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        host = ActivityScenario.launch(FoliateHostActivity.class);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        AppState a = AppState.get();
        saved = new AppState();
        saved.isDayNotInvert = a.isDayNotInvert;
        saved.appBrightness = a.appBrightness;
        saved.isEnableBlueFilter = a.isEnableBlueFilter;
        saved.blueLightAlpha = a.blueLightAlpha;
        saved.blueLightColor = a.blueLightColor;
        saved.isAllowMinBrigthness = a.isAllowMinBrigthness;

        a.isDayNotInvert = true;
        a.appBrightness = AppState.AUTO_BRIGTNESS;
        a.isEnableBlueFilter = true;
        a.blueLightAlpha = 40;
        a.blueLightColor = Color.parseColor("#FFDD00");
        a.isAllowMinBrigthness = false;
    }

    @After
    public void tearDown() {
        AppState a = AppState.get();
        a.isDayNotInvert = saved.isDayNotInvert;
        a.appBrightness = saved.appBrightness;
        a.isEnableBlueFilter = saved.isEnableBlueFilter;
        a.blueLightAlpha = saved.blueLightAlpha;
        a.blueLightColor = saved.blueLightColor;
        a.isAllowMinBrigthness = saved.isAllowMinBrigthness;
        host.close();
    }

    @Test
    public void brightnessKeepsTheFilter() {
        host.onActivity(activity -> {
            Handler h = new Handler(Looper.getMainLooper());
            TextView text = new TextView(activity);
            View overlay = new View(activity);

            // below zero dims, the filter keeps its own strength
            BrightnessHelper.onMessegeBrightness(h, new MessegeBrightness(-3), text, overlay);
            assertTrue(AppState.get().isEnableBlueFilter);
            assertEquals(40, AppState.get().blueLightAlpha);
            assertEquals(-3, AppState.get().appBrightness);
            assertEquals(View.VISIBLE, overlay.getVisibility());

            // back above zero: the filter stays on
            BrightnessHelper.onMessegeBrightness(h, new MessegeBrightness(60), text, overlay);
            assertTrue(AppState.get().isEnableBlueFilter);
            assertEquals(40, AppState.get().blueLightAlpha);
            assertEquals(60, AppState.get().appBrightness);
            assertEquals(View.VISIBLE, overlay.getVisibility());
            assertEquals(64, Color.alpha(color(overlay))); // 160 * 40%

            BrightnessHelper.onMessegeBrightness(h, new MessegeBrightness(AppState.AUTO_BRIGTNESS), text, overlay);
            assertTrue(AppState.get().isEnableBlueFilter);
            assertEquals(40, AppState.get().blueLightAlpha);
            assertEquals(AppState.AUTO_BRIGTNESS, AppState.get().appBrightness);
        });
    }

    @Test
    public void dimmingWithoutFilterIsBlack() {
        host.onActivity(activity -> {
            AppState.get().isEnableBlueFilter = false;
            Handler h = new Handler(Looper.getMainLooper());
            TextView text = new TextView(activity);
            View overlay = new View(activity);

            BrightnessHelper.onMessegeBrightness(h, new MessegeBrightness(-50), text, overlay);
            assertFalse(AppState.get().isEnableBlueFilter);
            assertEquals(View.VISIBLE, overlay.getVisibility());
            assertEquals(Color.argb(80, 0, 0, 0), color(overlay));

            BrightnessHelper.onMessegeBrightness(h, new MessegeBrightness(20), text, overlay);
            assertFalse(AppState.get().isEnableBlueFilter);
            assertEquals(View.GONE, overlay.getVisibility());
        });
    }

    @Test
    public void swipeFromAutoStartsAtSystemBrightness() {
        host.onActivity(activity -> {
            new BrightnessHelper(activity);
            assertEquals(BrightnessHelper.getSystemBrigtnessInt(activity), (int) BrightnessHelper.currentPercent);
        });
    }

    private static int color(View overlay) {
        return ((ColorDrawable) overlay.getBackground()).getColor();
    }
}
