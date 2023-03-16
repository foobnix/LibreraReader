package org.ebookdroid.core.codec;

import com.foobnix.pdf.info.Prefs;

import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.Collections;
import java.util.List;

public abstract class AbstractCodecPage implements CodecPage {


    String path;

    public AbstractCodecPage(String path) {
        this.path = path;
    }

    @Override
    public List<PageLink> getPageLinks() {
        return Collections.emptyList();
    }


    @Override
    public TextWord[][] getText() {
        if (Prefs.get().isErrorExist(path, 0)) {
            return new TextWord[0][0];
        }
        try {
            Prefs.get().put(path, 0);
            return getTextIml();
        } finally {
            Prefs.get().remove(path, 0);
        }
    }

    public abstract TextWord[][] getTextIml();
}
