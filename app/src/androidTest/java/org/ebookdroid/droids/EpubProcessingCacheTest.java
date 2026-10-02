package org.ebookdroid.droids;

import com.foobnix.model.AppSP;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.BookCSS;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import static org.junit.Assert.*;

public class EpubProcessingCacheTest {
    @Test public void parallelReaderConversionAndMetadataKeepTheirOwnBookContents() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "epub-concurrent-").toFile();
        com.foobnix.ext.CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        byte[] fixture;
        try (java.io.InputStream input = InstrumentationRegistry.getInstrumentation()
                .getContext().getAssets().open("reader-fixtures/book.epub")) {
            fixture = com.BaseExtractor.getEntryAsByte(input);
        }
        boolean replacement = AppState.get().isEnableTextReplacement;
        String css = BookCSS.get().customCSS2;
        boolean cancelled = com.foobnix.sys.TempHolder.get().loadingCancelled.getAndSet(false);
        AppState.get().isEnableTextReplacement = true;
        BookCSS.get().customCSS2 = "";
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            for (int iteration = 0; iteration < 4; iteration++) {
                File a = new File(root, "reader-" + iteration + ".epub");
                File b = new File(root, "metadata-" + iteration + ".epub");
                writeEpubTitle(fixture, a, "Reader " + iteration);
                writeEpubTitle(fixture, b, "Metadata " + iteration);
                java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
                java.util.concurrent.Future<String> reader = executor.submit(() -> {
                    start.await();
                    EpubContext context = new EpubContext();
                    org.ebookdroid.core.codec.CodecDocument document = context.openDocument(a.getPath(), "");
                    assertNotNull(document);
                    try (java.util.zip.ZipFile converted = new java.util.zip.ZipFile(context.cacheFile)) {
                        String opf = new String(com.BaseExtractor.getEntryAsByte(
                                converted.getInputStream(converted.getEntry("content.opf"))),
                                java.nio.charset.StandardCharsets.UTF_8);
                        return opf;
                    } finally { document.recycle(); }
                });
                java.util.concurrent.Future<String> metadata = executor.submit(() -> {
                    start.await();
                    return com.foobnix.ext.EpubExtractor.get()
                            .getBookMetaInformation(b.getPath()).getTitle();
                });
                start.countDown();
                assertTrue(reader.get(20, java.util.concurrent.TimeUnit.SECONDS)
                        .contains("Reader " + iteration));
                assertEquals("Metadata " + iteration,
                        metadata.get(20, java.util.concurrent.TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
            AppState.get().isEnableTextReplacement = replacement;
            BookCSS.get().customCSS2 = css;
            com.foobnix.sys.TempHolder.get().loadingCancelled.set(cancelled);
            File[] files = root.listFiles();
            if (files != null) for (File file : files) file.delete();
            root.delete();
        }
    }

    private static void writeEpubTitle(byte[] source, File destination, String title) throws Exception {
        try (java.util.zip.ZipInputStream input = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(source));
             java.util.zip.ZipOutputStream output = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(destination))) {
            java.util.zip.ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                output.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
                byte[] content = com.BaseExtractor.getEntryAsByte(input);
                if ("content.opf".equals(entry.getName())) {
                    content = new String(content, java.nio.charset.StandardCharsets.UTF_8)
                            .replace("Disposable fixture", title)
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
                output.write(content);
                output.closeEntry();
            }
        }
    }
    @Test public void reopeningAcrossAnotherBookReusesProcessedOutputAndRevisionChangesIt() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "epub-reuse-").toFile();
        File a = new File(root, "a.epub"), b = new File(root, "b.epub");
        byte[] fixture;
        try (java.io.InputStream input = InstrumentationRegistry.getInstrumentation()
                .getContext().getAssets().open("reader-fixtures/book.epub")) {
            fixture = com.BaseExtractor.getEntryAsByte(input);
        }
        Files.write(a.toPath(), fixture); Files.write(b.toPath(), fixture);
        com.foobnix.ext.CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        boolean replacement = AppState.get().isEnableTextReplacement;
        boolean referenceMode = AppState.get().isReferenceMode;
        String previousCss = BookCSS.get().customCSS2;
        boolean cancelled = com.foobnix.sys.TempHolder.get().loadingCancelled.getAndSet(false);
        AppState.get().isEnableTextReplacement = true;
        AppState.get().isReferenceMode = false;
        BookCSS.get().customCSS2 = "";
        org.ebookdroid.core.codec.CodecDocument document = null;
        File firstOutput = null, secondOutput = null;
        try {
            EpubContext first = new EpubContext();
            document = first.openDocument(a.getPath(), ""); assertNotNull(document);
            firstOutput = first.cacheFile; assertTrue(firstOutput.isFile());
            long originalInode = android.system.Os.stat(firstOutput.getPath()).st_ino;
            document.recycle(); document = null;

            EpubContext other = new EpubContext();
            document = other.openDocument(b.getPath(), ""); assertNotNull(document);
            document.recycle(); document = null;
            assertTrue("Opening B must retain A's reusable output", firstOutput.isFile());

            EpubContext reopened = new EpubContext();
            document = reopened.openDocument(a.getPath(), ""); assertNotNull(document);
            assertEquals(firstOutput, reopened.cacheFile);
            assertEquals("A was processed again", originalInode,
                    android.system.Os.stat(reopened.cacheFile.getPath()).st_ino);
            document.recycle(); document = null;

            File revision = new File(root, "revision.epub");
            try (java.util.zip.ZipInputStream input = new java.util.zip.ZipInputStream(
                    new java.io.ByteArrayInputStream(fixture));
                 java.util.zip.ZipOutputStream output = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(revision))) {
                java.util.zip.ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = input.getNextEntry()) != null) {
                    output.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    output.closeEntry();
                }
                output.putNextEntry(new java.util.zip.ZipEntry("revision.txt"));
                output.write("changed content".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
            }
            Files.write(a.toPath(), Files.readAllBytes(revision.toPath()));
            revision.delete();
            Files.setLastModifiedTime(a.toPath(), FileTime.fromMillis(a.lastModified() + 5000));
            EpubContext changedRevision = new EpubContext();
            document = changedRevision.openDocument(a.getPath(), ""); assertNotNull(document);
            secondOutput = changedRevision.cacheFile;
            assertNotEquals(firstOutput, secondOutput);
            document.recycle(); document = null;

            AppState.get().isEnableTextReplacement = false;
            AppState.get().isReferenceMode = true;
            EpubContext changedSettings = new EpubContext();
            document = changedSettings.openDocument(a.getPath(), ""); assertNotNull(document);
            assertNotEquals(secondOutput, changedSettings.cacheFile);
            document.recycle(); document = null;
        } finally {
            if (document != null) document.recycle();
            AppState.get().isEnableTextReplacement = replacement;
            AppState.get().isReferenceMode = referenceMode;
            BookCSS.get().customCSS2 = previousCss;
            com.foobnix.sys.TempHolder.get().loadingCancelled.set(cancelled);
            if (firstOutput != null) com.foobnix.pdf.info.BookCacheLeases.evict(firstOutput);
            if (secondOutput != null) com.foobnix.pdf.info.BookCacheLeases.evict(secondOutput);
            a.delete(); b.delete(); root.delete();
        }
    }
    @Test public void convertedTextReadersIncludeReplacementRulesInCacheIdentity() {
        boolean enabled = AppState.get().isEnableTextReplacement;
        long hash = AppState.get().textReplacementHash;
        try {
            File fb2 = new Fb2Context().getCacheFileName("fixture.fb2");
            File docx = new DocxContext().getCacheFileName("fixture.docx");
            File rtf = new RtfContext().getCacheFileName("fixture.rtf");
            AppState.get().isEnableTextReplacement = !enabled;
            assertNotEquals(fb2, new Fb2Context().getCacheFileName("fixture.fb2"));
            assertNotEquals(docx, new DocxContext().getCacheFileName("fixture.docx"));
            assertNotEquals(rtf, new RtfContext().getCacheFileName("fixture.rtf"));
            AppState.get().isEnableTextReplacement = enabled;
            AppState.get().textReplacementHash = hash + 1;
            assertNotEquals(fb2, new Fb2Context().getCacheFileName("fixture.fb2"));
            assertNotEquals(docx, new DocxContext().getCacheFileName("fixture.docx"));
            assertNotEquals(rtf, new RtfContext().getCacheFileName("fixture.rtf"));
        } finally {
            AppState.get().isEnableTextReplacement = enabled;
            AppState.get().textReplacementHash = hash;
        }
    }

    @Test public void transformationUsesCapturedOptionsAfterLiveSettingsChange() throws Exception {
        boolean canceled = com.foobnix.sys.TempHolder.get().loadingCancelled.getAndSet(false);
        boolean replacement = AppState.get().isEnableTextReplacement;
        boolean bionic = AppState.get().isBionicMode, hyphens = BookCSS.get().isAutoHypens;
        boolean reference = AppState.get().isReferenceMode, footer = AppState.get().isShowFooterNotesInText;
        boolean experimental = AppState.get().isExperimental;
        try {
            AppState.get().isEnableTextReplacement = true; AppState.get().isBionicMode = true;
            AppState.get().isReferenceMode = false; AppState.get().isShowFooterNotesInText = false;
            AppState.get().isExperimental = false; BookCSS.get().isAutoHypens = false;
            try (EpubProcessingSettings.Scope scope = EpubProcessingSettings.capture()) {
                AppState.get().isEnableTextReplacement = false; AppState.get().isBionicMode = false;
                File directory = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getCacheDir().toPath(), "epub-transform-").toFile();
                File source = new File(directory, "source.epub"), output = new File(directory, "output.part");
                try {
                    try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(source))) {
                        zip.putNextEntry(new java.util.zip.ZipEntry("chapter.xhtml"));
                        zip.write("<html><body><p>testing</p></body></html>".getBytes("UTF-8"));
                        zip.closeEntry();
                    }
                    com.foobnix.ext.EpubExtractor.proccessHypensApache(source.getPath(), output.getPath(), null);
                    try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(output);
                         java.io.InputStream chapter = zip.getInputStream(zip.getEntry("chapter.xhtml"))) {
                        String transformed = new String(com.BaseExtractor.getEntryAsByte(chapter), "UTF-8");
                        assertTrue(transformed, transformed.contains("<b>test</b>ing"));
                    }
                } finally { source.delete(); output.delete(); directory.delete(); }
            }
        } finally {
            AppState.get().isEnableTextReplacement = replacement; AppState.get().isBionicMode = bionic;
            BookCSS.get().isAutoHypens = hyphens; AppState.get().isReferenceMode = reference;
            AppState.get().isShowFooterNotesInText = footer; AppState.get().isExperimental = experimental;
            com.foobnix.sys.TempHolder.get().loadingCancelled.set(canceled);
        }
    }
    @Test public void processingSettingsRemainStableWhileUiSettingsChange() throws Exception {
        boolean footer = AppState.get().isShowFooterNotesInText;
        String language = AppSP.get().hypenLang;
        try {
            String original;
            try (EpubProcessingSettings.Scope scope = EpubProcessingSettings.capture()) {
                original = EpubContext.processingSettingsKey();
                Thread ui = new Thread(() -> {
                    AppState.get().isShowFooterNotesInText = !footer;
                    AppSP.get().hypenLang = "de";
                });
                ui.start(); ui.join(3000); assertFalse(ui.isAlive());
                assertEquals(original, EpubContext.processingSettingsKey());
                assertEquals(footer, EpubProcessingSettings.isShowFooterNotesInText());
                assertEquals(language, EpubProcessingSettings.language());
            }
            assertNotEquals(original, EpubContext.processingSettingsKey());
        } finally { AppState.get().isShowFooterNotesInText = footer; AppSP.get().hypenLang = language; }
    }
    @Test public void simultaneousSourcesDoNotShareAProcessingDestinationRegistration() throws Exception {
        File directory = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "epub-targets-").toFile();
        File saf = new File(directory, "saf-open"); saf.mkdir();
        File source = new File(saf, "source-book.epub");
        boolean footer = AppState.get().isShowFooterNotesInText;
        try {
            File first;
            try (EpubProcessingSettings.Scope scope = EpubProcessingSettings.capture()) {
                first = new EpubContext().getCacheFileName(source.getAbsolutePath());
                AppState.get().isShowFooterNotesInText = !footer;
                assertEquals(first, new EpubContext().getCacheFileName(source.getAbsolutePath()));
            }
            assertNotEquals(first, new EpubContext().getCacheFileName(source.getAbsolutePath()));
        } finally { AppState.get().isShowFooterNotesInText = footer; saf.delete(); directory.delete(); }
    }
    @Test public void changingReplacementSettingsInvalidatesProcessedCacheKey() {
        boolean enabled = AppState.get().isEnableTextReplacement;
        long hash = AppState.get().textReplacementHash;
        try {
            String original = EpubContext.processingSettingsKey();
            assertEquals(original, EpubContext.processingSettingsKey());
            AppState.get().isEnableTextReplacement = !enabled;
            assertNotEquals(original, EpubContext.processingSettingsKey());
            AppState.get().isEnableTextReplacement = enabled;
            AppState.get().textReplacementHash = hash + 1;
            assertNotEquals(original, EpubContext.processingSettingsKey());
        } finally { AppState.get().isEnableTextReplacement = enabled; AppState.get().textReplacementHash = hash; }
    }
    @Test public void hyphenationLanguageAndFooterSettingsInvalidateProcessedCache() {
        String lang = AppSP.get().hypenLang;
        boolean footer = AppState.get().isShowFooterNotesInText;
        try {
            String original = EpubContext.processingSettingsKey(); AppSP.get().hypenLang = "fixture-language";
            assertNotEquals(original, EpubContext.processingSettingsKey()); AppSP.get().hypenLang = lang;
            AppState.get().isShowFooterNotesInText = !footer;
            assertNotEquals(original, EpubContext.processingSettingsKey());
        } finally { AppSP.get().hypenLang = lang; AppState.get().isShowFooterNotesInText = footer; }
    }
    @Test public void processingLanguageNormalizesIsoCodesAndHonorsExplicitDefault() {
        String lang = AppSP.get().hypenLang, defaultLang = AppState.get().defaultHyphenLanguageCode;
        boolean useDefault = AppState.get().isDefaultHyphenLanguage;
        try {
            AppState.get().isDefaultHyphenLanguage = false;
            EpubContext.prepareProcessingLanguage("eng"); assertEquals("en", AppSP.get().hypenLang);
            EpubContext.prepareProcessingLanguage("pt_BR"); assertEquals("pt", AppSP.get().hypenLang);
            AppState.get().isDefaultHyphenLanguage = true; AppState.get().defaultHyphenLanguageCode = "de-DE";
            EpubContext.prepareProcessingLanguage("en"); assertEquals("de", AppSP.get().hypenLang);
        } finally { AppSP.get().hypenLang = lang; AppState.get().defaultHyphenLanguageCode = defaultLang; AppState.get().isDefaultHyphenLanguage = useDefault; }
    }
}
