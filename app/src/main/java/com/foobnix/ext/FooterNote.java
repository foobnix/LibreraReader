package com.foobnix.ext;

import com.foobnix.android.utils.LOG;

import java.util.Map;

public class FooterNote {

    public String path;
    public Map<String, String> notes;

    public FooterNote(String path, Map<String, String> notes) {
        this.path = path;
        this.notes = notes;
    }

    public void debugPrint() {
        LOG.d("debugPrint", path);
        if (notes == null) {
            LOG.d("Notes is null");
            return;
        }
        for (String key : notes.keySet()) {
            LOG.d(key, " = ", notes.get(key));
        }
    }

}
