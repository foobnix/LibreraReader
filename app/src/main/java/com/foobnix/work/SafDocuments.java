package com.foobnix.work;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.SafDocumentIdentity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** A completed SAF directory listing, including provider revisions. */
final class SafDocuments {
    static final class Document {
        final String name;
        final Uri uri;
        final boolean directory;
        final Long size;
        final Long modified;

        Document(String name, Uri uri, boolean directory, Long size, Long modified) {
            this.name = name;
            this.uri = uri;
            this.directory = directory;
            this.size = size;
            this.modified = modified;
        }

        FileMeta book() {
            FileMeta book = new FileMeta(SafDocumentIdentity.canonical(uri).toString());
            book.setTitle(name);
            book.setPathTxt(name);
            book.setExt(ExtUtils.getFileExtension(name));
            book.setSize(size);
            book.setDate(modified);
            return book;
        }
    }

    static List<Document> list(Context context, Uri parent, BooleanSupplier stopped)
            throws IOException, InterruptedException {
        Uri children = ExtUtils.getChildUri(context, parent);
        if (children == null) throw new IOException("Cannot list SAF folder: " + parent);
        List<Document> documents = new ArrayList<>();
        try (Cursor cursor = SafFolderQuery.query(context.getContentResolver(), children, new String[]{
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED}, stopped)) {
            while (cursor.moveToNext()) {
                if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
                documents.add(new Document(cursor.getString(0),
                        DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(1)),
                        DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2)),
                        cursor.isNull(3) ? null : cursor.getLong(3),
                        cursor.isNull(4) ? null : cursor.getLong(4)));
            }
        }
        return documents;
    }
}
