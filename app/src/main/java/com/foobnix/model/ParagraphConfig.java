package com.foobnix.model;

import org.librera.JSONException;
import org.librera.LinkedJSONObject;

/**
 * Per-book paragraph spacing configuration.
 * Controls paragraph height, gap rules, and markdown-style horizontal-rule detection.
 */
public class ParagraphConfig {

    public int paragraphHeight = 1;
    public String paragraphGapRules = null;
    public boolean lineMdRecognition = true;
    public boolean enabled = false;

    public String toJson() {
        try {
            LinkedJSONObject obj = new LinkedJSONObject();
            obj.put("paragraphHeight", paragraphHeight);
            obj.put("paragraphGapRules", paragraphGapRules);
            obj.put("lineMdRecognition", lineMdRecognition);
            obj.put("enabled", enabled);
            return obj.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public static ParagraphConfig fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            LinkedJSONObject obj = new LinkedJSONObject(json);
            ParagraphConfig config = new ParagraphConfig();
            config.paragraphHeight = obj.optInt("paragraphHeight", 1);
            config.paragraphGapRules = obj.isNull("paragraphGapRules") ? null : obj.optString("paragraphGapRules", null);
            config.lineMdRecognition = obj.optBoolean("lineMdRecognition", true);
            config.enabled = obj.optBoolean("enabled", false);
            return config;
        } catch (JSONException e) {
            return null;
        }
    }

    public static ParagraphConfig defaultConfig() {
        return new ParagraphConfig();
    }
}