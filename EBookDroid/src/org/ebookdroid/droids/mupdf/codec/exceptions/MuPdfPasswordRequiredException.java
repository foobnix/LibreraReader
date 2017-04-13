package org.ebookdroid.droids.mupdf.codec.exceptions;

public class MuPdfPasswordRequiredException extends MuPdfPasswordException {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 3864032980487465345L;

    public MuPdfPasswordRequiredException() {
        super(false, "");
    }

    public MuPdfPasswordRequiredException(final String detailMessage, final Throwable throwable) {
        super(false, detailMessage);
    }

    public MuPdfPasswordRequiredException(final String detailMessage) {
        super(false, detailMessage);
    }

    public MuPdfPasswordRequiredException(final Throwable throwable) {
        super(true, throwable != null ? throwable.getMessage() : "");
    }
}
