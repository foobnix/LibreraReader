package com.foobnix.pdf.info;

import android.content.Context;
import android.net.Uri;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CalirbeExtractor;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import org.json.JSONObject;

public class SafOpfRegistry {

    public static class Entry {
        public final Uri opfUri;
        public final Map<String, Uri> siblingByLowerName;
        public final String revision;

        public Entry(Uri opfUri, Map<String, Uri> siblingByLowerName) {
            this(opfUri, siblingByLowerName, "");
        }

        public Entry(Uri opfUri, Map<String, Uri> siblingByLowerName, String revision) {
            this.opfUri = opfUri;
            this.siblingByLowerName = Collections.unmodifiableMap(new HashMap<>(siblingByLowerName));
            this.revision = revision;
        }
    }

    private static final Map<String, Entry> pending = new HashMap<>();
    private static final Map<String, Entry> memory = new java.util.LinkedHashMap<>(64, .75f, true);
    private static final java.util.Set<String> loading = new java.util.HashSet<>();
    private static final java.util.concurrent.ExecutorService READS = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static volatile Context context;
    private static long generation;

    /** Binding from a cover row must neither open the database nor flush pending writes. */
    public static void restore(Context current) { context = current.getApplicationContext(); }

    public static void save(Context current) {
        restore(current);
        Map<String, Entry> copy;
        synchronized (pending) { copy = new HashMap<>(pending); pending.clear(); }
        updateAll(copy);
    }

    private static void cache(String key, Entry entry, long expectedGeneration) {
        synchronized (pending) {
            if (generation != expectedGeneration) return;
            memory.put(key, entry);
            while (memory.size() > 256) memory.remove(memory.keySet().iterator().next());
        }
    }

    static Map<String, Entry> decode(String json) throws org.json.JSONException {
        Map<String, Entry> result = new HashMap<>();
        JSONObject saved = new JSONObject(json);
        var books = saved.keys();
        while (books.hasNext()) {
            String book = books.next();
            JSONObject entry = saved.getJSONObject(book);
            JSONObject siblings = entry.getJSONObject("siblings");
            Map<String, Uri> uris = new HashMap<>();
            var names = siblings.keys();
            while (names.hasNext()) {
                String name = names.next();
                uris.put(name, Uri.parse(siblings.getString(name)));
            }
            result.put(book, new Entry(Uri.parse(entry.getString("opf")), uris, entry.optString("revision")));
        }
        return result;
    }

    /** A legacy tree-grant key and its document key refer to the same book. */
    static Map<String, Entry> normalizedKeys(Map<String, Entry> saved) {
        Map<String, Entry> result = new HashMap<>();
        saved.forEach((book, entry) -> {
            if (book.equals(canonicalKey(book))) result.put(book, entry);
        });
        saved.forEach((book, entry) -> result.putIfAbsent(canonicalKey(book), entry));
        return result;
    }

    static String encode(Map<String, Entry> entries) throws org.json.JSONException {
        JSONObject saved = new JSONObject();
        for (Map.Entry<String, Entry> book : entries.entrySet()) {
            JSONObject siblings = new JSONObject();
            for (Map.Entry<String, Uri> sibling : book.getValue().siblingByLowerName.entrySet()) {
                siblings.put(sibling.getKey(), sibling.getValue().toString());
            }
            saved.put(book.getKey(), new JSONObject().put("opf", book.getValue().opfUri.toString())
                    .put("siblings", siblings).put("revision", book.getValue().revision));
        }
        return saved.toString();
    }

    public static void register(String bookUri, Entry entry) {
        updateAll(Collections.singletonMap(bookUri, entry));
    }

    /** One optimization-store transaction per discovery batch, only for changed records. */
    public static void updateAll(Map<String, Entry> entries) {
        Context current = context;
        if (current == null) {
            synchronized (pending) {
                for (Map.Entry<String, Entry> entry : entries.entrySet()) {
                    String key = canonicalKey(entry.getKey());
                    if (entry.getValue() == null) pending.remove(key);
                    else pending.put(key, entry.getValue());
                }
            }
            return;
        }
        synchronized (pending) {
            for (String book : entries.keySet()) pending.remove(canonicalKey(book));
        }
        SafStateStore store = SafStateStore.get(current, "SafSidecars");
        SafStateStore.Editor editor = store.edit();
        Map<String, Entry> changed = new HashMap<>();
        try {
            for (Map.Entry<String, Entry> entry : entries.entrySet()) {
                String key = canonicalKey(entry.getKey());
                String value = entry.getValue() == null ? null
                        : encode(Collections.singletonMap(key, entry.getValue()));
                long observedGeneration;
                synchronized (pending) { observedGeneration = generation; }
                if (java.util.Objects.equals(value, store.getString(key, null))) {
                    cache(store.identity() + "|" + key, entry.getValue(), observedGeneration);
                    continue;
                }
                editor.putString(key, value);
                changed.put(key, entry.getValue());
            }
            if (changed.isEmpty()) return;
            long version;
            synchronized (pending) {
                version = ++generation;
                for (String key : changed.keySet()) memory.remove(store.identity() + "|" + key);
            }
            if (editor.commit()) for (Map.Entry<String, Entry> entry : changed.entrySet())
                cache(store.identity() + "|" + entry.getKey(), entry.getValue(), version);
        } catch (Exception failure) { LOG.e(failure); }
    }

    public static Entry get(String bookUri) {
        if (!ExtUtils.isExteralSD(bookUri)) return null;
        String key = canonicalKey(bookUri);
        Context current = context;
        if (current == null) { synchronized (pending) { return pending.get(key); } }
        SafStateStore store = SafStateStore.get(current, "SafSidecars");
        String memoryKey = store.identity() + "|" + key;
        long version;
        synchronized (pending) {
            if (pending.containsKey(key)) return pending.get(key);
            if (memory.containsKey(memoryKey)) return memory.get(memoryKey);
            version = generation;
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                if (loading.add(memoryKey)) READS.execute(() -> {
                    try { load(store, key, memoryKey, version); }
                    finally { synchronized (pending) { loading.remove(memoryKey); } }
                });
                return null;
            }
        }
        return load(store, key, memoryKey, version);
    }

    private static Entry load(SafStateStore store, String key, String memoryKey, long version) {
        try {
            Entry entry = decode(store.getString(key, "{}")).get(key);
            cache(memoryKey, entry, version);
            return entry;
        } catch (Exception failure) { LOG.e(failure); return null; }
    }

    public static void unregister(String bookUri) {
        updateAll(Collections.singletonMap(bookUri, null));
    }

    private static String canonicalKey(String bookUri) {
        if (bookUri == null) return null;
        try {
            return SafDocumentIdentity.canonical(Uri.parse(bookUri)).toString();
        } catch (RuntimeException invalid) {
            return bookUri;
        }
    }

    public static void clear() {
        synchronized (pending) { generation++; pending.clear(); memory.clear(); context = null; }
    }

    public static CalirbeExtractor.CoverResolver coverResolver(final Context context,
                                                               final Map<String, Uri> siblingByLowerName) {
        return coverResolver(context, siblingByLowerName, failure -> {});
    }

    public static CalirbeExtractor.CoverResolver coverResolver(final Context context,
                                                               final Map<String, Uri> siblingByLowerName,
                                                               final java.util.function.Consumer<Exception> onFailure) {
        return href -> {
            if (href == null) return null;
            String key = href.toLowerCase(Locale.US);
            int slash = key.lastIndexOf('/');
            if (slash >= 0) key = key.substring(slash + 1);
            Uri imgUri = siblingByLowerName.get(key);
            if (imgUri == null) return null;
            try (InputStream is = SafDocumentIdentity.openInputStream(context, imgUri)) {
                if (is == null) return null;
                return BaseExtractor.getEntryAsByte(is);
            } catch (Exception e) {
                LOG.e(e);
                onFailure.accept(e);
                return null;
            }
        };
    }
}
