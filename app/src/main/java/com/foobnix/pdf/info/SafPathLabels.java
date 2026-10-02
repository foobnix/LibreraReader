package com.foobnix.pdf.info;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.lang.ref.WeakReference;
import android.os.SystemClock;
import java.util.concurrent.ConcurrentHashMap;
import android.os.Build;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** User-facing provider names; the original URI remains the identity and permission key. */
public final class SafPathLabels {
    private static final ExecutorService LOOKUPS = Executors.newSingleThreadExecutor();
    private static final ConcurrentHashMap<String, Long> RESOLVED_AT = new ConcurrentHashMap<>();

    public static void bind(TextView view, String path) {
        bind(view, path, "");
    }

    public static void bind(TextView view, String path, String suffix) {
        bind(view, path, suffix, false);
    }

    /** Resolve missing ancestry on demand for a location field, never for a scrolling row. */
    public static void bindLocation(TextView view, String path) {
        bind(view, path, "", true);
    }

    private static void bind(TextView view, String path, String suffix, boolean recover) {
        Object binding = new Object();
        view.setTag(R.id.browserPath, binding);
        view.setText(displayName(view.getContext(), path) + suffix);
        WeakReference<TextView> target = new WeakReference<>(view);
        refresh(view.getContext(), path, recover, label -> {
            TextView current = target.get();
            if (current != null && current.getTag(R.id.browserPath) == binding) {
                current.setText(label + suffix);
            }
        });
    }

    public static void refresh(Context context, String path, Consumer<String> onLabel) {
        refresh(context, path, false, onLabel);
    }

    private static void refresh(Context context, String path, boolean recover, Consumer<String> onLabel) {
        if (path == null || !ExtUtils.isExteralSD(path)) return;
        Context app = context.getApplicationContext();
        LOOKUPS.execute(() -> {
            String label = resolve(app, path, recover);
            new Handler(Looper.getMainLooper()).post(() -> onLabel.accept(label));
        });
    }

    private SafPathLabels() {
    }

    public static String displayName(Context context, String path) {
        if (path == null) return "";
        if (!ExtUtils.isExteralSD(path)) return path;
        return SafStateStore.get(context, "saf-folder-labels").getString(path, context.getString(R.string.folder));
    }

    static String resolve(Context context, String path) {
        return resolve(context, path, false);
    }

    static String resolve(Context context, String path, boolean recover) {
        SafStateStore cache = SafStateStore.get(context, "saf-folder-labels");
        String cacheKey = com.foobnix.model.AppProfile.getCurrent() + "|" + path;
        Long resolvedAt = RESOLVED_AT.get(cacheKey);
        if (!recover && resolvedAt != null && SystemClock.elapsedRealtime() - resolvedAt < 30_000) {
            return cache.getString(path, context.getString(R.string.folder));
        }
        try {
            Uri identity = documentUri(path);
            // Library books use grant-neutral identities; use their live tree grants
            // to read metadata, just as opening the book does.
            for (Uri document : SafDocumentIdentity.accessCandidates(context, identity)) {
                try {
                    String name = queryName(context, document);
                    if (name == null || name.trim().isEmpty()) continue;
                    ProviderInfo provider = context.getPackageManager().resolveContentProvider(document.getAuthority(), 0);
                    CharSequence providerName = provider == null ? null
                            : provider.loadLabel(context.getPackageManager());
                    String location = documentPath(context, document, name);
                    if (recover && location.startsWith("…") && DocumentsContract.isTreeUri(document)) {
                        recoverHierarchy(context, document);
                        location = recordedPath(context, document, name);
                    }
                    String label = providerName == null || providerName.length() == 0
                            ? location : providerName + " / " + location;
                    cache.edit().putString(path, label).commit();
                    if (RESOLVED_AT.size() >= 1024) RESOLVED_AT.clear();
                    RESOLVED_AT.put(cacheKey, SystemClock.elapsedRealtime());
                    return label;
                } catch (Exception unavailable) {
                    // Another persisted grant may still allow this document's metadata.
                }
            }
        } catch (Exception ignored) {
            // A disconnected provider or revoked grant must not prevent opening preferences.
        }
        return cache.getString(path, context.getString(R.string.folder));
    }

    static Uri documentUri(String path) {
        // java.io.File may have collapsed content:// to content:/ for display callers.
        Uri uri = Uri.parse(path.startsWith("content://") ? path
                : "content://" + path.substring("content:/".length()));
        if (DocumentsContract.isTreeUri(uri)) {
            try {
                DocumentsContract.getDocumentId(uri);
            } catch (IllegalArgumentException treeOnly) {
                return DocumentsContract.buildDocumentUriUsingTree(uri,
                        DocumentsContract.getTreeDocumentId(uri));
            }
        }
        return uri;
    }

    /** Record provider-supplied relationships, never infer parents from opaque document IDs. */
    public static void rememberChildren(Context context, Uri parent, Map<Uri, String> children) {
        var editor = SafStateStore.get(context, "saf-document-hierarchy").edit();
        String parentKey = SafDocumentIdentity.canonical(documentUri(parent.toString())).toString();
        for (Map.Entry<Uri, String> child : children.entrySet()) {
            if (child.getValue() == null || child.getValue().trim().isEmpty()) continue;
            String key = SafDocumentIdentity.canonical(child.getKey()).toString();
            editor.putString(key + "|parent", parentKey);
            editor.putString(key + "|name", child.getValue());
        }
        editor.commit();
        RESOLVED_AT.clear();
    }

    private static String documentPath(Context context, Uri document, String leaf) {
        if (Build.VERSION.SDK_INT >= 26 && DocumentsContract.isTreeUri(document)) {
            try {
                DocumentsContract.Path path = DocumentsContract.findDocumentPath(context.getContentResolver(), document);
                if (path != null && !path.getPath().isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (String id : path.getPath()) {
                        String name = queryName(context, DocumentsContract.buildDocumentUriUsingTree(document, id));
                        if (name == null || name.trim().isEmpty()) throw new IllegalStateException("Unnamed ancestor");
                        names.add(name);
                    }
                    return String.join(" / ", names);
                }
            } catch (Exception unsupported) {
                // Some providers, including Nextcloud versions, do not implement this API.
            }
        }
        return recordedPath(context, document, leaf);
    }

    static String recordedPath(Context context, Uri document, String leaf) {
        var hierarchy = SafStateStore.get(context, "saf-document-hierarchy");
        String key = SafDocumentIdentity.canonical(document).toString();
        String root = DocumentsContract.isTreeUri(document) ? SafDocumentIdentity.canonical(
                DocumentsContract.buildDocumentUriUsingTree(document, DocumentsContract.getTreeDocumentId(document))).toString() : null;
        List<String> names = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        names.add(leaf);
        while (!key.equals(root)) {
            if (!visited.add(key) || visited.size() > 128) {
                names.add("…");
                break;
            }
            String parent = hierarchy.getString(key + "|parent", null);
            if (parent == null) {
                names.add("…");
                break;
            }
            String name = hierarchy.getString(parent + "|name", null);
            if (name == null && DocumentsContract.isTreeUri(document)) {
                try {
                    name = queryName(context, DocumentsContract.buildDocumentUriUsingTree(document,
                            DocumentsContract.getDocumentId(Uri.parse(parent))));
                } catch (Exception unavailable) {
                    // Keep the known part of the hierarchy when a provider is offline.
                }
            }
            if (name == null || name.trim().isEmpty()) {
                names.add("…");
                break;
            }
            names.add(name);
            key = parent;
        }
        Collections.reverse(names);
        return String.join(" / ", names);
    }

    private static void recoverHierarchy(Context context, Uri target) {
        if (Build.VERSION.SDK_INT < 29) return;
        try {
            String targetId = DocumentsContract.getDocumentId(target);
            Uri folder = DocumentsContract.buildDocumentUriUsingTree(target, DocumentsContract.getTreeDocumentId(target));
            queryName(context, folder);
            Set<Uri> visited = new HashSet<>();
            while (visited.add(folder) && visited.size() <= 128) {
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folder, DocumentsContract.getDocumentId(folder));
                Map<Uri, String> names = new java.util.LinkedHashMap<>();
                List<Uri> directories = new ArrayList<>();
                boolean found = false;
                try (Cursor cursor = context.getContentResolver().query(childrenUri, new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
                    while (cursor != null && cursor.moveToNext()) {
                        String id = cursor.getString(0);
                        Uri child = DocumentsContract.buildDocumentUriUsingTree(folder, id);
                        names.put(child, cursor.getString(1));
                        if (id.equals(targetId)) found = true;
                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))) directories.add(child);
                    }
                }
                rememberChildren(context, folder, names);
                if (found) return;
                Uri next = null;
                for (Uri directory : directories) {
                    if (DocumentsContract.isChildDocument(context.getContentResolver(), directory, target)) {
                        next = directory;
                        break;
                    }
                }
                if (next == null) return;
                folder = next;
            }
        } catch (Exception unsupportedOrUnavailable) {
            // Normal scanning/browsing will fill the hierarchy for providers without this API.
        }
    }

    private static String queryName(Context context, Uri document) {
        try (Cursor cursor = context.getContentResolver().query(document,
                new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) {
                    SafStateStore.get(context, "saf-document-hierarchy").edit()
                            .putString(SafDocumentIdentity.canonical(document).toString() + "|name", name).commit();
                }
                return name;
            }
        }
        return null;
    }
}
