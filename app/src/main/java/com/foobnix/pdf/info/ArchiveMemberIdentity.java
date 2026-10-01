package com.foobnix.pdf.info;

import android.net.Uri;

import com.foobnix.ext.CacheZipUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;

/** A member's reading identity is independent of each temporary extraction directory. */
public final class ArchiveMemberIdentity {
    private static final String SCHEME = "archive-member";
    private static final String MARKER = ".member-identity";

    private ArchiveMemberIdentity() {}

    public static String create(String archivePath, String entry) {
        String source = ExtUtils.isExteralSD(archivePath)
                ? SafDocumentIdentity.canonical(Uri.parse(archivePath)).toString()
                : new File(archivePath).getAbsolutePath();
        return new Uri.Builder().scheme(SCHEME).authority("librera")
                .appendPath(source).appendPath(shortHash(source) + "-" + entry).build().toString();
    }

    public static boolean isIdentity(String path) {
        return path != null && path.startsWith(SCHEME + "://librera/");
    }

    public static String source(String identity) {
        List<String> segments = segments(identity);
        return segments == null ? null : segments.get(0);
    }

    public static String entry(String identity) {
        List<String> segments = segments(identity);
        if (segments == null) return null;
        String member = segments.get(1);
        return member.startsWith(shortHash(segments.get(0)) + "-")
                ? member.substring(17) : null;
    }

    private static List<String> segments(String identity) {
        if (!isIdentity(identity)) return null;
        List<String> result = Uri.parse(identity).getPathSegments();
        return result.size() == 2 ? result : null;
    }

    public static void mark(File stagingDirectory, String identity) throws IOException {
        Files.write(new File(stagingDirectory, MARKER).toPath(),
                identity.getBytes(StandardCharsets.UTF_8));
    }

    public static String forExtractedFile(File file) {
        if (file == null || CacheZipUtils.CACHE_RECENT == null) return null;
        File directory = file.getParentFile();
        if (directory == null || !CacheZipUtils.CACHE_RECENT.equals(directory.getParentFile())) return null;
        try {
            String identity = new String(Files.readAllBytes(new File(directory, MARKER).toPath()),
                    StandardCharsets.UTF_8);
            return isIdentity(identity) ? identity : null;
        } catch (IOException unavailable) {
            return null;
        }
    }

    public static boolean sourceMayExist(String identity) {
        String source = source(identity);
        return source != null && entry(identity) != null
                && (ExtUtils.isExteralSD(source) || new File(source).isFile());
    }

    private static String shortHash(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(16);
            for (int i = 0; i < 8; i++) result.append(String.format(java.util.Locale.US, "%02x", hash[i] & 0xff));
            return result.toString();
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }
}
