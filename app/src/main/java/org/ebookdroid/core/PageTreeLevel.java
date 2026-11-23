package org.ebookdroid.core;

import org.ebookdroid.core.models.ZoomModel;

public class PageTreeLevel {

    /**
     * The zoom the page is first split at, into 4 tiles; every level after splits each tile into
     * 4 again at twice the zoom of the one before. As the tiles halve each side, a tile stays
     * 0.75-1.5 screen widths at any zoom: the whole page at 1x, the default, is still one bitmap.
     * The split used to come only every 3x of zoom, and the tiles grew half again each level: at
     * 13.5x and over a DjVu tile was 3638x4753 px, 69 MB, slow to decode and to send to the GPU,
     * and zooming out stalled while the GPU re-uploaded them every frame.
     */
    public static final float FIRST_SPLIT_ZOOM = 1.5f;
    public static final int ZOOM_THRESHOLD = 2;

    public static final PageTreeLevel ROOT;
    public static final PageTreeLevel[] LEVELS;
    public static final int NODES;

    static {
        // the root, then a level from FIRST_SPLIT_ZOOM on, and one more at every doubling up to the
        // largest zoom
        int count = 2 + (int) Math.floor(Math.log(ZoomModel.MAX_ZOOM / FIRST_SPLIT_ZOOM) / Math.log(ZOOM_THRESHOLD));
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
        //NODES = 1;
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
        this.zoom = parent.level == 0 ? FIRST_SPLIT_ZOOM : parent.zoom * ZOOM_THRESHOLD;
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
