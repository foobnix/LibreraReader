package org.ebookdroid.droids.mupdf.codec.exceptions;

public class MuPdfPasswordException extends RuntimeException {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7099241679450664187L;

    private final boolean wrongPasswordEntered;

    protected MuPdfPasswordException(final boolean wrongPasswordEntered, final String message) {
        super(message);
        this.wrongPasswordEntered = wrongPasswordEntered;
    }

    public boolean isWrongPasswordEntered() {
        return wrongPasswordEntered;
    }
}
