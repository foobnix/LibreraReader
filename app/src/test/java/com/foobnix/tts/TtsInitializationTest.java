package com.foobnix.tts;

import android.content.Context;
import android.content.res.Resources;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TtsInitializationTest {
    @Test
    public void pendingSpeechFollowsTheCurrentEngineLifecycle() throws Exception {
        Context originalContext = LibreraApp.context;
        // Android's JVM stubs omit device values needed by AppState's static initializer.
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        java.lang.reflect.Field model = android.os.Build.class.getField("MODEL");
        Object modelBase = unsafeClass.getMethod("staticFieldBase", java.lang.reflect.Field.class).invoke(unsafe, model);
        long modelOffset = (long) unsafeClass.getMethod("staticFieldOffset", java.lang.reflect.Field.class).invoke(unsafe, model);
        java.lang.reflect.Method put = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
        Object originalModel = android.os.Build.MODEL;
        put.invoke(unsafe, modelBase, modelOffset, "test");
        java.io.File originalDownloads = com.foobnix.model.AppProfile.DOWNLOADS_DIR;
        com.foobnix.model.AppProfile.DOWNLOADS_DIR = new java.io.File(".");
        List<TextToSpeech.OnInitListener> callbacks = new ArrayList<>();
        try (MockedStatic<Resources> resources = mockStatic(Resources.class)) {
            Resources system = mock(Resources.class);
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.density = metrics.scaledDensity = 1;
            metrics.widthPixels = 1080;
            metrics.heightPixels = 2160;
            when(system.getDisplayMetrics()).thenReturn(metrics);
            when(system.getConfiguration()).thenReturn(new android.content.res.Configuration());
            resources.when(Resources::getSystem).thenReturn(system);
        try (MockedStatic<LOG> logs = mockStatic(LOG.class);
             MockedStatic<TTSEngine> singleton = mockStatic(TTSEngine.class);
             MockedStatic<AppState> state = mockStatic(AppState.class);
             MockedStatic<AppSP> preferences = mockStatic(AppSP.class);
             MockedConstruction<TextToSpeech> engines = mockConstruction(TextToSpeech.class,
                     (engine, context) -> callbacks.add((TextToSpeech.OnInitListener) context.arguments().get(1)))) {
            LibreraApp.context = mock(Context.class);
            AppState settings = mock(AppState.class);
            settings.allowOtherMusic = true;
            settings.ttsSpeed = 1;
            state.when(AppState::get).thenReturn(settings);
            preferences.when(AppSP::get).thenReturn(mock(AppSP.class));
            TTSEngine engine = spy(new TTSEngine());
            doReturn(false).when(engine).isMp3();
            singleton.when(TTSEngine::get).thenReturn(engine);
            engine.listener = status -> {};

            engine.getTTS(); // The popup creates the engine before Play.
            engine.speek("first");
            engine.speek("latest");
            TextToSpeech first = engines.constructed().get(0);
            verify(first, never()).speak(anyString(), anyInt(), any());
            callbacks.get(0).onInit(TextToSpeech.SUCCESS);
            verify(first).speak(eq("latest"), eq(TextToSpeech.QUEUE_FLUSH), any());

            engine.shutdown();
            engine.speek("cancelled");
            TextToSpeech second = engines.constructed().get(1);
            engine.stop();
            callbacks.get(1).onInit(TextToSpeech.SUCCESS);
            verify(second, never()).speak(anyString(), anyInt(), any());

            engine.shutdown();
            engine.speek("failed");
            callbacks.get(2).onInit(TextToSpeech.ERROR);
            engine.speek("retry");
            assertEquals(4, engines.constructed().size());
            verify(engines.constructed().get(2)).shutdown();
            callbacks.get(2).onInit(TextToSpeech.SUCCESS); // Obsolete engine must not release the new request.
            TextToSpeech fourth = engines.constructed().get(3);
            verify(fourth, never()).speak(anyString(), anyInt(), any());
            callbacks.get(3).onInit(TextToSpeech.SUCCESS);
            verify(fourth).speak(eq("retry"), eq(TextToSpeech.QUEUE_FLUSH), any());

            engine.shutdown();
            engine.getTTS(status -> { throw new IllegalStateException("UI callback"); });
            engine.speek("survives callback failure");
            assertThrows(IllegalStateException.class, () -> callbacks.get(4).onInit(TextToSpeech.SUCCESS));
            verify(engines.constructed().get(4)).speak(eq("survives callback failure"), anyInt(), any());
            engine.shutdown();
        }
        } finally {
            LibreraApp.context = originalContext;
            put.invoke(unsafe, modelBase, modelOffset, originalModel);
            com.foobnix.model.AppProfile.DOWNLOADS_DIR = originalDownloads;
        }
    }
}
