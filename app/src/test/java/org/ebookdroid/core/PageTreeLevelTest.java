package org.ebookdroid.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.ebookdroid.core.models.ZoomModel;
import org.junit.Test;

public class PageTreeLevelTest {

    /** A tile's width in screen widths: the page is a screen wide at 1x, a level halves each side. */
    private static float tileWidth(float zoom) {
        return zoom / (1 << PageTreeLevel.getLevel(zoom).level);
    }

    @Test public void tilesStayAboutAScreenWideAtEveryZoom() {
        for (float zoom = ZoomModel.MIN_ZOOM; zoom <= ZoomModel.MAX_ZOOM; zoom += 0.01f) {
            final float width = tileWidth(zoom);
            assertTrue("zoom " + zoom + ": tile " + width + " screens wide", width <= 1.5f + 1e-3f);
            if (zoom >= 1) {
                assertTrue("zoom " + zoom + ": tile " + width + " screens wide", width >= 0.75f - 1e-3f);
            }
        }
        assertTrue(tileWidth(ZoomModel.MAX_ZOOM) <= 1.5f);
    }

    @Test public void theDefaultZoomIsOneBitmapAPage() {
        assertEquals(0, PageTreeLevel.getLevel(1f).level);
        assertEquals(1, PageTreeLevel.getLevel(1.5f).level);
    }

    @Test public void levelsReachTheLargestZoom() {
        assertEquals(6, PageTreeLevel.LEVELS.length);
        assertEquals(1 + 4 + 16 + 64 + 256 + 1024, PageTreeLevel.NODES);
        assertEquals(PageTreeLevel.LEVELS.length - 1, PageTreeLevel.getLevel(ZoomModel.MAX_ZOOM).level);
    }
}
