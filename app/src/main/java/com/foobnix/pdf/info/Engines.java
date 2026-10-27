package com.foobnix.pdf.info;

import com.foobnix.LibreraBuildConfig;

/**
 * Which rendering engines this build was made with.
 *
 * The flavours whose names end in JS are built without MuPDF: a PDF is rendered by PDFium, a
 * reflowable book is laid out by foliate-js in a WebView, and a comic archive has its images
 * decoded straight to bitmaps by {@link org.ebookdroid.droids.comic.codec.ComicContext}. The rest
 * of Librera's native code - DjVu, the {@code .doc} and {@code .mobi} converters, the bitmap
 * scaler - is not MuPDF: each part is a library of its own ({@link #NATIVE_LIBRARIES},
 * Builder/jni/Librera-libs.mk), the very same in every flavour, and those flavours carry all of
 * them but {@code libmupdf.so}.
 *
 * This lives apart from {@link AppsConfig}, which holds everything else of the kind, for one
 * reason: AppsConfig loads the native library in a static initializer, so the first read of any
 * of its fields pulls that library in. The engine checks are read from
 * {@link com.foobnix.model.AppState} and from the codec layer, both of which run while the
 * library is being set up, and a class whose only inputs are the flavour's own
 * {@link LibreraBuildConfig} constants can be read at any time.
 */
public final class Engines {

    /**
     * Whether MuPDF is in this build. When it is not, there is no second engine to fall back to
     * and nothing asks the user to choose one: {@link com.foobnix.model.AppState#getPdfEngine()}
     * and {@link com.foobnix.model.AppState#getEpubEngine()} answer with the only engine there is,
     * whatever the stored setting says - which matters because a JS flavour carries the
     * application ID of the flavour it replaces, so it is installed over it and reads its
     * settings.
     */
    public static final boolean IS_MUPDF = !LibreraBuildConfig.FLAVOR.endsWith("JS");

    /**
     * Whether the reader picks the engines: the fdroid and pro flavours, which carry MuPDF and the
     * JS engines (PDFium, foliate-js) side by side. Every other MuPDF flavour renders with MuPDF
     * alone, as it always has, and a JS flavour has only the JS engines - in both there is
     * nothing to choose, so the settings are not shown and a stored choice is not read.
     */
    public static final boolean IS_ENGINE_CHOICE = IS_MUPDF
            && ("fdroid".equals(LibreraBuildConfig.FLAVOR) || "pro".equals(LibreraBuildConfig.FLAVOR));

    /** MuPDF and its JNI, {@code lib<this>.so}, in the flavours built with it. */
    public static final String MUPDF_LIBRARY = "mupdf";

    /**
     * The native libraries every flavour carries and loads, with MuPDF or without:
     * {@code lib<name>.so} each. libarchive.so and liblame.so are loaded by the classes that use
     * them ({@link com.foobnix.ext.NativeArchive}, the MP3 encoder).
     */
    public static final String[] NATIVE_LIBRARIES = {"djvu", "mobi", "antiword", "hqx"};

    private Engines() {
    }
}
