package com.foobnix.android.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;
import com.naman14.androidlame.WaveReader;

public class Audio {
    private static final String TAG = "AUDIO-LOG";
    private static final int OUTPUT_STREAM_BUFFER = 8192;

    public static void addLog(String txt) {
        LOG.d(TAG, txt);
    }

    public static synchronized void encode(String inputPath, String outputPath) {

        File input = new File(inputPath);
        final File output = new File(inputPath);

        int CHUNK_SIZE = 8192;

        addLog("Initialising wav reader");
        addLog("in:" + inputPath);
        addLog("out:" + outputPath);
        final WaveReader waveReader = new WaveReader(input);

        try {
            waveReader.openWave();
        } catch (IOException e) {
            LOG.e(e, TAG);
        }

        addLog("Intitialising encoder");
        AndroidLame androidLame = new LameBuilder().setInSampleRate(waveReader.getSampleRate()).setOutChannels(waveReader.getChannels()).setOutBitrate(128).setOutSampleRate(waveReader.getSampleRate()).setQuality(5)
                .build();
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(output), OUTPUT_STREAM_BUFFER);
        } catch (FileNotFoundException e) {
            LOG.e(e, TAG);
        }

        int bytesRead = 0;

        short[] buffer_l = new short[CHUNK_SIZE];
        short[] buffer_r = new short[CHUNK_SIZE];
        byte[] mp3Buf = new byte[CHUNK_SIZE];

        int channels = waveReader.getChannels();

        addLog("started encoding");
        while (true) {
            try {
                if (channels == 2) {

                    bytesRead = waveReader.read(buffer_l, buffer_r, CHUNK_SIZE);
                    addLog("bytes read=" + bytesRead);

                    if (bytesRead > 0) {

                        int bytesEncoded = 0;
                        bytesEncoded = androidLame.encode(buffer_l, buffer_r, bytesRead, mp3Buf);
                        addLog("bytes encoded=" + bytesEncoded);

                        if (bytesEncoded > 0) {
                            try {
                                addLog("writing mp3 buffer to outputstream with " + bytesEncoded + " bytes");
                                outputStream.write(mp3Buf, 0, bytesEncoded);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else
                        break;
                } else {

                    bytesRead = waveReader.read(buffer_l, CHUNK_SIZE);
                    addLog("bytes read=" + bytesRead);

                    if (bytesRead > 0) {
                        int bytesEncoded = 0;

                        bytesEncoded = androidLame.encode(buffer_l, buffer_l, bytesRead, mp3Buf);
                        addLog("bytes encoded=" + bytesEncoded);

                        if (bytesEncoded > 0) {
                            try {
                                addLog("writing mp3 buffer to outputstream with " + bytesEncoded + " bytes");
                                outputStream.write(mp3Buf, 0, bytesEncoded);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else
                        break;
                }

            } catch (IOException e) {
                LOG.e(e, TAG);
            }
        }

        addLog("flushing final mp3buffer");
        int outputMp3buf = androidLame.flush(mp3Buf);
        addLog("flushed " + outputMp3buf + " bytes");

        if (outputMp3buf > 0) {
            try {
                addLog("writing final mp3buffer to outputstream");
                outputStream.write(mp3Buf, 0, outputMp3buf);
                addLog("closing output stream");
                outputStream.close();

            } catch (IOException e) {
                LOG.e(e, TAG);
            }
        }

    }

}
