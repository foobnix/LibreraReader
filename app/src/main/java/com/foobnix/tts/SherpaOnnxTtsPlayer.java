package com.foobnix.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SherpaOnnxTtsPlayer {

    public interface Listener {
        void onDone(String utteranceId);
        void onError(String utteranceId, Exception error);
    }

    private static final String TAG = "SherpaOnnxTtsPlayer";
    private static final String MODEL_DIR = "matcha-icefall-zh-baker";
    private static final String ACOUSTIC_MODEL = MODEL_DIR + "/model-steps-3.onnx";
    private static final String VOCODER = "vocos-22khz-univ.onnx";
    private static final String LEXICON = MODEL_DIR + "/lexicon.txt";
    private static final String TOKENS = MODEL_DIR + "/tokens.txt";
    private static final String RULE_FSTS =
            MODEL_DIR + "/phone.fst," + MODEL_DIR + "/date.fst," + MODEL_DIR + "/number.fst";
    private static final int MAX_CHUNK_LENGTH = 80;
    private static final int PREFETCH_CHUNKS = 2;
    private OfflineTts tts;
    private AudioTrack track;
    private Thread worker;
    private volatile boolean stopped;
    private volatile boolean playing;

    private static class AudioChunk {
        final GeneratedAudio audio;
        final long pauseAfterMs;
        final Exception error;
        final boolean end;

        AudioChunk(GeneratedAudio audio, long pauseAfterMs) {
            this.audio = audio;
            this.pauseAfterMs = pauseAfterMs;
            this.error = null;
            this.end = false;
        }

        AudioChunk(Exception error) {
            this.audio = null;
            this.pauseAfterMs = 0;
            this.error = error;
            this.end = false;
        }

        private AudioChunk() {
            this.audio = null;
            this.pauseAfterMs = 0;
            this.error = null;
            this.end = true;
        }

        static AudioChunk end() {
            return new AudioChunk();
        }
    }

    private static class TextChunk {
        final String text;
        final long pauseAfterMs;

        TextChunk(String text, long pauseAfterMs) {
            this.text = text;
            this.pauseAfterMs = pauseAfterMs;
        }
    }

    public synchronized void speak(final Context context, final String text, final Listener listener) {
        stop();
        stopped = false;
        playing = true;
        worker = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    OfflineTts engine = getTts(context);
                    List<TextChunk> chunks = splitForLowLatency(normalize(text));
                    BlockingQueue<AudioChunk> queue = new ArrayBlockingQueue<>(PREFETCH_CHUNKS);
                    Thread producer = createProducer(engine, chunks, queue);
                    producer.start();
                    consume(queue);
                    producer.interrupt();
                    if (!stopped && listener != null) {
                        listener.onDone(TTSEngine.UTTERANCE_ID_DONE);
                    }
                } catch (Exception e) {
                    LOG.e(e);
                    if (listener != null) {
                        listener.onError(TTSEngine.UTTERANCE_ID_DONE, e);
                    }
                } finally {
                    playing = false;
                    EventBus.getDefault().post(new TtsStatus());
                }
            }
        }, "@T Sherpa TTS");
        worker.start();
    }

    public synchronized void stop() {
        stopped = true;
        playing = false;
        releaseTrack();
        worker = null;
    }

    private synchronized void releaseTrack() {
        if (track != null) {
            try {
                track.pause();
                track.flush();
                track.release();
            } catch (Exception e) {
                LOG.e(e);
            }
            track = null;
        }
    }

    public synchronized void shutdown() {
        stop();
        if (tts != null) {
            tts.release();
            tts = null;
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    private synchronized OfflineTts getTts(Context context) {
        if (tts == null) {
            tts = new OfflineTts(context.getAssets(), createConfig(context));
        }
        return tts;
    }

    private OfflineTtsConfig createConfig(Context context) {
        OfflineTtsModelConfig model = new OfflineTtsModelConfig();
        model.numThreads = Math.max(1, AppState.get().sherpaOnnxTtsNumThreads);
        model.debug = false;
        model.provider = "cpu";

        OfflineTtsMatchaModelConfig matcha = new OfflineTtsMatchaModelConfig();
        matcha.acousticModel = ACOUSTIC_MODEL;
        matcha.vocoder = VOCODER;
        matcha.lexicon = LEXICON;
        matcha.tokens = TOKENS;
        model.matcha = matcha;

        OfflineTtsConfig config = new OfflineTtsConfig();
        config.model = model;
        config.ruleFsts = RULE_FSTS;
        return config;
    }

    private Thread createProducer(final OfflineTts engine, final List<TextChunk> chunks,
                                  final BlockingQueue<AudioChunk> queue) {
        return new Thread(new Runnable() {
            @Override public void run() {
                try {
                    for (TextChunk chunk : chunks) {
                        if (stopped || Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        long start = System.currentTimeMillis();
                        GeneratedAudio audio = engine.generate(chunk.text, AppState.get().sherpaOnnxTtsSpeakerId,
                                safeSpeed(AppState.get().ttsSpeed));
                        LOG.d(TAG, "generated chunk", chunk.text.length(), "chars in",
                                System.currentTimeMillis() - start, "ms");
                        put(queue, new AudioChunk(audio, chunk.pauseAfterMs));
                    }
                    put(queue, AudioChunk.end());
                } catch (Exception e) {
                    LOG.e(e);
                    put(queue, new AudioChunk(e));
                }
            }
        }, "@T Sherpa TTS Producer");
    }

    private void consume(BlockingQueue<AudioChunk> queue) throws Exception {
        while (!stopped) {
            AudioChunk chunk = queue.take();
            if (chunk.end) {
                return;
            }
            if (chunk.error != null) {
                throw chunk.error;
            }
            play(chunk.audio);
            pause(chunk.pauseAfterMs);
        }
    }

    private void put(BlockingQueue<AudioChunk> queue, AudioChunk chunk) {
        while (!stopped) {
            try {
                queue.put(chunk);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void play(GeneratedAudio audio) {
        if (audio == null || audio.samples == null || audio.samples.length == 0 || stopped) {
            return;
        }
        int bufferSizeBytes = AudioTrack.getMinBufferSize(audio.sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(audio.sampleRate)
                .build();
        track = new AudioTrack(attributes, format, bufferSizeBytes, AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
        track.play();
        int offset = 0;
        int bufferSizeSamples = Math.max(1, bufferSizeBytes / 4);
        while (!stopped && offset < audio.samples.length) {
            int count = Math.min(bufferSizeSamples, audio.samples.length - offset);
            int written = track.write(audio.samples, offset, count, AudioTrack.WRITE_BLOCKING);
            if (written <= 0) {
                break;
            }
            offset += written;
        }
        releaseTrack();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace(TxtUtils.TTS_PAUSE, " ").trim();
    }

    private List<TextChunk> splitForLowLatency(String text) {
        ArrayList<TextChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);
            if (shouldBreak(c) || current.length() >= MAX_CHUNK_LENGTH) {
                addChunk(chunks, current, pauseFor(c));
            }
        }
        addChunk(chunks, current, 0);
        if (chunks.isEmpty() && TxtUtils.isNotEmpty(text)) {
            chunks.add(new TextChunk(text, 0));
        }
        return chunks;
    }

    private boolean shouldBreak(char c) {
        return c == '。' || c == '！' || c == '？' || c == '；' || c == '\n' || c == '\r'
                || c == '.' || c == '!' || c == '?' || c == ';';
    }

    private long pauseFor(char c) {
        if (c == '\n' || c == '\r') {
            return Math.max(AppState.get().ttsPauseDuration, 250);
        }
        if (shouldBreak(c)) {
            return AppState.get().ttsPauseDuration;
        }
        return 0;
    }

    private void addChunk(List<TextChunk> chunks, StringBuilder current, long pauseAfterMs) {
        String chunk = current.toString().trim();
        current.setLength(0);
        if (TxtUtils.isNotEmpty(chunk)) {
            chunks.add(new TextChunk(chunk, pauseAfterMs));
        }
    }

    private void pause(long pauseMs) throws InterruptedException {
        if (pauseMs > 0 && !stopped) {
            Thread.sleep(pauseMs);
        }
    }

    private float safeSpeed(float speed) {
        return speed <= 0 ? 1.0f : speed;
    }

}
