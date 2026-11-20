package com.foobnix.sys;

import android.graphics.Bitmap;

/** The cover process, see CoverRenderService and CoverProcess. */
interface ICoverRenderer {

    /** The process the covers are drawn in, which is killed when one takes too long. */
    int pid();

    /**
     * The page a cover url (PageUrl.toString) asks for, drawn by the given PDF engine, or null
     * when it could not be drawn.
     */
    Bitmap render(String pageUrl, int pdfEngine);
}
