package com.foobnix.android.utils;

public class DebugException extends RuntimeException {

    private static final long serialVersionUID = 112L;

    public DebugException(Throwable e) {
        super(e);
    }

}
