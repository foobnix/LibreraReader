# R8 rules for release builds.
# Only code reached by reflection or from native code is kept here;
# everything else is shrunk, optimized and obfuscated.

# Readable crash reports in Play Console (the mapping file is uploaded inside the AAB).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Settings, reading progress and bookmarks: com.foobnix.android.utils.Objects saves and loads
# these classes field by field, using the field name as the JSON / SharedPreferences key.
# Instance fields are kept as declared (private ones too, so access modification can't
# turn them into saved fields); static fields are never saved and stay optimizable.
-keepclassmembers class com.foobnix.model.AppState,
                        com.foobnix.model.AppSP,
                        com.foobnix.model.AppBook,
                        com.foobnix.model.AppBookmark,
                        com.foobnix.pdf.info.model.BookCSS,
                        com.foobnix.pdf.info.wrapper.PasswordState,
                        com.foobnix.pdf.info.Clouds {
    !static <fields>;
}
-keep,allowobfuscation @interface com.foobnix.android.utils.Objects$*

# The table of contents is cached on disk with Java serialization (CacheZipUtils.loadJavaCache), which
# stores the class name and derives serialVersionUID from the members; keep them so older caches still load.
-keep class com.foobnix.pdf.info.model.OutlineLinkWrapper {
    *;
}

# libMuPDF.so creates these objects and sets their fields by name (Builder/jni).
-keep class org.ebookdroid.core.codec.CodecPageInfo {
    int width;
    int height;
    int dpi;
    int rotation;
    int version;
}
-keep class org.ebookdroid.core.codec.PageTextBox {
    <init>();
    java.lang.String text;
}
-keep class org.ebookdroid.core.codec.PageLink {
    <init>(java.lang.String, int[]);
}
-keep class org.ebookdroid.core.codec.Annotation {
    <init>(float, float, float, float, int, byte[]);
}
-keep class org.ebookdroid.droids.mupdf.codec.TextChar {
    <init>(float, float, float, float, int);
}
-keep class org.ebookdroid.droids.mupdf.codec.exceptions.** {
    <init>(java.lang.String);
}
# liblame.so keeps its encoder pointer in this field.
-keepclassmembers class com.github.axet.lamejni.Lame {
    long handle;
}

# greenDAO (DaoConfig) reads TABLENAME and loads <Dao>$Properties by class name.
-keepnames class com.foobnix.dao2.*Dao
-keepclassmembers class com.foobnix.dao2.*Dao {
    public static java.lang.String TABLENAME;
}
-keep class com.foobnix.dao2.*Dao$Properties {
    public static <fields>;
}

# Created with Class.newInstance(): document codecs (org.ebookdroid.BookType)
# and StarView metafile actions (at.stefl.svm.enumeration.ActionType).
-keepclassmembers class * implements org.ebookdroid.core.codec.CodecContext {
    <init>();
}
-keepclassmembers class * extends at.stefl.svm.object.action.SVMAction {
    <init>();
}

# SettingsManager wraps this interface in a java.lang.reflect.Proxy (org.emdev.utils.listeners.ListenerProxy),
# so it must stay an interface even though nothing implements it.
-keep,allowobfuscation interface org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener {
    *;
}

# VerticalViewPager replaces these private ViewPager fields by reflection.
-keepclassmembers class androidx.viewpager.widget.ViewPager {
    private android.widget.Scroller mScroller;
    private int mFlingDistance;
    private int mMinimumVelocity;
}
