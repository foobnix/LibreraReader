package com.foobnix.pdf.info;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.pdf.info.model.BookCSS;

import android.content.Context;

public class FontExtractor {

    public static void extractFonts(final Context c) {
        if (c == null) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                extractInside(c, "fonts", BookCSS.FONTS_DIR);
            };
        }.start();

    }

    private static void extractInside(final Context c, String from, String to) {
        try {
            File fontsDir = getFontsDir(c, to);
            if (fontsDir.exists()) {
                LOG.d("FontExtractor Dir exists", fontsDir);
            } else {
                fontsDir.mkdirs();
            }
            String[] list = c.getAssets().list(from);
            for (String fontName : list) {
                File fontFile = new File(fontsDir, fontName);
                if (!fontFile.exists()) {
                    LOG.d("FontExtractor Copy file" + fontName, "to", fontFile);
                    InputStream open = c.getAssets().open(from + "/" + fontName);
                    EpubExtractor.writeToStream(open, new FileOutputStream(fontFile));
                    open.close();
                }
            }

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static File getFontsDir(final Context c, String to) {
        return new File(c.getExternalCacheDir(), to);
    }
}
