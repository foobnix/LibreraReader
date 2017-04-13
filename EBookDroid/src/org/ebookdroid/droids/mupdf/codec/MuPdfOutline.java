package org.ebookdroid.droids.mupdf.codec;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.codec.OutlineLink;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.sys.TempHolder;

public class MuPdfOutline {

    private static final float[] temp = new float[4];

    private long docHandle;

    public List<OutlineLink> getOutline(final long dochandle) {
        final List<OutlineLink> ls = new ArrayList<OutlineLink>();
        docHandle = dochandle;
        try {
            TempHolder.lock.lock();
            final long outline = open(dochandle);
            ttOutline(ls, outline, 0);
            free(dochandle);

            ls.add(new OutlineLink("", "", -1));
        } finally {
            TempHolder.lock.unlock();
        }
        return ls;
    }

    private void ttOutline(final List<OutlineLink> ls, long outline, final int level) {
        while (outline != -1) {
            final String title = getTitle(outline);
            final String link = getLink(outline, docHandle);
            if (title != null) {
                final OutlineLink outlineLink = new OutlineLink(title, link, level);
                // if (outlineLink.targetPage != -1) {
                // int flags = fillLinkTargetPoint(outline, temp);
                // outlineLink.targetRect = new RectF();
                // outlineLink.targetRect.left = temp[0];
                // outlineLink.targetRect.top = temp[1];
                // MuPdfDocument.normalizeLinkTargetRect(docHandle,
                // outlineLink.targetPage, outlineLink.targetRect,
                // flags);
                // }
                if (outlineLink.getTitle().contains(Fb2Extractor.DIVIDER)) {
                    try {
                        String[] split = outlineLink.getTitle().split(Fb2Extractor.DIVIDER);
                        int level2 = Integer.parseInt(split[0]);
                        outlineLink.setLevel(level2);
                        outlineLink.setTitle(split[1]);
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }

                ls.add(outlineLink);
            }

            final long child = getChild(outline);
            ttOutline(ls, child, level + 1);

            outline = getNext(outline);
        }
    }

    private static native String getTitle(long outlinehandle);

    private static native String getLink(long outlinehandle, long dochandle);

    private static native int fillLinkTargetPoint(long outlinehandle, float[] point);

    private static native long getNext(long outlinehandle);

    private static native long getChild(long outlinehandle);

    private static native long open(long dochandle);

    private static native void free(long dochandle);
}
