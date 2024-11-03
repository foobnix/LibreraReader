package com.foobnix.pdf.info;

import android.content.Context;

public class ReadingGroup {

    private static ReadingGroup instance;
    private Context context;

    private ReadingGroup(Context context) {
        this.context = context;
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ReadingGroup(context);
        }
    }

    public static synchronized ReadingGroup getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ReadingGroup is not initialized, call init() method first.");
        }
        return instance;
    }

    public void createGroup(String groupName) {
        // Code to create a new reading group
    }

    public void joinGroup(String groupId) {
        // Code to join an existing reading group
    }

    public void manageGroup(String groupId) {
        // Code to manage a reading group
    }
}
