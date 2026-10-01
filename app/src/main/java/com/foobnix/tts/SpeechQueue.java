package com.foobnix.tts;

import android.speech.tts.TextToSpeech;
import com.foobnix.android.utils.TxtUtils;
import java.util.HashMap;

/** Submits a page without depending on mutable reader or application settings. */
final class SpeechQueue {
    static boolean enqueue(TextToSpeech tts, String text, boolean continuing, int pause,
                           int startParagraph, SpeechRequest request) {
        try {
            if (pause > 0 && text.contains(TxtUtils.TTS_PAUSE)) {
                if (!continuing) silence(tts, 0, TextToSpeech.QUEUE_FLUSH, request, "Temp");
                String[] parts = text.split(TxtUtils.TTS_PAUSE);
                int offset = 0;
                for (int i = 0; i < parts.length; i++) {
                    int partOffset = offset;
                    offset += parts[i].length() + TxtUtils.TTS_PAUSE.length();
                    if (i < startParagraph) continue;
                    String part = parts[i].trim();
                    if (part.isEmpty() || part.contains(TxtUtils.TTS_SKIP)
                            || (part.length() == 1 && !Character.isLetterOrDigit(part.charAt(0)))) continue;
                    if (part.contains(TxtUtils.TTS_STOP)) {
                        silence(tts, pause, TextToSpeech.QUEUE_ADD, request, TTSEngine.STOP_SIGNAL);
                    }
                    // The single completion below also handles explicit next-page commands.
                    if (part.contains(TxtUtils.TTS_NEXT)) break;
                    int boundary = request.pageBoundary - partOffset - parts[i].indexOf(part);
                    speak(tts, part, TextToSpeech.QUEUE_ADD, request,
                            TTSEngine.FINISHED_SIGNAL + i, boundary);
                    silence(tts, pause, TextToSpeech.QUEUE_ADD, request, "Temp");
                }
                silence(tts, 0, TextToSpeech.QUEUE_ADD, request, TTSEngine.UTTERANCE_ID_DONE);
            } else {
                String spoken = text.replace(TxtUtils.TTS_PAUSE, "");
                int boundary = text.substring(0, request.pageBoundary)
                        .replace(TxtUtils.TTS_PAUSE, "").length();
                speak(tts, spoken, continuing ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH,
                        request, TTSEngine.UTTERANCE_ID_DONE, boundary);
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void speak(TextToSpeech tts, String text, int mode, SpeechRequest request,
                              String signal, int boundary) {
        int end = boundary > 0 && boundary < text.length()
                ? SpeechRequest.firstSentenceEnd(text, boundary) : text.length();
        if (end < text.length()) {
            String boundarySignal = "Boundary" + signal;
            request.register(boundarySignal, boundary, end);
            check(tts.speak(text.substring(0, end), mode, params(request, boundarySignal)));
            // The remaining chunk starts entirely on the new page.
            request.register(signal, 0, text.length() - end);
            check(tts.speak(text.substring(end), TextToSpeech.QUEUE_ADD, params(request, signal)));
        } else {
            request.register(signal, boundary, text.length());
            check(tts.speak(text, mode, params(request, signal)));
        }
    }

    private static void silence(TextToSpeech tts, long duration, int mode,
                                SpeechRequest request, String signal) {
        check(tts.playSilence(duration, mode, params(request, signal)));
    }

    private static HashMap<String, String> params(SpeechRequest request, String signal) {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, request.id(signal));
        return params;
    }

    private static void check(int result) {
        if (result == TextToSpeech.ERROR) throw new IllegalStateException("TTS rejected speech request");
    }
}
