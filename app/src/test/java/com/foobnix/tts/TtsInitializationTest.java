package com.foobnix.tts;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.DisplayMetrics;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Android initializes TextToSpeech asynchronously, so everything the engine is asked to do
 * between construction and onInit has to wait for it.
 */
public class TtsInitializationTest {

    @Test public void theLatestRequestIsKeptUntilInitializationSucceeds() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            tts.getTTS(); // The popup creates the engine before Play is pressed.
            tts.speek("first");
            tts.speek("latest");

            TextToSpeech engine = engines.constructed().get(0);
            verify(engine, never()).speak(anyString(), anyInt(), any());

            callbacks.get(0).onInit(TextToSpeech.SUCCESS);
            verify(engine).speak(eq("latest"), eq(TextToSpeech.QUEUE_FLUSH), any());
        });
    }

    @Test public void stopDropsTheQueuedRequest() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            tts.getTTS();
            tts.speek("cancelled");
            tts.stop();

            callbacks.get(0).onInit(TextToSpeech.SUCCESS);
            verify(engines.constructed().get(0), never()).speak(anyString(), anyInt(), any());
        });
    }

    @Test public void aRestartedEngineIgnoresTheCallbackOfTheOldOne() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            tts.getTTS();
            tts.shutdown();
            tts.speek("after restart");
            assertEquals(2, engines.constructed().size());
            TextToSpeech restarted = engines.constructed().get(1);

            callbacks.get(0).onInit(TextToSpeech.SUCCESS);
            assertFalse(tts.isInitialized());
            verify(restarted, never()).speak(anyString(), anyInt(), any());

            callbacks.get(1).onInit(TextToSpeech.SUCCESS);
            verify(restarted).speak(eq("after restart"), eq(TextToSpeech.QUEUE_FLUSH), any());
        });
    }

    @Test public void anEngineThatFailedToInitializeIsReplacedOnTheNextRequest() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            tts.getTTS();
            tts.speek("failed");
            callbacks.get(0).onInit(TextToSpeech.ERROR);

            tts.speek("retry");
            assertEquals(2, engines.constructed().size());
            verify(engines.constructed().get(0)).shutdown();

            TextToSpeech replacement = engines.constructed().get(1);
            callbacks.get(0).onInit(TextToSpeech.SUCCESS); // The replaced engine owns nothing now.
            verify(replacement, never()).speak(anyString(), anyInt(), any());

            callbacks.get(1).onInit(TextToSpeech.SUCCESS);
            verify(replacement).speak(eq("retry"), eq(TextToSpeech.QUEUE_FLUSH), any());
        });
    }

    @Test public void queuedSpeechSurvivesAFailingInitListener() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            tts.getTTS(status -> {
                throw new IllegalStateException("UI callback");
            });
            tts.speek("survives callback failure");

            callbacks.get(0).onInit(TextToSpeech.SUCCESS);
            verify(engines.constructed().get(0)).speak(eq("survives callback failure"), anyInt(), any());
        });
    }

    @Test public void aListenerArrivingWhileTheEngineConnectsIsStillNotified() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            tts.getTTS(); // Playback created the engine.
            AtomicInteger reported = new AtomicInteger(Integer.MIN_VALUE);
            tts.getTTS(reported::set); // The settings popup opens while it connects.
            assertEquals(1, engines.constructed().size());

            callbacks.get(0).onInit(TextToSpeech.SUCCESS);
            assertEquals(TextToSpeech.SUCCESS, reported.get());
        });
    }

    @Test public void aLanguageIsOnlyJudgedOnceTheEngineIsInitialized() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            TextToSpeech engine = tts.getTTS();
            when(engine.getLanguage()).thenReturn(new Locale("eng", "USA", ""));
            when(engine.isLanguageAvailable(any())).thenReturn(TextToSpeech.LANG_NOT_SUPPORTED);

            assertFalse(tts.isInitialized());
            assertFalse("still connecting - the language is not known yet", tts.hasUnknownLanguage());

            callbacks.get(0).onInit(TextToSpeech.SUCCESS);
            assertTrue(tts.isInitialized());
            assertTrue("the engine cannot speak the language it reports", tts.hasUnknownLanguage());
        });
    }

    @Test public void anAvailableLanguageNeedsNoSettings() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            TextToSpeech engine = tts.getTTS();
            when(engine.getLanguage()).thenReturn(new Locale("deu", "DEU", ""));
            when(engine.isLanguageAvailable(any())).thenReturn(TextToSpeech.LANG_COUNTRY_AVAILABLE);
            callbacks.get(0).onInit(TextToSpeech.SUCCESS);

            assertFalse(tts.hasUnknownLanguage());
        });
    }

    @Test public void aLanguageTheEngineCannotSpeakReadsAsNoLanguage() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            TextToSpeech engine = tts.getTTS();
            when(engine.getLanguage()).thenReturn(new Locale("eng", "USA", ""));
            when(engine.isLanguageAvailable(any())).thenReturn(TextToSpeech.LANG_MISSING_DATA);
            callbacks.get(0).onInit(TextToSpeech.SUCCESS);

            assertEquals(TTSEngine.UNKNOWN, tts.getCurrentLang());
        });
    }

    @Test public void anEngineWithoutAnyLanguageNeedsSettings() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            TextToSpeech engine = tts.getTTS();
            when(engine.getLanguage()).thenReturn(new Locale("", "", ""));
            callbacks.get(0).onInit(TextToSpeech.SUCCESS);

            assertTrue(tts.hasUnknownLanguage());
            verify(engine, never()).isLanguageAvailable(any());
        });
    }

    @Test public void anEngineOutsideTheSystemImageIsNamed() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            // A phone without Google's engine: the only one is user-installed and no default
            // is set, so Android's getDefaultEngine() has nothing to report.
            installEngines("org.woheller69.ttsengine");
            TextToSpeech engine = tts.getTTS();
            TextToSpeech.EngineInfo sherpa = new TextToSpeech.EngineInfo();
            sherpa.name = "org.woheller69.ttsengine";
            sherpa.label = "SherpaTTS";
            List<TextToSpeech.EngineInfo> installed = new ArrayList<TextToSpeech.EngineInfo>();
            installed.add(sherpa);
            when(engine.getEngines()).thenReturn(installed);
            callbacks.get(0).onInit(TextToSpeech.SUCCESS);

            assertEquals("SherpaTTS", tts.getCurrentEngineName());
        });
    }

    @Test public void noEnginesIsKnownBeforeTheEngineIsCreated() throws Exception {
        withTtsEngine((tts, callbacks, engines) -> {
            installEngines();
            assertTrue(tts.hasNoEngines());
            assertEquals(0, engines.constructed().size());

            installEngines("com.reecedunn.espeak");
            assertFalse(tts.hasNoEngines());
        });
    }

    private static void installEngines(String... packages) {
        List<ResolveInfo> services = new ArrayList<ResolveInfo>();
        for (String name : packages) {
            ResolveInfo info = new ResolveInfo();
            info.serviceInfo = new ServiceInfo();
            info.serviceInfo.packageName = name;
            services.add(info);
        }
        PackageManager pm = mock(PackageManager.class);
        when(pm.queryIntentServices(any(), anyInt())).thenReturn(services);
        when(LibreraApp.context.getPackageManager()).thenReturn(pm);
    }

    private interface Scenario {
        void run(TTSEngine tts, List<OnInitListener> callbacks, MockedConstruction<TextToSpeech> engines);
    }

    /**
     * Runs the scenario against a TTSEngine whose TextToSpeech instances are mocks, collecting
     * the OnInitListener each of them was constructed with so the test can decide when - and
     * whether - initialization completes.
     */
    private void withTtsEngine(Scenario scenario) {
        final Context originalContext = LibreraApp.context;
        final File originalDownloads = AppProfile.DOWNLOADS_DIR;
        // Nothing of the device is behind Android's JVM stubs, but the settings classes
        // loaded below read it while initializing.
        AppProfile.DOWNLOADS_DIR = new File(".");
        final List<OnInitListener> callbacks = new ArrayList<OnInitListener>();
        final Resources system = systemResources();
        try (MockedStatic<Resources> resources = mockStatic(Resources.class)) {
            resources.when(Resources::getSystem).thenReturn(system);
            try (MockedStatic<LOG> logs = mockStatic(LOG.class);
                 MockedStatic<TTSEngine> singleton = mockStatic(TTSEngine.class, CALLS_REAL_METHODS);
                 MockedStatic<AppState> state = mockStatic(AppState.class);
                 MockedStatic<AppSP> preferences = mockStatic(AppSP.class);
                 MockedConstruction<TextToSpeech> engines = mockConstruction(TextToSpeech.class,
                         (engine, context) -> callbacks.add((OnInitListener) context.arguments().get(1)))) {
                LibreraApp.context = mock(Context.class);
                AppState settings = mock(AppState.class);
                settings.allowOtherMusic = true;
                settings.ttsSpeed = 1;
                state.when(AppState::get).thenReturn(settings);
                preferences.when(AppSP::get).thenReturn(mock(AppSP.class));

                TTSEngine tts = spy(new TTSEngine());
                doReturn(false).when(tts).isMp3();
                singleton.when(TTSEngine::get).thenReturn(tts);
                tts.listener = status -> {
                };
                try {
                    scenario.run(tts, callbacks, engines);
                } finally {
                    tts.shutdown();
                }
            }
        } finally {
            LibreraApp.context = originalContext;
            AppProfile.DOWNLOADS_DIR = originalDownloads;
        }
    }

    private Resources systemResources() {
        Resources system = mock(Resources.class);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = metrics.scaledDensity = 1;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2160;
        when(system.getDisplayMetrics()).thenReturn(metrics);
        when(system.getConfiguration()).thenReturn(new Configuration());
        return system;
    }
}
