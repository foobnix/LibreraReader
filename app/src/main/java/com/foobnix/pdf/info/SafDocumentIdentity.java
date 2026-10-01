package com.foobnix.pdf.info;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A provider document has one reading identity even when several tree grants reach it. */
public final class SafDocumentIdentity {

    private SafDocumentIdentity() {}

    public static Uri canonical(Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme())) return uri;
        try {
            String documentId = DocumentsContract.getDocumentId(uri);
            if (documentId == null) return uri;
            return DocumentsContract.buildDocumentUri(uri.getAuthority(), documentId);
        } catch (IllegalArgumentException invalid) {
            return uri;
        }
    }

    /** Try addresses derived from live grants; an identity itself never creates permission. */
    public static List<Uri> accessCandidates(Context context, Uri identity) {
        Set<Uri> candidates = new LinkedHashSet<>();
        if (identity == null) return new ArrayList<>();
        Uri canonical = canonical(identity);
        if (!canonical.equals(identity)) candidates.add(identity);
        try {
            String documentId = DocumentsContract.getDocumentId(canonical);
            for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
                Uri grant = permission.getUri();
                if (permission.isReadPermission() && canonical.getAuthority().equals(grant.getAuthority())) {
                    try {
                        DocumentsContract.getTreeDocumentId(grant);
                        candidates.add(DocumentsContract.buildDocumentUriUsingTree(grant, documentId));
                    } catch (IllegalArgumentException notTreeGrant) {
                        if (canonical.equals(grant)) candidates.add(grant);
                    }
                }
            }
        } catch (IllegalArgumentException notDocument) {
            // Other content providers keep their original URI as their identity.
        }
        candidates.add(canonical);
        return new ArrayList<>(candidates);
    }

    public static Uri readableAccess(Context context, Uri identity) throws IOException {
        IOException lastFailure = null;
        for (Uri candidate : accessCandidates(context, identity)) {
            try (ParcelFileDescriptor descriptor = context.getContentResolver()
                    .openFileDescriptor(candidate, "r")) {
                if (descriptor != null) {
                    return candidate;
                }
            } catch (Exception failure) {
                lastFailure = new IOException("Cannot read " + candidate, failure);
            }
        }
        if (lastFailure != null) throw lastFailure;
        throw new IOException("Cannot read " + identity);
    }

    public static InputStream openInputStream(Context context, Uri identity) throws IOException {
        IOException lastFailure = null;
        for (Uri candidate : accessCandidates(context, identity)) {
            try {
                InputStream input = context.getContentResolver().openInputStream(candidate);
                if (input != null) {
                    return input;
                }
            } catch (Exception failure) {
                lastFailure = new IOException("Cannot read " + candidate, failure);
            }
        }
        if (lastFailure != null) throw lastFailure;
        throw new IOException("Cannot read " + identity);
    }
}
