package com.foobnix.tts;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.opds.OPDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The internet radio the audio player can play beside a book: a list of sources, each a stream's
 * own address or an .m3u playlist of streams. A playlist is read and every station in it listed,
 * by the name its #EXTINF line gives. The sources are kept in {@link AppState#streamSources}.
 */
public final class StreamPlaylist {

    /** The list offered from the start: music radio stations of many kinds. */
    public static final String DEFAULT =
            "https://raw.githubusercontent.com/junguler/m3u-radio-music-playlists/main/various.m3u";

    /** A station: what it is called and where its stream is. */
    public static final class Entry {
        public final String title;
        public final String url;
        public final String source;

        Entry(String title, String url, String source) {
            this.title = title;
            this.url = url;
            this.source = source;
        }
    }

    /** The stations of each source read so far in this run. */
    private static final Map<String, List<Entry>> loaded = new ConcurrentHashMap<String, List<Entry>>();

    /** The names of the stations played, for the player line and the notification. */
    private static final Map<String, String> titles = new ConcurrentHashMap<String, String>();

    private StreamPlaylist() {
    }

    public static boolean isStream(String path) {
        if (path == null) {
            return false;
        }
        final String lower = path.toLowerCase(Locale.US);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    public static List<String> sources() {
        final List<String> sources = new ArrayList<String>();
        final String saved = AppState.get().streamSources;
        if (saved != null) {
            for (String line : saved.split("\n")) {
                final String source = line.trim();
                if (source.length() > 0 && !sources.contains(source)) {
                    sources.add(source);
                }
            }
        }
        return sources;
    }

    /** Adds a source; false when it is not a web address or is there already. */
    public static boolean add(String address) {
        final String source = normalize(address);
        if (!isStream(source)) {
            return false;
        }
        final List<String> sources = sources();
        if (sources.contains(source)) {
            return false;
        }
        sources.add(source);
        save(sources);
        return true;
    }

    public static void remove(String source) {
        final List<String> sources = sources();
        sources.remove(source);
        save(sources);
        loaded.remove(source);
    }

    private static void save(List<String> sources) {
        AppState.get().streamSources = TxtUtils.joinList("\n", sources);
    }

    /**
     * A web address as the player can read it. A file on GitHub is linked to as its page; the
     * file itself is on raw.githubusercontent.com.
     */
    public static String normalize(String address) {
        if (address == null) {
            return null;
        }
        String url = address.trim();
        final String github = "https://github.com/";
        if (url.startsWith(github) && url.contains("/blob/")) {
            url = "https://raw.githubusercontent.com/" + url.substring(github.length()).replaceFirst("/blob/", "/");
        }
        return url;
    }

    /** The stations a source holds, read once a run. Not on the main thread: it goes online. */
    public static List<Entry> entries(String source) {
        final List<Entry> known = loaded.get(source);
        if (known != null) {
            return known;
        }
        List<Entry> entries;
        if (isPlaylist(source)) {
            entries = read(source);
        } else {
            entries = Collections.singletonList(new Entry(name(source), source, source));
        }
        loaded.put(source, entries);
        return entries;
    }

    /** Whether the stations of a source are already read. */
    public static boolean isLoaded(String source) {
        return loaded.containsKey(source);
    }

    /** A playlist rather than a stream, by its name. */
    static boolean isPlaylist(String source) {
        final String path = pathOf(source);
        return path.endsWith(".m3u") || path.endsWith(".m3u8") || path.endsWith(".pls");
    }

    private static List<Entry> read(String source) {
        final List<Entry> entries = new ArrayList<Entry>();
        try {
            final String text = OPDS.getHttpResponse(source, "", "");
            if (TxtUtils.isEmpty(text) || OPDS.CODE_401.equals(text)) {
                return entries;
            }
            if (text.contains("#EXT-X-")) {
                // an HLS playlist is one stream in pieces, not a list of stations
                entries.add(new Entry(name(source), source, source));
                return entries;
            }
            if (pathOf(source).endsWith(".pls")) {
                return readPls(text, source);
            }
            String title = null;
            for (String raw : text.split("\r?\n")) {
                final String line = raw.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#EXTINF")) {
                    final int comma = line.indexOf(',');
                    title = comma >= 0 ? line.substring(comma + 1).trim() : null;
                } else if (!line.startsWith("#")) {
                    final String url = resolve(source, line);
                    if (isStream(url)) {
                        entries.add(new Entry(TxtUtils.isNotEmpty(title) ? title : name(url), url, source));
                    }
                    title = null;
                }
            }
        } catch (Exception e) {
            LOG.e(e, source);
        }
        return entries;
    }

    private static List<Entry> readPls(String text, String source) {
        final List<Entry> entries = new ArrayList<Entry>();
        final Map<String, String> files = new java.util.TreeMap<String, String>();
        final Map<String, String> names = new java.util.HashMap<String, String>();
        for (String raw : text.split("\r?\n")) {
            final String line = raw.trim();
            final int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }
            final String key = line.substring(0, equals).trim().toLowerCase(Locale.US);
            final String value = line.substring(equals + 1).trim();
            if (key.startsWith("file")) {
                files.put(String.format(Locale.US, "%06d", toInt(key.substring(4))), value);
            } else if (key.startsWith("title")) {
                names.put(String.format(Locale.US, "%06d", toInt(key.substring(5))), value);
            }
        }
        for (Map.Entry<String, String> file : files.entrySet()) {
            final String url = resolve(source, file.getValue());
            if (isStream(url)) {
                final String title = names.get(file.getKey());
                entries.add(new Entry(TxtUtils.isNotEmpty(title) ? title : name(url), url, source));
            }
        }
        return entries;
    }

    /** Remembers the name of a station about to be played. */
    public static void playing(Entry entry) {
        titles.put(entry.url, entry.title);
        AppState.get().streamTitle = entry.title;
    }

    /** The name of a stream: the one it had in its playlist, else its address. */
    public static String titleOf(String url) {
        final String title = titles.get(url);
        if (title != null) {
            return title;
        }
        if (TxtUtils.isNotEmpty(AppState.get().streamTitle) && url != null) {
            return AppState.get().streamTitle;
        }
        return name(url);
    }

    /** The station before or after this one in its playlist, or null. */
    public static Entry neighbour(String url, int step) {
        for (List<Entry> entries : loaded.values()) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).url.equals(url)) {
                    final int next = i + step;
                    return next >= 0 && next < entries.size() ? entries.get(next) : null;
                }
            }
        }
        return null;
    }

    /** What a source is called in the list: its file name, or its host. */
    public static String name(String url) {
        if (url == null) {
            return "";
        }
        String path = url.replaceFirst("^[a-zA-Z]+://", "");
        final int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        final int slash = path.lastIndexOf('/');
        final String last = slash >= 0 ? path.substring(slash + 1) : path;
        return last.length() > 0 ? last : path;
    }

    private static String pathOf(String url) {
        String path = url.toLowerCase(Locale.US);
        final int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        return path;
    }

    private static String resolve(String base, String reference) {
        if (isStream(reference)) {
            return reference;
        }
        try {
            return new java.net.URL(new java.net.URL(base), reference).toString();
        } catch (Exception e) {
            return reference;
        }
    }

    private static int toInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
