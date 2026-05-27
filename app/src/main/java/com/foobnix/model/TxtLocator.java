package com.foobnix.model;

import org.librera.JSONException;
import org.librera.LinkedJSONObject;

/**
 * Represents a position within a plain-text file by byte offset and character encoding.
 * Used to persist and restore the reader's location in TXT documents.
 */
public class TxtLocator {

    public long byteOffset;
    public String encoding;
    public String textSnippet;

    public TxtLocator() {
    }

    public TxtLocator(long byteOffset, String encoding, String textSnippet) {
        this.byteOffset = byteOffset;
        this.encoding = encoding;
        this.textSnippet = textSnippet;
    }

    public TxtLocator(long byteOffset, String encoding) {
        this(byteOffset, encoding, "");
    }

    public String toJson() {
        LinkedJSONObject obj = new LinkedJSONObject();
        try {
            obj.put("byteOffset", byteOffset);
            obj.put("encoding", encoding);
            obj.put("textSnippet", textSnippet == null ? "" : textSnippet);
        } catch (JSONException e) {
            return "";
        }
        return obj.toString();
    }

    public static TxtLocator fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            LinkedJSONObject obj = new LinkedJSONObject(json);
            TxtLocator locator = new TxtLocator();
            locator.byteOffset = obj.optLong("byteOffset", 0);
            locator.encoding = obj.optString("encoding", null);
            locator.textSnippet = obj.optString("textSnippet", "");
            return locator;
        } catch (JSONException e) {
            return null;
        }
    }

    public boolean isValid() {
        return byteOffset >= 0 && encoding != null && !encoding.isEmpty();
    }
}