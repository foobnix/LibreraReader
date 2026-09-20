package com.foobnix.ext;

import com.foobnix.model.AppData;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.model.BookCSS;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/** Captures processing inputs once; other threads continue to see their own settings. */
public final class EpubProcessingSettings {
    private static final ThreadLocal<EpubProcessingSettings> active = new ThreadLocal<>();
    private final boolean isEnableTextReplacement = AppState.get().isEnableTextReplacement;
    private final boolean isReferenceMode = AppState.get().isReferenceMode;
    private final boolean isShowPageNumbers = AppState.get().isShowPageNumbers;
    private final boolean isShowFooterNotesInText = AppState.get().isShowFooterNotesInText;
    private final boolean isBionicMode = AppState.get().isBionicMode;
    private final boolean isExperimental = AppState.get().isExperimental;
    private final boolean isCharacterEncoding = AppState.get().isCharacterEncoding;
    private final boolean isPreText = AppState.get().isPreText;
    private final boolean isLineBreaksText = AppState.get().isLineBreaksText;
    private final boolean isDouble = AppSP.get().isDouble;
    private final boolean isCapitalLetter = BookCSS.get().isCapitalLetter;
    private final boolean isAccurateFontSize = AppState.get().isAccurateFontSize;
    private final boolean isAutoHypens = BookCSS.get().isAutoHypens;
    private final boolean isEnableBBCode = BookCSS.get().isEnableBBCode;
    private final int documentStyle = BookCSS.get().documentStyle;
    private final int fullScreenMode = AppState.get().fullScreenMode;
    private final boolean enableImageScale = AppState.get().enableImageScale;
    private final String characterEncoding = AppState.get().characterEncoding;
    private final String language;

    private EpubProcessingSettings() { language = AppSP.get().hypenLang; }
    private EpubProcessingSettings(String selectedLanguage) { language = selectedLanguage; }

    private final long replacementHash = AppState.get().textReplacementHash;
    private final List<SimpleMeta> replacements = copyReplacements();
    private static List<SimpleMeta> copyReplacements() {
        List<SimpleMeta> result = new ArrayList<>();
        for (SimpleMeta value : AppData.get().getAllTextReplaces()) {
            result.add(new SimpleMeta(value.name, value.path, value.time));
        }
        return Collections.unmodifiableList(result);
    }
    public static List<SimpleMeta> replacements() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? copyReplacements() : settings.replacements;
    }
    private static String replacementKey() {
        EpubProcessingSettings settings = active.get();
        StringBuilder result = new StringBuilder().append(settings == null
                ? AppState.get().textReplacementHash : settings.replacementHash);
        for (SimpleMeta value : replacements()) {
            String name = String.valueOf(value.name), path = String.valueOf(value.path);
            result.append('|').append(name.length()).append(':').append(name)
                    .append(path.length()).append(':').append(path);
        }
        return result.toString();
    }
    public static Scope capture() { return new Scope(new EpubProcessingSettings()); }
    public static Scope capture(String selectedLanguage) {
        return new Scope(new EpubProcessingSettings(selectedLanguage));
    }
    public static final class Scope implements AutoCloseable {
        private final EpubProcessingSettings previous;
        private Scope(EpubProcessingSettings settings) {
            previous = active.get();
            active.set(settings);
        }
        @Override public void close() {
            if (previous == null) active.remove(); else active.set(previous);
        }
    }
    public static boolean isEnableTextReplacement() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isEnableTextReplacement : settings.isEnableTextReplacement;
    }
    public static boolean isReferenceMode() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isReferenceMode : settings.isReferenceMode;
    }
    public static boolean isShowPageNumbers() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isShowPageNumbers : settings.isShowPageNumbers;
    }
    public static boolean isShowFooterNotesInText() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isShowFooterNotesInText : settings.isShowFooterNotesInText;
    }
    public static boolean isBionicMode() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isBionicMode : settings.isBionicMode;
    }
    public static boolean isExperimental() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isExperimental : settings.isExperimental;
    }
    public static boolean isCharacterEncoding() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isCharacterEncoding : settings.isCharacterEncoding;
    }
    public static boolean isPreText() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isPreText : settings.isPreText;
    }
    public static boolean isLineBreaksText() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isLineBreaksText : settings.isLineBreaksText;
    }
    public static boolean isDouble() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppSP.get().isDouble : settings.isDouble;
    }
    public static boolean isCapitalLetter() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? BookCSS.get().isCapitalLetter : settings.isCapitalLetter;
    }
    public static boolean isAccurateFontSize() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().isAccurateFontSize : settings.isAccurateFontSize;
    }
    public static boolean isAutoHypens() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? BookCSS.get().isAutoHypens : settings.isAutoHypens;
    }
    public static boolean isEnableBBCode() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? BookCSS.get().isEnableBBCode : settings.isEnableBBCode;
    }
    public static int documentStyle() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? BookCSS.get().documentStyle : settings.documentStyle;
    }
    public static int fullScreenMode() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().fullScreenMode : settings.fullScreenMode;
    }
    public static boolean enableImageScale() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().enableImageScale : settings.enableImageScale;
    }
    public static String characterEncoding() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppState.get().characterEncoding : settings.characterEncoding;
    }
    public static String language() {
        EpubProcessingSettings settings = active.get();
        return settings == null ? AppSP.get().hypenLang : settings.language;
    }
    public static boolean enabled() {
        return isEnableTextReplacement() || isAutoHypens() || isReferenceMode() || isShowFooterNotesInText() || isEnableBBCode();
    }
    public static String key() {
        return "v3|" + isEnableTextReplacement() + "|" + isReferenceMode() + "|" +
                isShowPageNumbers() + "|" + isShowFooterNotesInText() + "|" + fullScreenMode() + "|" +
                documentStyle() + "|" + isAutoHypens() + "|" + isEnableBBCode() + "|" + isBionicMode() + "|" +
                language() + "|" + enableImageScale() + "|" + isExperimental() + "|" +
                isCharacterEncoding() + "|" + characterEncoding() + "|" + isPreText() + "|" +
                isLineBreaksText() + "|" + isDouble() + "|" + isCapitalLetter() + "|" +
                isAccurateFontSize() + "|" + replacementKey();
    }
}
