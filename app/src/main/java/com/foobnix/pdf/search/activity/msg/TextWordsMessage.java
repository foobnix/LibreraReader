package com.foobnix.pdf.search.activity.msg;

import org.ebookdroid.droids.mupdf.codec.TextWord;

public class TextWordsMessage {

    private final TextWord[][] messages;
    private final int page;

    public TextWordsMessage(TextWord[][] messages, int page) {
        this.messages = messages;
        this.page = page;
    }

    public TextWord[][] getMessages() {
        return messages;
    }

    public int getPage() {
        return page;
    }

}
