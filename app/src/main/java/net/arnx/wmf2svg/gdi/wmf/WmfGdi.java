package net.arnx.wmf2svg.gdi.wmf;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.GdiBrush;
import net.arnx.wmf2svg.gdi.GdiFont;
import net.arnx.wmf2svg.gdi.GdiObject;
import net.arnx.wmf2svg.gdi.GdiPalette;
import net.arnx.wmf2svg.gdi.GdiPatternBrush;
import net.arnx.wmf2svg.gdi.GdiPen;
import net.arnx.wmf2svg.gdi.GdiRegion;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WmfGdi implements Gdi, WmfConstants {
	private byte[] placeableHeader;
	private byte[] header;

	private List<GdiObject> objects = new ArrayList<GdiObject>();
	private List<byte[]> records = new ArrayList<byte[]>();

	public WmfGdi() {
	}

	public void write(OutputStream out) throws IOException {
		footer();
		if (placeableHeader != null) out.write(placeableHeader);
		if (header != null) out.write(header);

		Iterator i = records.iterator();
		while (i.hasNext()) {
			out.write((byte[])i.next());
		}
		out.flush();
	}

	public void placeableHeader(int vsx, int vsy, int vex, int vey, int dpi) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, 0x9AC6CDD7);
		pos = setInt16(record, pos, 0x0000);
		pos = setInt16(record, pos, vsx);
		pos = setInt16(record, pos, vsy);
		pos = setInt16(record, pos, vex);
		pos = setInt16(record, pos, vey);
		pos = setUint16(record, pos, dpi);
		pos = setUint32(record, pos, 0x00000000);

		int checksum = 0;
		for (int i = 0; i < record.length-2; i+=2) {
			checksum ^= (0xFF & record[i]) | ((0xFF & record[i+1]) << 8);
		}

		pos = setUint16(record, pos, checksum);
		placeableHeader = record;
	}

	public void header() {
		byte[] record = new byte[18];
		int pos = 0;
		pos = setUint16(record, pos, 0x0001);
		pos = setUint16(record, pos, 0x0009);
		pos = setUint16(record, pos, 0x0300);
		pos = setUint32(record, pos, 0x0000); // dummy size
		pos = setUint16(record, pos, 0x0000); // dummy noObjects
		pos = setUint32(record, pos, 0x0000); // dummy maxRecords
		pos = setUint16(record, pos, 0x0000);
		header = record;
	}

	public void animatePalette(GdiPalette palette, int startIndex, int[] entries) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_ANIMATE_PALETTE);
		pos = setUint16(record, pos, entries.length);
		pos = setUint16(record, pos, startIndex);
		pos = setUint16(record, pos, ((WmfPalette)palette).getID());
		for (int i = 0; i < entries.length; i++) {
			pos = setInt32(record, pos, entries[i]);
		}
		records.add(record);
	}

	public void arc(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_ARC);
		pos = setInt16(record, pos, eya);
		pos = setInt16(record, pos, exa);
		pos = setInt16(record, pos, sya);
		pos = setInt16(record, pos, sxa);
		pos = setInt16(record, pos, eyr);
		pos = setInt16(record, pos, exr);
		pos = setInt16(record, pos, syr);
		pos = setInt16(record, pos, sxr);
		records.add(record);
	}

	public void bitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		byte[] record = new byte[22 + (image.length + image.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_BIT_BLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void chord(int sxr, int syr, int exr, int eyr, int sxa, int sya, int exa, int eya) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_CHORD);
		pos = setInt16(record, pos, eya);
		pos = setInt16(record, pos, exa);
		pos = setInt16(record, pos, sya);
		pos = setInt16(record, pos, sxa);
		pos = setInt16(record, pos, eyr);
		pos = setInt16(record, pos, exr);
		pos = setInt16(record, pos, syr);
		pos = setInt16(record, pos, sxr);
		records.add(record);
	}

	public GdiBrush createBrushIndirect(int style, int color, int hatch) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_CREATE_BRUSH_INDIRECT);
		pos = setUint16(record, pos, style);
		pos = setInt32(record, pos, color);
		pos = setUint16(record, pos, hatch);
		records.add(record);

		WmfBrush brush = new WmfBrush(objects.size(), style, color, hatch);
		objects.add(brush);
		return brush;
	}

	public GdiFont createFontIndirect(int height, int width, int escapement,
			int orientation, int weight, boolean italic, boolean underline,
			boolean strikeout, int charset, int outPrecision,
			int clipPrecision, int quality, int pitchAndFamily, byte[] faceName) {

		byte[] record = new byte[24 + (faceName.length + faceName.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_CREATE_FONT_INDIRECT);
		pos = setInt16(record, pos, height);
		pos = setInt16(record, pos, width);
		pos = setInt16(record, pos, escapement);
		pos = setInt16(record, pos, orientation);
		pos = setInt16(record, pos, weight);
		pos = setByte(record, pos, (italic) ? 0x01 : 0x00);
		pos = setByte(record, pos, (underline) ? 0x01 : 0x00);
		pos = setByte(record, pos, (strikeout) ? 0x01 : 0x00);
		pos = setByte(record, pos, charset);
		pos = setByte(record, pos, outPrecision);
		pos = setByte(record, pos, clipPrecision);
		pos = setByte(record, pos, quality);
		pos = setByte(record, pos, pitchAndFamily);
		pos = setBytes(record, pos, faceName);
		if (faceName.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);

		WmfFont font = new WmfFont(objects.size(), height, width, escapement,
				orientation, weight, italic, underline, strikeout, charset, outPrecision,
				clipPrecision, quality, pitchAndFamily, faceName);
		objects.add(font);
		return font;
	}

	public GdiPalette createPalette(int version, int[] entries) {
		byte[] record = new byte[10 + entries.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_CREATE_PALETTE);
		pos = setUint16(record, pos, version);
		pos = setUint16(record, pos, entries.length);
		for (int i = 0; i < entries.length; i++) {
			pos = setInt32(record, pos, entries[i]);
		}
		records.add(record);

		GdiPalette palette = new WmfPalette(objects.size(), version, entries);
		objects.add(palette);
		return palette;
	}

	public GdiPatternBrush createPatternBrush(byte[] image) {
		byte[] record = new byte[6 + (image.length + image.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_CREATE_PATTERN_BRUSH);
		pos = setBytes(record, pos, image);
		if (image.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);

		GdiPatternBrush brush = new WmfPatternBrush(objects.size(), image);
		objects.add(brush);
		return brush;
	}

	public GdiPen createPenIndirect(int style, int width, int color) {
		byte[] record = new byte[16];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_CREATE_PEN_INDIRECT);
		pos = setUint16(record, pos, style);
		pos = setInt16(record, pos, width);
		pos = setInt16(record, pos, 0);
		pos = setInt32(record, pos, color);
		records.add(record);

		WmfPen pen = new WmfPen(objects.size(), style, width, color);
		objects.add(pen);
		return pen;
	}

	public GdiRegion createRectRgn(int left, int top, int right, int bottom) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_CREATE_RECT_RGN);
		pos = setInt16(record, pos, bottom);
		pos = setInt16(record, pos, right);
		pos = setInt16(record, pos, top);
		pos = setInt16(record, pos, left);
		records.add(record);

		WmfRectRegion rgn = new WmfRectRegion(objects.size(), left, top, right, bottom);
		objects.add(rgn);
		return rgn;
	}

	public void deleteObject(GdiObject obj) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_DELETE_OBJECT);
		pos = setUint16(record, pos, ((WmfObject)obj).getID());
		records.add(record);

		objects.set(((WmfObject)obj).getID(), null);
	}

	public void dibBitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop) {
		byte[] record = new byte[22 + (image.length + image.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_DIB_BIT_BLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);
	}

	public GdiPatternBrush dibCreatePatternBrush(byte[] image, int usage) {
		byte[] record = new byte[10 + (image.length + image.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_DIB_CREATE_PATTERN_BRUSH);
		pos = setInt32(record, pos, usage);
		pos = setBytes(record, pos, image);
		if (image.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);

		// TODO usage
		GdiPatternBrush brush = new WmfPatternBrush(objects.size(), image);
		objects.add(brush);
		return brush;
	}

	public void dibStretchBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop) {
		byte[] record = new byte[26 + (image.length + image.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_DIB_STRETCH_BLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, sh);
		pos = setInt16(record, pos, sw);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void ellipse(int sx, int sy, int ex, int ey) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_ELLIPSE);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		records.add(record);
	}

	public void escape(byte[] data) {
		byte[] record = new byte[10 + (data.length + data.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_ESCAPE);
		pos = setBytes(record, pos, data);
		if (data.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);
	}

	public int excludeClipRect(int left, int top, int right, int bottom) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_EXCLUDE_CLIP_RECT);
		pos = setInt16(record, pos, bottom);
		pos = setInt16(record, pos, right);
		pos = setInt16(record, pos, top);
		pos = setInt16(record, pos, left);
		records.add(record);

		// TODO
		return GdiRegion.COMPLEXREGION;
	}

	public void extFloodFill(int x, int y, int color, int type) {
		byte[] record = new byte[16];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_EXT_FLOOD_FILL);
		pos = setUint16(record, pos, type);
		pos = setInt32(record, pos, color);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void extTextOut(int x, int y, int options, int[] rect, byte[] text, int[] lpdx) {
		if (rect != null && rect.length != 4) {
			throw new IllegalArgumentException("rect must be 4 length.");
		}
		byte[] record = new byte[14 + ((rect != null) ? 8 : 0) + (text.length + text.length%2) + (lpdx.length * 2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_EXT_TEXT_OUT);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		pos = setInt16(record, pos, text.length);
		pos = setInt16(record, pos, options);
		if (rect != null) {
			pos = setInt16(record, pos, rect[0]);
			pos = setInt16(record, pos, rect[1]);
			pos = setInt16(record, pos, rect[2]);
			pos = setInt16(record, pos, rect[3]);
		}
		pos = setBytes(record, pos, text);
		if (text.length%2 == 1) pos = setByte(record, pos, 0);
		for (int i = 0; i < lpdx.length; i++) {
			pos = setInt16(record, pos, lpdx[i]);
		}
		records.add(record);
	}

	public void fillRgn(GdiRegion rgn, GdiBrush brush) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_FLOOD_FILL);
		pos = setUint16(record, pos, ((WmfBrush)brush).getID());
		pos = setUint16(record, pos, ((WmfRegion)rgn).getID());
		records.add(record);
	}

	public void floodFill(int x, int y, int color) {
		byte[] record = new byte[16];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_FLOOD_FILL);
		pos = setInt32(record, pos, color);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void frameRgn(GdiRegion rgn, GdiBrush brush, int w, int h) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_FRAME_RGN);
		pos = setInt16(record, pos, h);
		pos = setInt16(record, pos, w);
		pos = setUint16(record, pos, ((WmfBrush)brush).getID());
		pos = setUint16(record, pos, ((WmfRegion)rgn).getID());
		records.add(record);
	}

	public void intersectClipRect(int left, int top, int right, int bottom) {
		byte[] record = new byte[16];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_INTERSECT_CLIP_RECT);
		pos = setInt16(record, pos, bottom);
		pos = setInt16(record, pos, right);
		pos = setInt16(record, pos, top);
		pos = setInt16(record, pos, left);
		records.add(record);
	}

	public void invertRgn(GdiRegion rgn) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_INVERT_RGN);
		pos = setUint16(record, pos, ((WmfRegion)rgn).getID());
		records.add(record);
	}

	public void lineTo(int ex, int ey) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_LINE_TO);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		records.add(record);
	}

	public void moveToEx(int x, int y, Point old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_MOVE_TO_EX);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		//TODO old
		records.add(record);
	}

	public void offsetClipRgn(int x, int y) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_OFFSET_CLIP_RGN);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void offsetViewportOrgEx(int x, int y, Point point) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_OFFSET_VIEWPORT_ORG_EX);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		// TODO
		records.add(record);
	}

	public void offsetWindowOrgEx(int x, int y, Point point) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_OFFSET_WINDOW_ORG_EX);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		// TODO
		records.add(record);
	}

	public void paintRgn(GdiRegion rgn) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_PAINT_RGN);
		pos = setUint16(record, pos, ((WmfRegion)rgn).getID());
		records.add(record);
	}

	public void patBlt(int x, int y, int width, int height, long rop) {
		byte[] record = new byte[18];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_PAT_BLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, height);
		pos = setInt16(record, pos, width);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void pie(int sx, int sy, int ex, int ey, int sxr, int syr, int exr, int eyr) {
		byte[] record = new byte[22];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_PIE);
		pos = setInt16(record, pos, eyr);
		pos = setInt16(record, pos, exr);
		pos = setInt16(record, pos, syr);
		pos = setInt16(record, pos, sxr);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		records.add(record);
	}

	public void polygon(Point[] points) {
		byte[] record = new byte[8 + points.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_POLYGON);
		pos = setInt16(record, pos, points.length);
		for (int i = 0; i < points.length; i++) {
			pos = setInt16(record, pos, points[i].x);
			pos = setInt16(record, pos, points[i].y);
		}
		records.add(record);
	}

	public void polyline(Point[] points) {
		byte[] record = new byte[8 + points.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_POLYLINE);
		pos = setInt16(record, pos, points.length);
		for (int i = 0; i < points.length; i++) {
			pos = setInt16(record, pos, points[i].x);
			pos = setInt16(record, pos, points[i].y);
		}
		records.add(record);
	}

	public void polyPolygon(Point[][] points) {
		int length = 8;
		for (int i = 0; i < points.length; i++) {
			length += 2 + points[i].length * 4;
		}
		byte[] record = new byte[length];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_POLYLINE);
		pos = setInt16(record, pos, points.length);
		for (int i = 0; i < points.length; i++) {
			pos = setInt16(record, pos, points[i].length);
		}
		for (int i = 0; i < points.length; i++) {
			for (int j = 0; j < points[i].length; j++) {
				pos = setInt16(record, pos, points[i][j].x);
				pos = setInt16(record, pos, points[i][j].y);
			}
		}
		records.add(record);
	}

	public void realizePalette() {
		byte[] record = new byte[6];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_REALIZE_PALETTE);
		records.add(record);
	}

	public void restoreDC(int savedDC) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_RESTORE_DC);
		pos = setInt16(record, pos, savedDC);
		records.add(record);
	}

	public void rectangle(int sx, int sy, int ex, int ey) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_RECTANGLE);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		records.add(record);
	}

	public void resizePalette(GdiPalette palette) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_REALIZE_PALETTE);
		pos = setUint16(record, pos, ((WmfPalette)palette).getID());
		records.add(record);
	}

	public void roundRect(int sx, int sy, int ex, int ey, int rw, int rh) {
		byte[] record = new byte[18];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_ROUND_RECT);
		pos = setInt16(record, pos, rh);
		pos = setInt16(record, pos, rw);
		pos = setInt16(record, pos, ey);
		pos = setInt16(record, pos, ex);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		records.add(record);
	}

	public void seveDC() {
		byte[] record = new byte[6];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SAVE_DC);
		records.add(record);
	}

	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SCALE_VIEWPORT_EXT_EX);
		pos = setInt16(record, pos, yd);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, xd);
		pos = setInt16(record, pos, x);
		// TODO
		records.add(record);
	}

	public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SCALE_WINDOW_EXT_EX);
		pos = setInt16(record, pos, yd);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, xd);
		pos = setInt16(record, pos, x);
		// TODO
		records.add(record);
	}

	public void selectClipRgn(GdiRegion rgn) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SELECT_CLIP_RGN);
		pos = setUint16(record, pos, ((WmfRegion)rgn).getID());
		records.add(record);
	}

	public void selectObject(GdiObject obj) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SELECT_OBJECT);
		pos = setUint16(record, pos, ((WmfObject)obj).getID());
		records.add(record);
	}

	public void selectPalette(GdiPalette palette, boolean mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SELECT_PALETTE);
		pos = setInt16(record, pos, mode ? 1 : 0);
		pos = setUint16(record, pos, ((WmfPalette)palette).getID());
		records.add(record);
	}

	public void setBkColor(int color) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_BK_COLOR);
		pos = setInt32(record, pos, color);
		records.add(record);
	}

	public void setBkMode(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_BK_MODE);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setDIBitsToDevice(int dx, int dy, int dw, int dh, int sx,
			int sy, int startscan, int scanlines, byte[] image, int colorUse) {
		byte[] record = new byte[24 + (image.length + image.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_DIBITS_TO_DEVICE);
		pos = setUint16(record, pos, colorUse);
		pos = setUint16(record, pos, scanlines);
		pos = setUint16(record, pos, startscan);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void setLayout(long layout) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_LAYOUT);
		pos = setUint32(record, pos, layout);
		records.add(record);
	}

	public void setMapMode(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_MAP_MODE);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setMapperFlags(long flags) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_MAPPER_FLAGS);
		pos = setUint32(record, pos, flags);
		records.add(record);
	}

	public void setPaletteEntries(GdiPalette palette, int startIndex, int[] entries) {
		byte[] record = new byte[6 + entries.length * 4];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_PALETTE_ENTRIES);
		pos = setUint16(record, pos, ((WmfPalette)palette).getID());
		pos = setUint16(record, pos, entries.length);
		pos = setUint16(record, pos, startIndex);
		for (int i = 0; i < entries.length; i++) {
			pos = setInt32(record, pos, entries[i]);
		}
		records.add(record);
	}

	public void setPixel(int x, int y, int color) {
		byte[] record = new byte[14];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_PIXEL);
		pos = setInt32(record, pos, color);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void setPolyFillMode(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_POLY_FILL_MODE);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setRelAbs(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_REL_ABS);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setROP2(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_ROP2);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setStretchBltMode(int mode) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_STRETCH_BLT_MODE);
		pos = setInt16(record, pos, mode);
		records.add(record);
	}

	public void setTextAlign(int align) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_TEXT_ALIGN);
		pos = setInt16(record, pos, align);
		records.add(record);
	}

	public void setTextCharacterExtra(int extra) {
		byte[] record = new byte[8];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_TEXT_CHARACTER_EXTRA);
		pos = setInt16(record, pos, extra);
		records.add(record);
	}

	public void setTextColor(int color) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_TEXT_COLOR);
		pos = setInt32(record, pos, color);
		records.add(record);
	}

	public void setTextJustification(int breakExtra, int breakCount) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_TEXT_COLOR);
		pos = setInt16(record, pos, breakCount);
		pos = setInt16(record, pos, breakExtra);
		records.add(record);
	}

	public void setViewportExtEx(int x, int y, Size old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_VIEWPORT_EXT_EX);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void setViewportOrgEx(int x, int y, Point old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_VIEWPORT_ORG_EX);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void setWindowExtEx(int width, int height, Size old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_WINDOW_EXT_EX);
		pos = setInt16(record, pos, height);
		pos = setInt16(record, pos, width);
		records.add(record);
	}

	public void setWindowOrgEx(int x, int y, Point old) {
		byte[] record = new byte[10];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_SET_WINDOW_ORG_EX);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void stretchBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop) {
		byte[] record = new byte[26 + (image.length + image.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_STRETCH_BLT);
		pos = setUint32(record, pos, rop);
		pos = setInt16(record, pos, sh);
		pos = setInt16(record, pos, sw);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void stretchDIBits(int dx, int dy, int dw, int dh, int sx, int sy,
			int sw, int sh, byte[] image, int usage, long rop) {
		byte[] record = new byte[26 + (image.length + image.length%2)];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_STRETCH_DIBITS);
		pos = setUint32(record, pos, rop);
		pos = setUint16(record, pos, usage);
		pos = setInt16(record, pos, sh);
		pos = setInt16(record, pos, sw);
		pos = setInt16(record, pos, sy);
		pos = setInt16(record, pos, sx);
		pos = setInt16(record, pos, dw);
		pos = setInt16(record, pos, dh);
		pos = setInt16(record, pos, dy);
		pos = setInt16(record, pos, dx);
		pos = setBytes(record, pos, image);
		if (image.length%2 == 1) pos = setByte(record, pos, 0);
		records.add(record);
	}

	public void textOut(int x, int y, byte[] text) {
		byte[] record = new byte[10 + text.length + text.length%2];
		int pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, RECORD_TEXT_OUT);
		pos = setInt16(record, pos, text.length);
		pos = setBytes(record, pos, text);
		if (text.length%2 == 1) pos = setByte(record, pos, 0);
		pos = setInt16(record, pos, y);
		pos = setInt16(record, pos, x);
		records.add(record);
	}

	public void footer() {
		int pos = 0;
		if (header != null) {
			long size = header.length;
			long maxRecordSize = 0;
			Iterator i = records.iterator();
			while (i.hasNext()) {
				byte[] record = (byte[])i.next();
				size += record.length;
				if (record.length > maxRecordSize) maxRecordSize = record.length;
			}

			pos = setUint32(header, 6, size/2);
			pos = setUint16(header, pos, objects.size());
			pos = setUint32(header, pos, maxRecordSize / 2);
		}

		byte[] record = new byte[6];
		pos = 0;
		pos = setUint32(record, pos, record.length/2);
		pos = setUint16(record, pos, 0x0000);
		records.add(record);
	}

	private int setByte(byte[] out, int pos, int value) {
		out[pos] = (byte)(0xFF & value);
		return pos + 1;
	}

	private int setBytes(byte[] out, int pos, byte[] data) {
		System.arraycopy(data, 0, out, pos, data.length);
		return pos + data.length;
	}

	private int setInt16(byte[] out, int pos, int value) {
		out[pos] = (byte)(0xFF & value);
		out[pos+1] = (byte)(0xFF & (value >> 8));
		return pos + 2;
	}

	private int setInt32(byte[] out, int pos, int value) {
		out[pos] = (byte)(0xFF & value);
		out[pos+1] = (byte)(0xFF & (value >> 8));
		out[pos+2] = (byte)(0xFF & (value >> 16));
		out[pos+3] = (byte)(0xFF & (value >> 24));
		return pos + 4;
	}

	private int setUint16(byte[] out, int pos, int value) {
		out[pos] = (byte)(0xFF & value);
		out[pos+1] = (byte)(0xFF & (value >> 8));
		return pos + 2;
	}

	private int setUint32(byte[] out, int pos, long value) {
		out[pos] = (byte)(0xFF & value);
		out[pos+1] = (byte)(0xFF & (value >> 8));
		out[pos+2] = (byte)(0xFF & (value >> 16));
		out[pos+3] = (byte)(0xFF & (value >> 24));
		return pos + 4;
	}
}
