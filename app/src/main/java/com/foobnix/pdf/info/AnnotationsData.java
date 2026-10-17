package com.foobnix.pdf.info;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppAnnotation;
import com.foobnix.model.AppProfile;

import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Where Librera keeps the marks the reader put on a book's text.
 *
 * It follows {@link BookmarksData} exactly, because that is how everything in the profile is kept
 * and how it reaches other devices: one JSON file per device under
 * {@code profile.<name>/device.<MODEL>/}, written only by that device, and read by taking the
 * union of every device's file. A folder shared through Drive or Syncthing then carries the marks
 * without any of them ever writing to the same file.
 *
 * One deliberate difference from the bookmarks: the marks are de-duplicated on the way out. A
 * bookmark that exists in two devices' files is listed twice, which is untidy in a list but
 * harmless; a highlight listed twice is drawn twice, over itself, and comes out darker than the
 * colour that was chosen.
 */
public class AnnotationsData {

    private static final AnnotationsData instance = new AnnotationsData();

    public static AnnotationsData get() {
        return instance;
    }

    /** Adds a mark, or replaces the one with the same {@link AppAnnotation#t}. */
    public void add(final AppAnnotation annotation) {
        if (annotation == null || annotation.path == null) {
            return;
        }
        final File file = annotation.file != null ? annotation.file : AppProfile.syncAnnotations;
        try {
            final LinkedJSONObject obj = IO.readJsonObject(file);
            obj.put("" + annotation.t, Objects.toJSONObject(annotation));
            IO.writeObjSync(file, obj);
            LOG.d("AnnotationsData add", annotation, file.getName());
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /** Removes a mark from the device file it was read from. */
    public void remove(final AppAnnotation annotation) {
        if (annotation == null) {
            return;
        }
        final File file = annotation.file != null ? annotation.file : AppProfile.syncAnnotations;
        try {
            final LinkedJSONObject obj = IO.readJsonObject(file);
            obj.remove("" + annotation.t);
            IO.writeObjSync(file, obj);
            LOG.d("AnnotationsData remove", annotation, file.getName());
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /**
     * Every mark on one book, from every device, oldest first.
     *
     * The book is matched by file name rather than by path, as the bookmarks are: the same book
     * sits in a different folder on each device.
     */
    public List<AppAnnotation> byBook(final String bookPath) {
        final List<AppAnnotation> result = new ArrayList<AppAnnotation>();
        if (bookPath == null) {
            return result;
        }
        final String name = ExtUtils.getFileName(bookPath);
        final Set<Long> seen = new HashSet<Long>();
        for (File file : AppProfile.getAllFiles(AppProfile.APP_ANNOTATIONS_JSON)) {
            try {
                final LinkedJSONObject obj = IO.readJsonObject(file);
                final Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    final String key = keys.next();
                    final AppAnnotation annotation = new AppAnnotation();
                    Objects.loadFromJson(annotation, obj.getJSONObject(key));
                    if (annotation.path == null) {
                        continue;
                    }
                    if (!name.equals(ExtUtils.getFileName(annotation.getPath()))) {
                        continue;
                    }
                    // The same mark copied onto two devices is still one mark
                    if (!seen.add(annotation.t)) {
                        continue;
                    }
                    annotation.file = file;
                    annotation.path = bookPath;
                    result.add(annotation);
                }
            } catch (Exception e) {
                LOG.e(e, file);
            }
        }
        Collections.sort(result, new Comparator<AppAnnotation>() {
            @Override
            public int compare(final AppAnnotation a, final AppAnnotation b) {
                return Long.compare(a.t, b.t);
            }
        });
        return result;
    }

    /** Whether this book has any marks at all - cheap enough to ask before every page is drawn. */
    public boolean hasAny(final String bookPath) {
        return !byBook(bookPath).isEmpty();
    }

    /** Drops every mark on a book, for "clear" in the UI. */
    public void removeAll(final String bookPath) {
        for (AppAnnotation annotation : byBook(bookPath)) {
            remove(annotation);
        }
    }

    /** Where the device's own file lives; created on demand, as the other profile files are. */
    static File file() {
        if (AppProfile.syncAnnotations == null) {
            CacheZipUtils.createAllCacheDirs();
        }
        return AppProfile.syncAnnotations;
    }
}
