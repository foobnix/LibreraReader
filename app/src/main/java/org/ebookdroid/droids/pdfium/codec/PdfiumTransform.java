package org.ebookdroid.droids.pdfium.codec;

import android.graphics.Point;
import android.graphics.RectF;

import io.legere.pdfiumandroid.PdfPage;

/**
 * The page's PDF space -> Librera's page space transform.
 *
 * PDFium hands out text and link rectangles in PDF user space: the origin is the bottom left of
 * the page box, y grows upwards and the page's <code>/Rotate</code> is not applied. Librera (and
 * MuPDF, which it was written against) works in page space instead: the origin is the top left of
 * the page as it is shown, y grows downwards, the size is the page's size in points after
 * <code>/Rotate</code>, and the rendered bitmap covers exactly that rectangle.
 *
 * The mapping between the two is affine, so it is read out of PDFium itself - three corners of the
 * page box put through <code>FPDF_PageToDevice</code> - instead of being re-derived here from the
 * box and the rotation. That keeps it in step with how PDFium places the same page on a bitmap,
 * whatever the box and rotation are.
 */
class PdfiumTransform {

    /**
     * The device size the reference points are mapped to is the page size in points times this,
     * so the integer device coordinates PDFium returns still carry a fraction of a point.
     */
    private static final int PRECISION = 64;

    /** x' = a * x + c * y + e, y' = b * x + d * y + f */
    private final float a, b, c, d, e, f;

    /** Page size in points, after /Rotate: the page-space rectangle is (0, 0, width, height). */
    final float width, height;

    private PdfiumTransform(float a, float b, float c, float d, float e, float f, float width, float height) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
        this.width = width;
        this.height = height;
    }

    /**
     * Reads the transform off the page.
     *
     * @param page        an open PDFium page
     * @param widthPoint  the page width in points, after /Rotate
     * @param heightPoint the page height in points, after /Rotate
     */
    static PdfiumTransform of(final PdfPage page, final int widthPoint, final int heightPoint) {
        final float w = Math.max(1, widthPoint);
        final float h = Math.max(1, heightPoint);

        final RectF box = pageBox(page, widthPoint, heightPoint);
        // The box is in PDF space, where "top" is the larger y: take the corners by value so the
        // order PDFium fills the rectangle in cannot matter.
        final float x0 = Math.min(box.left, box.right);
        final float x1 = Math.max(box.left, box.right);
        final float y0 = Math.min(box.top, box.bottom);
        final float y1 = Math.max(box.top, box.bottom);
        final float bw = x1 - x0;
        final float bh = y1 - y0;

        if (bw <= 0 || bh <= 0) {
            return identity(w, h);
        }

        final int sx = (int) w * PRECISION;
        final int sy = (int) h * PRECISION;

        final Point origin = page.mapPageCoordsToDevice(0, 0, sx, sy, 0, x0, y0);
        final Point alongX = page.mapPageCoordsToDevice(0, 0, sx, sy, 0, x1, y0);
        final Point alongY = page.mapPageCoordsToDevice(0, 0, sx, sy, 0, x0, y1);

        if (origin == null || alongX == null || alongY == null) {
            return identity(w, h);
        }

        final float a = (alongX.x - origin.x) / (PRECISION * bw);
        final float b = (alongX.y - origin.y) / (PRECISION * bw);
        final float c = (alongY.x - origin.x) / (PRECISION * bh);
        final float d = (alongY.y - origin.y) / (PRECISION * bh);

        if (a == 0 && c == 0) {
            // A page PDFium would not map at all; the plain box transform is still better than
            // collapsing every rectangle onto one point.
            return identity(w, h);
        }

        return new PdfiumTransform(a, b, c, d, //
                origin.x / (float) PRECISION - a * x0 - c * y0, //
                origin.y / (float) PRECISION - b * x0 - d * y0, //
                w, h);
    }

    /** The box the page is shown through, in PDF space; the media box, else the page itself. */
    private static RectF pageBox(final PdfPage page, final int widthPoint, final int heightPoint) {
        RectF box = valid(page.getPageCropBox());
        if (box == null) {
            box = valid(page.getPageMediaBox());
        }
        if (box == null) {
            box = valid(page.getPageBoundingBox());
        }
        return box != null ? box : new RectF(0, 0, Math.max(1, widthPoint), Math.max(1, heightPoint));
    }

    private static RectF valid(final RectF box) {
        if (box == null) {
            return null;
        }
        final float w = Math.abs(box.right - box.left);
        final float h = Math.abs(box.bottom - box.top);
        // PDFium answers RectF(-1, -1, -1, -1) when a box is not present
        return w > 0 && h > 0 ? box : null;
    }

    /** The fallback: PDF space with y flipped about the page, as an unrotated page box gives. */
    private static PdfiumTransform identity(final float width, final float height) {
        return new PdfiumTransform(1, 0, 0, -1, 0, height, width, height);
    }

    /**
     * Maps a PDF-space rectangle to page space. All four corners are mapped and the result is
     * their bounding box, so a rotated page comes out right.
     */
    RectF toPageSpace(final RectF pdf) {
        final float x0 = a * pdf.left + c * pdf.top + e;
        final float y0 = b * pdf.left + d * pdf.top + f;
        final float x1 = a * pdf.right + c * pdf.top + e;
        final float y1 = b * pdf.right + d * pdf.top + f;
        final float x2 = a * pdf.right + c * pdf.bottom + e;
        final float y2 = b * pdf.right + d * pdf.bottom + f;
        final float x3 = a * pdf.left + c * pdf.bottom + e;
        final float y3 = b * pdf.left + d * pdf.bottom + f;

        return new RectF(//
                Math.min(Math.min(x0, x1), Math.min(x2, x3)), //
                Math.min(Math.min(y0, y1), Math.min(y2, y3)), //
                Math.max(Math.max(x0, x1), Math.max(x2, x3)), //
                Math.max(Math.max(y0, y1), Math.max(y2, y3)));
    }

    /** Maps a page-space point back to PDF space; the inverse of {@link #toPageSpace(RectF)}. */
    void toPdfSpace(final float x, final float y, final float[] out) {
        final float det = a * d - b * c;
        if (det == 0) {
            out[0] = x;
            out[1] = y;
            return;
        }
        final float dx = x - e;
        final float dy = y - f;
        out[0] = (d * dx - c * dy) / det;
        out[1] = (a * dy - b * dx) / det;
    }

    /** Scales a page-space rectangle to the 0..1 of the page, as Librera's pages are expressed. */
    void normalize(final RectF wd) {
        wd.left = wd.left / width;
        wd.top = wd.top / height;
        wd.right = wd.right / width;
        wd.bottom = wd.bottom / height;
    }
}
