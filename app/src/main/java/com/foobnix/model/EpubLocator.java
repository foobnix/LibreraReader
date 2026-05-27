package com.foobnix.model;

import org.librera.JSONException;
import org.librera.LinkedJSONObject;

/**
 * Represents a location within an EPUB document, identified by spine index,
 * chapter href, DOM path, text node index, and character index.
 */
public class EpubLocator {

    public int spineIndex;
    public String chapterHref;
    public String domPath;
    public int textNodeIndex;
    public int charIndex;
    public String textSnippet;

    public EpubLocator() {
    }

    public EpubLocator(int spineIndex, String chapterHref, String domPath, int textNodeIndex, int charIndex, String textSnippet) {
        this.spineIndex = spineIndex;
        this.chapterHref = chapterHref;
        this.domPath = domPath;
        this.textNodeIndex = textNodeIndex;
        this.charIndex = charIndex;
        this.textSnippet = textSnippet;
    }

    public EpubLocator(int spineIndex, String chapterHref, String domPath, int textNodeIndex, int charIndex) {
        this(spineIndex, chapterHref, domPath, textNodeIndex, charIndex, "");
    }

    public String toJson() {
        LinkedJSONObject json = new LinkedJSONObject();
        try {
            json.put("spineIndex", spineIndex);
            json.put("chapterHref", chapterHref);
            json.put("domPath", domPath);
            json.put("textNodeIndex", textNodeIndex);
            json.put("charIndex", charIndex);
            json.put("textSnippet", textSnippet == null ? "" : textSnippet);
        } catch (JSONException e) {
            return "";
        }
        return json.toString();
    }

    public static EpubLocator fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            LinkedJSONObject obj = new LinkedJSONObject(json);
            EpubLocator locator = new EpubLocator();
            locator.spineIndex = obj.optInt("spineIndex", 0);
            locator.chapterHref = obj.optString("chapterHref", null);
            locator.domPath = obj.optString("domPath", null);
            locator.textNodeIndex = obj.optInt("textNodeIndex", 0);
            locator.charIndex = obj.optInt("charIndex", 0);
            locator.textSnippet = obj.optString("textSnippet", "");
            return locator;
        } catch (JSONException e) {
            return null;
        }
    }

    public boolean isValid() {
        return chapterHref != null && !chapterHref.isEmpty();
    }
}