package com.foobnix.pdf.info;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.android.utils.Objects;
import com.foobnix.model.AppSP;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class ReaderRestartStateTest {
    private SharedPreferences preferences;
    private Context context;
    private String preferencesName;
    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        preferencesName = "restart-fixture-" + UUID.randomUUID();
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
    }
    @After public void tearDown() { context.deleteSharedPreferences(preferencesName); }
    @Test public void originalIdentityAndPhysicalFileSurviveSettingsSerialization() {
        AppSP saved = new AppSP(); saved.lastBookPath = "/cache/source-book.pdf";
        saved.lastBookOriginalUri = "content://fixture/book";
        Objects.saveToSP(saved, preferences);
        AppSP restored = new AppSP(); Objects.loadFromSp(restored, preferences);
        assertEquals(saved.lastBookPath, restored.lastBookPath); assertEquals(saved.lastBookOriginalUri, restored.lastBookOriginalUri);
    }
    @Test public void switchingToLocalBookRemovesPreviouslyPersistedSafIdentity() {
        AppSP saved = new AppSP(); saved.lastBookOriginalUri = "content://fixture/book"; Objects.saveToSP(saved, preferences);
        saved.lastBookOriginalUri = null; saved.lastBookPath = "/books/Local.pdf"; Objects.saveToSP(saved, preferences);
        AppSP restored = new AppSP(); Objects.loadFromSp(restored, preferences);
        assertNull(restored.lastBookOriginalUri); assertEquals("/books/Local.pdf", restored.lastBookPath);
    }
    @Test public void oldSettingsWithoutOriginalUriKeepLocalRestartBehavior() {
        preferences.edit().putString("lastBookPath", "/books/Local.pdf").commit();
        AppSP restored = new AppSP(); Objects.loadFromSp(restored, preferences);
        assertNull(restored.lastBookOriginalUri); assertEquals("/books/Local.pdf", restored.lastBookPath);
    }
}
