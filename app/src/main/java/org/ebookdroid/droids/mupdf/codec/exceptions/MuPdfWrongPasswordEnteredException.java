package org.ebookdroid.droids.mupdf.codec.exceptions;

public class MuPdfWrongPasswordEnteredException extends MuPdfPasswordException {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 8124041062263037442L;

    public MuPdfWrongPasswordEnteredException() {
        super(true, "");
    }

    public MuPdfWrongPasswordEnteredException(final String detailMessage, final Throwable throwable) {
        super(true, detailMessage);
    }

    public MuPdfWrongPasswordEnteredException(final String detailMessage) {
        super(true, detailMessage);
    }

    public MuPdfWrongPasswordEnteredException(final Throwable throwable) {
        super(true, throwable != null ? throwable.getMessage() : "");
    }
}
