package com.foobnix.tts;

import android.speech.tts.TextToSpeech;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SpeechQueueTest {
    @Test public void nextCommandQueuesExactlyOneCompletionAndNoFollowingText() {
        TextToSpeech tts = mock(TextToSpeech.class);
        SpeechRequest request = new SpeechRequest(0);
        assertTrue(SpeechQueue.enqueue(tts, "hello ttsPAUSEttsNEXTttsPAUSEmust not speak", true, 50, 0, request));
        verify(tts, times(1)).playSilence(eq(0L), eq(TextToSpeech.QUEUE_ADD),
                argThat(p -> request.id(TTSEngine.UTTERANCE_ID_DONE)
                        .equals(p.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))));
        verify(tts, times(1)).speak(eq("hello"), eq(TextToSpeech.QUEUE_ADD), any());
        verify(tts, never()).speak(contains("must not speak"), anyInt(), any());
    }

    @Test public void rejectedSpeechIsReportedToPlaybackOwner() {
        TextToSpeech tts = mock(TextToSpeech.class);
        when(tts.speak(anyString(), anyInt(), any())).thenReturn(TextToSpeech.ERROR);
        assertFalse(SpeechQueue.enqueue(tts, "hello", true, 50, 0, new SpeechRequest(0)));
        verify(tts, times(1)).speak(anyString(), anyInt(), any());
    }

    @Test public void rejectedSilenceIsReportedToPlaybackOwner() {
        TextToSpeech tts = mock(TextToSpeech.class);
        when(tts.playSilence(anyLong(), anyInt(), any())).thenReturn(TextToSpeech.ERROR);
        assertFalse(SpeechQueue.enqueue(tts, "hello ttsPAUSEworld", false, 50, 0, new SpeechRequest(0)));
    }

    @Test public void splitKeepsSentenceIntactAndCompletionOnFinalChunk() {
        TextToSpeech tts = mock(TextToSpeech.class);
        SpeechRequest request = new SpeechRequest(8);
        assertTrue(SpeechQueue.enqueue(tts, "Carried sentence ends. More text.", true, 50, 0, request));
        verify(tts).speak(eq("Carried sentence ends."), eq(TextToSpeech.QUEUE_ADD),
                argThat(p -> request.id("Boundary" + TTSEngine.UTTERANCE_ID_DONE)
                        .equals(p.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))));
        verify(tts).speak(eq(" More text."), eq(TextToSpeech.QUEUE_ADD),
                argThat(p -> request.id(TTSEngine.UTTERANCE_ID_DONE)
                        .equals(p.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))));
    }

    @Test public void boundaryOffsetsAccountForRemovedPauseMarkers() {
        TextToSpeech tts = mock(TextToSpeech.class);
        String text = "oldttsPAUSE new.";
        SpeechRequest request = new SpeechRequest(text.indexOf("new"));
        assertTrue(SpeechQueue.enqueue(tts, text, true, 0, 0, request));
        verify(tts).speak(eq("old new."), eq(TextToSpeech.QUEUE_ADD), any());
        assertFalse(request.reachesPage(TTSEngine.UTTERANCE_ID_DONE, 4));
        assertTrue(request.reachesPage(TTSEngine.UTTERANCE_ID_DONE, 7));
    }

    @Test public void trimmedParagraphOffsetsAndResumePreserveTheBoundary() {
        TextToSpeech tts = mock(TextToSpeech.class);
        String text = "earlierttsPAUSE  carried new.";
        SpeechRequest request = new SpeechRequest(text.indexOf("new"));
        assertTrue(SpeechQueue.enqueue(tts, text, true, 50, 1, request));
        verify(tts, never()).speak(eq("earlier"), anyInt(), any());
        verify(tts).speak(eq("carried new."), eq(TextToSpeech.QUEUE_ADD), any());
        assertFalse(request.reachesPage("Finished1", 8));
        assertTrue(request.reachesPage("Finished1", 11));
    }

}
