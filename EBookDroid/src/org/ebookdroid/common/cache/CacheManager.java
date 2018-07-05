package org.ebookdroid.common.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.ebookdroid.BookType;
import org.ebookdroid.LibreraApp;
import org.emdev.utils.StringUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

public class CacheManager {
    private static Context s_context;

    public static String getFilePathFromAttachmentIfNeed(Activity activity) {
        try {
            if (activity != null && activity.getIntent() != null && "content".equals(activity.getIntent().getScheme())) {

                String fileName = getFileName(activity.getIntent().getData());
                if (fileName != null) {
                    final File tempFile = CacheManager.createTempFile(activity.getIntent().getData(), fileName);
                    return tempFile.getAbsolutePath();
                } else {
                    String mime = activity.getIntent().getType();
                    if (mime == null) {
                        mime = ExtUtils.getMimeTypeByUri(activity.getIntent().getData());
                    }
                    if (mime != null) {
                        BookType bookType = BookType.getByMimeType(mime);
                        if (bookType != null) {
                            final File tempFile = CacheManager.createTempFile(activity.getIntent().getData(), "book." + bookType.getExt());
                            return tempFile.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    public static String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = null;
            try {
                cursor = LibreraApp.context.getContentResolver().query(uri, new String[] { OpenableColumns.DISPLAY_NAME }, null, null, null);
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static PageCacheFile getPageFile(final String path, int pages) {
        long lastModified = new File(path).lastModified();
        final String md5 = StringUtils.md5(path + lastModified + pages + AppState.get().isFullScreen);
        LOG.d("TEST", "LAST" + md5);
        final File cacheDir = s_context.getFilesDir();
        return new PageCacheFile(cacheDir, md5 + ".cache");
    }

    public static File createTempFile(final Uri uri, String ext) throws IOException {
        LOG.d("createTempFile", uri);

        final File cacheDir = s_context.getFilesDir();
        // final File tempfile = File.createTempFile("temp", ext, cacheDir);
        final File tempfile = new File(cacheDir, ExtUtils.getFileName(ext));
        LOG.d("TEMP_FILE", tempfile);
        tempfile.deleteOnExit();

        final InputStream source = s_context.getContentResolver().openInputStream(uri);
        copy(source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static void clearAllTemp() {
        try {
            final File cacheDir = s_context.getFilesDir();
            LOG.d("Cache dir for TEMP_FILE", cacheDir.getPath());
            CacheZipUtils.removeFiles(cacheDir.listFiles());
            LOG.d("remove TEMP_FILE files");
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void init(Context eBookDroidApp) {
        s_context = eBookDroidApp;
    }

    public static void copy(final InputStream source, final OutputStream target) throws IOException {
        ReadableByteChannel in = null;
        WritableByteChannel out = null;
        try {
            in = Channels.newChannel(source);
            out = Channels.newChannel(target);
            final ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);
            while (in.read(buf) > 0) {
                buf.flip();
                out.write(buf);
                buf.flip();
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        }
    }

}
