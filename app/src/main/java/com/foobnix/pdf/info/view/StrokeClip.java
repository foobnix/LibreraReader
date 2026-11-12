package com.foobnix.pdf.info.view;

import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

/**
 * A stroke drawn on screen cut to one page: the pieces of it that lie on the page, each a line
 * of its own, ending where the stroke leaves the page. A figure drawn over two pages is then the
 * part of it on each page, rather than the points that happen to fall on one - a rectangle over
 * the gap between pages stays a rectangle cut in two, not two lines.
 */
public final class StrokeClip {

    private StrokeClip() {
    }

    /** The pieces of the stroke inside the rect, as new points; empty when none of it is. */
    public static List<List<PointF>> clip(final List<PointF> stroke, final RectF rect) {
        final List<List<PointF>> pieces = new ArrayList<List<PointF>>();
        if (stroke.isEmpty()) {
            return pieces;
        }
        if (stroke.size() == 1) {
            final PointF p = stroke.get(0);
            if (rect.contains(p.x, p.y)) {
                pieces.add(line(new PointF(p.x, p.y)));
            }
            return pieces;
        }
        List<PointF> piece = null;
        final float[] t = new float[2];
        for (int i = 1; i < stroke.size(); i++) {
            final PointF a = stroke.get(i - 1);
            final PointF b = stroke.get(i);
            if (!clipSegment(a, b, rect, t)) {
                piece = null;
                continue;
            }
            final PointF from = at(a, b, t[0]);
            final PointF to = at(a, b, t[1]);
            if (piece == null || t[0] > 0) {
                piece = line(from);
                pieces.add(piece);
            }
            piece.add(to);
            if (t[1] < 1) {
                // It leaves the page here: what comes back on is a piece of its own
                piece = null;
            }
        }
        // A closed figure cut by the page edge: the piece it ends with goes on into the one it
        // starts with, so the corner it was started from is one line, not two
        final PointF first = stroke.get(0);
        final PointF last = stroke.get(stroke.size() - 1);
        if (pieces.size() > 1 && first.equals(last.x, last.y)) {
            final List<PointF> head = pieces.get(0);
            final List<PointF> tail = pieces.get(pieces.size() - 1);
            if (head.get(0).equals(first.x, first.y) && tail.get(tail.size() - 1).equals(last.x, last.y)) {
                tail.remove(tail.size() - 1);
                tail.addAll(head);
                pieces.remove(0);
            }
        }
        return pieces;
    }

    private static List<PointF> line(final PointF start) {
        final List<PointF> list = new ArrayList<PointF>();
        list.add(start);
        return list;
    }

    private static PointF at(final PointF a, final PointF b, final float t) {
        if (t <= 0) {
            return new PointF(a.x, a.y);
        }
        if (t >= 1) {
            return new PointF(b.x, b.y);
        }
        return new PointF(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    /**
     * Liang-Barsky: how much of a..b is inside the rect, as t[0]..t[1] along it.
     *
     * @return false when none of it is
     */
    private static boolean clipSegment(final PointF a, final PointF b, final RectF r, final float[] t) {
        final float dx = b.x - a.x;
        final float dy = b.y - a.y;
        final float[] p = {-dx, dx, -dy, dy};
        final float[] q = {a.x - r.left, r.right - a.x, a.y - r.top, r.bottom - a.y};
        float t0 = 0;
        float t1 = 1;
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) {
                    return false;
                }
            } else {
                final float s = q[i] / p[i];
                if (p[i] < 0) {
                    t0 = Math.max(t0, s);
                } else {
                    t1 = Math.min(t1, s);
                }
            }
        }
        // Only touching the edge at a point is not on the page either
        if (t0 > t1 || t0 == t1 && (dx != 0 || dy != 0)) {
            return false;
        }
        t[0] = t0;
        t[1] = t1;
        return true;
    }
}
