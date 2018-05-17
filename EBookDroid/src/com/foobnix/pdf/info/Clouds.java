package com.foobnix.pdf.info;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;

import android.content.Context;
import android.content.SharedPreferences;

public class Clouds {

    private static final Clouds instance = new Clouds();

    transient SharedPreferences sp;
    transient public CloudStorage dropbox;

    public String dropboxToken;

    public void init(Context c) {
        CloudRail.setAppKey("5817abf0c40abf10ce9a04c5");

        sp = c.getSharedPreferences("Clouds", Context.MODE_PRIVATE);
        Objects.loadFromSp(this, sp);

        dropbox = new Dropbox(c, "wp5uvfelqbdnwkg", "e7hfer9dh5r18tz", "https://auth.cloudrail.com/Librera", "foobnix");
        if (dropbox != null) {
            try {
                dropbox.loadAsString(dropboxToken);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    }

    public boolean isDropbox() {
        return dropboxToken != null;
    }

    public void save() {
        LOG.d("CloudRail save");
        Objects.saveToSP(this, sp);

    }

    public static Clouds get() {
        return instance;
    }

}
