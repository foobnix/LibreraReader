package com.foobnix.pdf.info;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;

/** A descriptor-backed local path whose basename remains the provider's display name. */
public final class SafFileLink implements AutoCloseable {
    public final File file;
    private final File directory;
    private final ParcelFileDescriptor descriptor;
    private final AutoCloseable lease;

    public SafFileLink(Context context, Uri uri, String displayName) throws Exception {
        this(context, open(context, uri), displayName);
    }

    private static ParcelFileDescriptor open(Context context, Uri uri) throws Exception {
        ParcelFileDescriptor opened = null;
        Exception lastFailure = null;
        for (Uri access : SafDocumentIdentity.accessCandidates(context, uri)) {
            try {
                opened = context.getContentResolver().openFileDescriptor(access, "r");
                if (opened != null) {
                    break;
                }
            } catch (Exception failure) {
                lastFailure = failure;
            }
        }
        if (opened == null && lastFailure != null) throw lastFailure;
        if (opened == null) throw new IOException("Cannot open SAF book: " + uri);
        return opened;
    }

    SafFileLink(Context context, ParcelFileDescriptor opened, String displayName) throws Exception {
        descriptor = opened;
        File temporaryDirectory = null;
        File temporaryLink = null;
        AutoCloseable temporaryLease = null;
        try {
            synchronized (BookCacheLeases.class) {
                temporaryDirectory = File.createTempFile("saf-book-", "", context.getCacheDir());
                if (!temporaryDirectory.delete() || !temporaryDirectory.mkdir()) {
                    throw new IOException("Cannot create SAF link directory");
                }
                temporaryLease = BookCacheLeases.acquire(temporaryDirectory);
            }
            temporaryLink = new File(temporaryDirectory, new File(displayName).getName());
            if (seekableRegularFile(descriptor)) {
                Os.symlink("/proc/self/fd/" + descriptor.getFd(), temporaryLink.getAbsolutePath());
            } else {
                // A pipe cannot be reopened/probed safely. Stage once before codec extraction.
                try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(
                        ParcelFileDescriptor.dup(descriptor.getFileDescriptor()));
                     FileOutputStream output = new FileOutputStream(temporaryLink)) {
                    byte[] buffer = new byte[64 * 1024];
                    long bytes = 0;
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) throw new IOException("SAF read cancelled");
                        bytes += count;
                        if (bytes > 512L * 1024 * 1024) throw new IOException("SAF metadata stream exceeds staging limit");
                        output.write(buffer, 0, count);
                    }
                }
            }
        } catch (Exception e) {
            if (temporaryLink != null) temporaryLink.delete();
            if (temporaryDirectory != null) temporaryDirectory.delete();
            if (temporaryLease != null) temporaryLease.close();
            descriptor.close();
            throw e;
        }
        directory = temporaryDirectory;
        file = temporaryLink;
        lease = temporaryLease;
    }

    static boolean seekableRegularFile(ParcelFileDescriptor descriptor) {
        try {
            if (!OsConstants.S_ISREG(Os.fstat(descriptor.getFileDescriptor()).st_mode)) return false;
            Os.lseek(descriptor.getFileDescriptor(), 0, OsConstants.SEEK_CUR);
            return true;
        } catch (Exception notSeekable) { return false; }
    }

    @Override public void close() throws IOException {
        file.delete();
        directory.delete();
        try { descriptor.close(); }
        finally {
            try { lease.close(); }
            catch (Exception failure) { throw new IOException("Cannot release SAF staging lease", failure); }
        }
    }
}
