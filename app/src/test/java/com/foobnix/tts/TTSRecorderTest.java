package com.foobnix.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;

public class TTSRecorderTest {

    @Test public void splitsLongTextAtSentencesWithinLimit() {
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            text.append("Sentence number ").append(i).append(" is here. ");
        }
        final List<String> parts = TTSRecorder.split(text.toString(), 3900);
        assertTrue(parts.size() > 1);
        final StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            assertTrue(part.length() <= 3900);
            assertTrue(part, part.endsWith("."));
            joined.append(part).append(' ');
        }
        assertEquals(text.toString().trim(), joined.toString().trim());
    }

    @Test public void splitsTextWithoutSpacesHard() {
        final String text = new String(new char[9000]).replace('\0', 'a');
        final List<String> parts = TTSRecorder.split(text, 4000);
        assertEquals(3, parts.size());
        assertEquals(4000, parts.get(0).length());
    }

    @Test public void shortTextIsOnePart() {
        assertEquals(1, TTSRecorder.split("Hello world.", 4000).size());
    }

    @Test public void readsWavWithExtraChunkAndUnsetDataSize() throws Exception {
        final TTSRecorder.WavInfo info = new TTSRecorder.WavInfo();
        info.channels = 1;
        info.sampleRate = 24000;
        info.bits = 16;
        final byte[] header = TTSRecorder.wavHeader(info, 0); // the size an engine may leave unset
        final File file = File.createTempFile("tts", ".wav");
        try (FileOutputStream out = new FileOutputStream(file)) {
            // RIFF, WAVE, fmt chunk, then a LIST chunk before the data
            out.write(header, 0, 36);
            out.write(new byte[]{'L', 'I', 'S', 'T', 3, 0, 0, 0, 1, 2, 3, 0});
            out.write(header, 36, 8);
            out.write(new byte[1000]);
        }
        final TTSRecorder.WavInfo read = TTSRecorder.readWav(file);
        assertEquals(24000, read.sampleRate);
        assertEquals(1, read.channels);
        assertEquals(16, read.bits);
        assertEquals(36 + 12 + 8, read.dataStart);
        assertEquals(1000, read.dataLength);
        Files.delete(file.toPath());
    }
}
