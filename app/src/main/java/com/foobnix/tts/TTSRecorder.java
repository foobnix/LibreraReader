package com.foobnix.tts;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.github.axet.lamejni.Lame;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Records the pages of a book, read by the voice, into an MP3 file per page.
 *
 * The pages come from where TTS reading takes them, never from the reader's own pages on the
 * main thread: the WebView book (foliate) answers on the main thread and makes its callers wait,
 * so it is asked from this thread; a MuPDF or PDFium book is opened again, a copy of its own laid
 * out as the reader's, so the pages on screen are neither touched nor recycled. A page longer than
 * the engine takes at once is spoken in parts, joined into the one file.
 */
public final class TTSRecorder {

    private static final String TAG = "TTSRecorder";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "@T record"));

    /** The longest a part may take to synthesize before the engine is taken for stuck. */
    private static final long PART_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);

    private TTSRecorder() {
    }

    /** Records the 1-based pages from..to; info hears the progress, and the word success at the end. */
    public static void record(final DocumentController controller, final ResultResponse<String> info, final int from,
                              final int to) {
        final String path = controller.getCurrentBook().getPath();
        final String bookName = controller.getCurrentBook().getName();
        final String success = controller.getString(R.string.success);
        final String fail = controller.getString(R.string.msg_unexpected_error);
        final int width = AppSP.get().lastBookWidth > 0 ? AppSP.get().lastBookWidth : controller.getBookWidth();
        final int height = AppSP.get().lastBookHeight > 0 ? AppSP.get().lastBookHeight : controller.getBookHeight();
        // reading aloud and recording share the one engine and its listener
        TTSEngine.get().stop();

        executor.execute(() -> {
            CodecDocument own = null;
            try {
                final File folder = new File(BookCSS.get().ttsSpeakPath, "TTS_" + bookName);
                folder.mkdirs();
                if (!folder.isDirectory()) {
                    info.onResultRecive(controller.getString(R.string.file_not_found) + " " + folder.getPath());
                    return;
                }
                final TextToSpeech tts = engine();
                if (tts == null) {
                    info.onResultRecive(fail);
                    return;
                }
                info.onResultRecive("…");
                CodecDocument dc = FoliateDocument.current();
                if (dc == null) {
                    own = TempHolder.get().loadingCancelled.ignoredWhile(() -> {
                        final CodecDocument copy = ImageExtractor.singleCodecContext(path, "");
                        if (copy != null) {
                            copy.getPageCount(width, height, BookCSS.get().fontSizeSp);
                        }
                        return copy;
                    });
                    dc = own;
                }
                if (dc == null) {
                    info.onResultRecive(fail);
                    return;
                }
                final int last = Math.min(to, dc.getPageCount());
                final DecimalFormat df = new DecimalFormat("0000");
                for (int page = from - 1; page < last && TempHolder.isRecordTTS; page++) {
                    info.onResultRecive((page + 1) + " / " + last);
                    final String text = pageText(dc, page);
                    if (TxtUtils.isEmpty(text)) {
                        continue;
                    }
                    final String name = "page-" + df.format(page + 1);
                    recordPage(tts, text, folder, name, new File(folder, name + TTSEngine.MP3));
                }
                info.onResultRecive(success);
            } catch (Throwable e) {
                LOG.e(e);
                info.onResultRecive(fail);
            } finally {
                if (own != null) {
                    own.recycle();
                }
                TempHolder.isRecordTTS = false;
            }
        });
    }

    /** The engine, once it is ready to speak; null when it does not get there. */
    private static TextToSpeech engine() throws InterruptedException {
        final TTSEngine engine = TTSEngine.get();
        final CountDownLatch ready = new CountDownLatch(1);
        final TextToSpeech tts = engine.getTTS(status -> ready.countDown());
        if (tts == null) {
            return null;
        }
        for (int i = 0; i < 100 && !engine.isInitialized(); i++) {
            ready.await(100, TimeUnit.MILLISECONDS);
        }
        if (!engine.isInitialized()) {
            return null;
        }
        tts.setPitch(AppState.get().ttsPitch);
        tts.setSpeechRate(AppState.get().ttsSpeed);
        return tts;
    }

    private static String pageText(CodecDocument dc, int number) {
        final CodecPage page = dc.getPage(number);
        if (page == null) {
            return null;
        }
        try {
            String html = page.getPageHTML();
            if (html == null) {
                return null;
            }
            html = TxtUtils.replaceHTMLforTTS(html);
            html = html.replace(TxtUtils.TTS_PAUSE, " ");
            html = html.replace(TxtUtils.NON_BREAKE_SPACE, " ");
            return html.trim();
        } finally {
            // the WebView book's pages are the reader's own
            if (!(dc instanceof FoliateDocument)) {
                page.recycle();
            }
        }
    }

    /** Speaks a page part by part into WAV files, then joins them into the page's file. */
    private static void recordPage(TextToSpeech tts, String text, File folder, String name, File out)
            throws Exception {
        final List<String> parts = split(text, Math.max(500, TextToSpeech.getMaxSpeechInputLength() - 100));
        final List<File> wavs = new ArrayList<File>();
        try {
            for (int i = 0; i < parts.size() && TempHolder.isRecordTTS; i++) {
                final File wav = new File(folder, name + "-" + i + ".part" + TTSEngine.WAV);
                if (synthesize(tts, parts.get(i), wav)) {
                    wavs.add(wav);
                }
            }
            if (wavs.isEmpty() || !TempHolder.isRecordTTS) {
                return;
            }
            out.delete();
            toMp3(wavs, out);
            LOG.d(TAG, "page", out.getName(), out.length());
        } finally {
            for (File wav : wavs) {
                wav.delete();
            }
        }
    }

    /** One part into a WAV file; false when the engine fails or is stopped. */
    private static boolean synthesize(final TextToSpeech tts, String text, File wav) throws InterruptedException {
        final String id = "record-" + wav.getName();
        final CountDownLatch done = new CountDownLatch(1);
        final boolean[] ok = new boolean[1];
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {
            }

            @Override public void onDone(String utteranceId) {
                if (id.equals(utteranceId)) {
                    ok[0] = true;
                    done.countDown();
                }
            }

            @Override public void onError(String utteranceId) {
                if (id.equals(utteranceId)) {
                    done.countDown();
                }
            }

            @Override public void onStop(String utteranceId, boolean interrupted) {
                if (id.equals(utteranceId)) {
                    done.countDown();
                }
            }
        });
        wav.delete();
        if (tts.synthesizeToFile(text, new Bundle(), wav, id) != TextToSpeech.SUCCESS) {
            LOG.d(TAG, "synthesizeToFile refused", wav.getName());
            return false;
        }
        final long until = System.currentTimeMillis() + PART_TIMEOUT_MS;
        while (!done.await(300, TimeUnit.MILLISECONDS)) {
            if (!TempHolder.isRecordTTS || System.currentTimeMillis() > until) {
                tts.stop();
                return false;
            }
        }
        return ok[0] && wav.length() > 44;
    }

    /** Text cut into parts no longer than max, at the end of a sentence where one is near. */
    static List<String> split(String text, int max) {
        final List<String> parts = new ArrayList<String>();
        String rest = text.trim();
        while (rest.length() > max) {
            int cut = -1;
            for (int i = max; i > max / 2; i--) {
                final char c = rest.charAt(i - 1);
                if (c == '.' || c == '!' || c == '?' || c == '…' || c == '\n') {
                    cut = i;
                    break;
                }
            }
            if (cut < 0) {
                cut = rest.lastIndexOf(' ', max);
            }
            if (cut <= 0) {
                cut = max;
            }
            parts.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        if (rest.length() > 0) {
            parts.add(rest);
        }
        return parts;
    }

    // ---------------------------------------------------------------- WAV and MP3

    /** Where the samples of a WAV file are and what they are. */
    static final class WavInfo {
        int channels = 1;
        int sampleRate = 22050;
        int bits = 16;
        long dataStart = -1;
        long dataLength;
    }

    /** Reads the chunks of a WAV file; an engine may leave the data size unset, then it runs to the end. */
    static WavInfo readWav(File file) throws IOException {
        final WavInfo info = new WavInfo();
        try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
            final byte[] head = new byte[12];
            in.readFully(head);
            if (!"RIFF".equals(new String(head, 0, 4, "US-ASCII")) || !"WAVE".equals(new String(head, 8, 4, "US-ASCII"))) {
                throw new IOException("not a WAV file " + file.getName());
            }
            final byte[] chunk = new byte[8];
            while (in.getFilePointer() + 8 <= in.length()) {
                in.readFully(chunk);
                final String id = new String(chunk, 0, 4, "US-ASCII");
                final long size = ByteBuffer.wrap(chunk, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
                final long start = in.getFilePointer();
                if ("fmt ".equals(id)) {
                    final byte[] fmt = new byte[16];
                    in.readFully(fmt);
                    final ByteBuffer b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN);
                    b.getShort(); // format
                    info.channels = b.getShort();
                    info.sampleRate = b.getInt();
                    b.getInt();
                    b.getShort();
                    info.bits = b.getShort();
                } else if ("data".equals(id)) {
                    info.dataStart = start;
                    final long rest = in.length() - start;
                    info.dataLength = size == 0 || size > rest ? rest : size;
                    break;
                }
                in.seek(start + size + (size & 1));
            }
        }
        if (info.dataStart < 0) {
            throw new IOException("no data in " + file.getName());
        }
        return info;
    }

    private static void toMp3(List<File> wavs, File out) throws IOException {
        final WavInfo first = readWav(wavs.get(0));
        if (first.bits != 16) {
            // the encoder takes 16-bit samples only
            toWav(wavs, new File(out.getPath().replace(TTSEngine.MP3, TTSEngine.WAV)));
            return;
        }
        final Lame lame = new Lame();
        lame.open(first.channels, first.sampleRate, 128, 4);
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
            final byte[] bytes = new byte[32768];
            final short[] shorts = new short[bytes.length / 2];
            for (File file : wavs) {
                final WavInfo wav = readWav(file);
                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    skip(in, wav.dataStart);
                    long left = wav.dataLength;
                    while (left > 1) {
                        final int n = in.read(bytes, 0, (int) Math.min(bytes.length, left & ~1L));
                        if (n <= 0) {
                            break;
                        }
                        int count = n;
                        if ((count & 1) == 1) {
                            // keep whole samples: read the odd byte's partner
                            final int b = in.read();
                            if (b < 0) {
                                count--;
                            } else {
                                bytes[count++] = (byte) b;
                            }
                        }
                        left -= count;
                        ByteBuffer.wrap(bytes, 0, count).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts, 0, count / 2);
                        os.write(lame.encode(shorts, 0, count / 2));
                    }
                }
            }
            // what the encoder still holds, else the last of the page is cut off
            os.write(lame.encode(new short[0], 0, 0));
        } finally {
            lame.close();
        }
    }

    private static void toWav(List<File> wavs, File out) throws IOException {
        final WavInfo first = readWav(wavs.get(0));
        long total = 0;
        for (File file : wavs) {
            total += readWav(file).dataLength;
        }
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
            os.write(wavHeader(first, total));
            final byte[] buffer = new byte[32768];
            for (File file : wavs) {
                final WavInfo wav = readWav(file);
                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    skip(in, wav.dataStart);
                    long left = wav.dataLength;
                    int n;
                    while (left > 0 && (n = in.read(buffer, 0, (int) Math.min(buffer.length, left))) > 0) {
                        os.write(buffer, 0, n);
                        left -= n;
                    }
                }
            }
        }
    }

    static byte[] wavHeader(WavInfo info, long dataLength) {
        final ByteBuffer b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        final int blockAlign = info.channels * info.bits / 8;
        b.put(new byte[]{'R', 'I', 'F', 'F'});
        b.putInt((int) (36 + dataLength));
        b.put(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
        b.putInt(16);
        b.putShort((short) 1);
        b.putShort((short) info.channels);
        b.putInt(info.sampleRate);
        b.putInt(info.sampleRate * blockAlign);
        b.putShort((short) blockAlign);
        b.putShort((short) info.bits);
        b.put(new byte[]{'d', 'a', 't', 'a'});
        b.putInt((int) dataLength);
        return b.array();
    }

    private static void skip(InputStream in, long count) throws IOException {
        long left = count;
        while (left > 0) {
            final long n = in.skip(left);
            if (n <= 0) {
                if (in.read() < 0) {
                    throw new IOException("short WAV file");
                }
                left--;
            } else {
                left -= n;
            }
        }
    }
}
