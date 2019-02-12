package org.ebookdroid.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.common.bitmaps.Bitmaps;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.ebookdroid.ui.viewer.IActivityController;
import org.emdev.utils.MathUtils;
import org.emdev.utils.MatrixUtils;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.wrapper.AppState;

import android.graphics.Matrix;
import android.graphics.RectF;

public class Page {

    public final PageIndex index;
    public final PageType type;
    public final CodecPageInfo cpi;

    final IActivityController base;
    final PageTree nodes;

    RectF bounds;
    int aspectRatio;
    boolean recycled;
    float storedZoom;
    RectF zoomedBounds;

    List<PageLink> links;
    TextWord[][] texts;
    public List<TextWord> selectedText = new ArrayList<TextWord>();
    public List<Annotation> annotations;

    public RectF selectionAnnotion;

    public Page(final IActivityController base, final PageIndex index, final PageType pt, final CodecPageInfo cpi) {
        this.base = base;
        this.index = index;
        this.cpi = cpi;
        this.type = pt != null ? pt : PageType.FULL_PAGE;
        this.bounds = new RectF(0, 0, cpi.width / type.getWidthScale(), cpi.height);

        setAspectRatio(cpi);

        nodes = new PageTree(this);
    }

    public List<TextWord> findText(String text) {
        return findText(text, texts);
    }

    public static List<TextWord> findText(String text, TextWord[][] texts) {
        List<TextWord> result = new ArrayList<TextWord>();
        if (texts == null) {
            return result;
        }
        text = text.toLowerCase(Locale.US);
        int index = 0;
        List<TextWord> find = new ArrayList<TextWord>();

        boolean nextWorld = false;
        String firstPart = "";
        TextWord firstWord = null;

        for (final TextWord[] lines : texts) {
            find.clear();
            index = 0;
            for (final TextWord word : lines) {
                if (AppState.get().selectingByLetters) {
                    String it = String.valueOf(text.charAt(index));
                    if (word.w.toLowerCase(Locale.US).equals(it)) {
                        index++;
                        find.add(word);
                    } else {
                        index = 0;
                        find.clear();
                    }

                    if (index == text.length()) {
                        index = 0;
                        for (TextWord t : find) {
                            result.add(t);
                        }
                    }
                } else if (word.w.toLowerCase(Locale.US).contains(text)) {
                    result.add(word);
                } else if (word.w.length() >= 3 && word.w.endsWith("-")) {
                    nextWorld = true;
                    firstWord = word;
                    firstPart = word.w.replace("-", "");
                } else if (nextWorld && (firstPart + word.w.toLowerCase(Locale.US)).contains(text)) {
                    result.add(firstWord);
                    result.add(word);
                    nextWorld = false;
                    firstWord = null;
                    firstPart = "";
                } else if (nextWorld && TxtUtils.isNotEmpty(word.w)) {
                    nextWorld = false;
                    firstWord = null;
                }
            }
        }
        return result;
    }

    public RectF getBounds() {
        return bounds;
    }

    public void recycle(final List<Bitmaps> bitmapsToRecycle) {
        texts = null;
        recycled = true;
        nodes.recycleAll(bitmapsToRecycle, true);
    }

    public float getAspectRatio() {
        return aspectRatio / 128.0f;
    }

    private boolean setAspectRatio(final float aspectRatio) {
        final int newAspectRatio = (int) Math.floor(aspectRatio * 128);
        if (this.aspectRatio != newAspectRatio) {
            this.aspectRatio = newAspectRatio;
            return true;
        }
        return false;
    }

    public boolean setAspectRatio(final CodecPageInfo page) {
        if (page != null) {
            return this.setAspectRatio(page.width / type.getWidthScale(), page.height);
        }
        return false;
    }

    public boolean setAspectRatio(final float width, final float height) {
        return setAspectRatio(width / height);
    }

    public void setBounds(final RectF pageBounds) {
        storedZoom = 0.0f;
        zoomedBounds = null;
        bounds = pageBounds;
    }

    public void setBounds(final float l, final float t, final float r, final float b) {
        if (bounds == null) {
            bounds = new RectF(l, t, r, b);
        } else {
            bounds.set(l, t, r, b);
        }
    }

    public RectF getBounds(final float zoom) {
        // if (zoom != storedZoom) {
        // storedZoom = zoom;
        // zoomedBounds = MathUtils.zoom(bounds, zoom);
        // }
        // return zoomedBounds;
        return MathUtils.zoom(bounds, zoom);
    }

    public float getTargetRectScale() {
        return type.getWidthScale();
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("Page");
        buf.append("[");

        buf.append("index").append("=").append(index);
        buf.append(", ");
        buf.append("bounds").append("=").append(bounds);
        buf.append(", ");
        buf.append("aspectRatio").append("=").append(aspectRatio);
        buf.append(", ");
        buf.append("type").append("=").append(type.name());
        buf.append("]");
        return buf.toString();
    }

    public static RectF getTargetRect(final PageType pageType, final RectF pageBounds, final RectF normalizedRect) {
        final Matrix tmpMatrix = MatrixUtils.get();

        tmpMatrix.postScale(pageBounds.width() * pageType.getWidthScale(), pageBounds.height());
        tmpMatrix.postTranslate(pageBounds.left - pageBounds.width() * pageType.getLeftPos(), pageBounds.top);

        final RectF targetRectF = new RectF();
        tmpMatrix.mapRect(targetRectF, normalizedRect);

        MathUtils.floor(targetRectF);

        return targetRectF;
    }

    public RectF getLinkSourceRect(final RectF pageBounds, final PageLink link) {
        if (link == null || link.sourceRect == null) {
            return null;
        }
        return getPageRegion(pageBounds, new RectF(link.sourceRect));
    }

    public RectF getPageRegion(final RectF pageBounds, final RectF sourceRect) {
        final BookSettings bs = SettingsManager.getBookSettings();
        final RectF cb = nodes.root.croppedBounds;
        if (bs != null && bs.cropPages && cb != null) {
            final Matrix m = MatrixUtils.get();
            final RectF psb = nodes.root.pageSliceBounds;
            m.postTranslate(psb.left - cb.left, psb.top - cb.top);
            m.postScale(psb.width() / cb.width(), psb.height() / cb.height());
            m.mapRect(sourceRect);
        }

        if (type == PageType.LEFT_PAGE && sourceRect.left >= 0.5f) {
            return null;
        }

        if (type == PageType.RIGHT_PAGE && sourceRect.right < 0.5f) {
            return null;
        }

        return getTargetRect(type, pageBounds, sourceRect);
    }
}
