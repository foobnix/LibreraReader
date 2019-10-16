package org.ebookdroid.core;

import org.ebookdroid.core.models.ZoomModel;

public class PageTreeLevel {

    public static final int ZOOM_THRESHOLD = 2;

    public static final PageTreeLevel ROOT;
    public static final PageTreeLevel[] LEVELS;
    public static final int NODES;

    static {
        int count = 1 + (int) Math.ceil(Math.log(ZoomModel.MAX_ZOOM) / Math.log(2));
        ROOT = new PageTreeLevel(count);
        LEVELS = new PageTreeLevel[count];
        LEVELS[0] = ROOT;
        int end = ROOT.end;
        int index = 1;
        for (PageTreeLevel l = ROOT.next; l != null; l = l.next) {
            LEVELS[index++] = l;
            end = l.end;
        }
        NODES = end;
    }

    public final int level;
    public final float zoom;
    public final int start;
    public final int end;

    public final PageTreeLevel prev;
    public final PageTreeLevel next;

    private PageTreeLevel(int count) {
        this.level = 0;
        this.zoom = ZoomModel.MIN_ZOOM;
        this.start = 0;
        this.end = 1;

        this.prev = null;
        this.next = new PageTreeLevel(this, count - 1);
    }

    private PageTreeLevel(final PageTreeLevel parent, int count) {
        this.level = parent.level + 1;
        this.zoom = parent.zoom * ZOOM_THRESHOLD;
        this.start = parent.end;
        this.end = this.start + (int) Math.pow(PageTree.splitMasks.length, this.level);

        this.prev = parent;
        this.next = count > 1 ? new PageTreeLevel(this, count - 1) : null;
    }

    public static PageTreeLevel getLevel(float zoom) {
        for (int i = 1; i < LEVELS.length; i++) {
            if (zoom < LEVELS[i].zoom) {
                return LEVELS[i - 1];
            } else if (zoom == LEVELS[i].zoom) {
                return LEVELS[i];
            }
        }
        return LEVELS[LEVELS.length - 1];
    }
}
