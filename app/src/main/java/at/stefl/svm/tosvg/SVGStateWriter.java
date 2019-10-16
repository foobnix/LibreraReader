package at.stefl.svm.tosvg;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import at.stefl.commons.lwxml.writer.LWXMLStreamWriter;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.math.RectangleD;
import at.stefl.commons.math.vector.Vector2d;

public class SVGStateWriter {
    
    public interface StyleCallback {
        public void writeStyle(Writer out) throws IOException;
    }
    
    private LWXMLWriter out;
    
    private boolean started;
    private boolean ended;
    private boolean openElement;
    private boolean attributeWriteable;
    
    public SVGStateWriter(Writer out) {
        this.out = new LWXMLStreamWriter(out);
    }
    
    public SVGStateWriter(LWXMLWriter out) {
        this.out = out;
    }
    
    public void writeHeader() throws IOException {
        out.writeStartElement("svg");
        out.writeAttribute("xmlns", "http://www.w3.org/2000/svg");
        out.writeAttribute("version", "1.1");
        
        started = true;
        attributeWriteable = true;
    }
    
    public void writeFooter() throws IOException {
        preWrite();
        
        out.writeEndElement("svg");
        
        ended = true;
        attributeWriteable = false;
    }
    
    public void writeAttribute(String name, String value) throws IOException {
        if (!attributeWriteable) throw new IllegalStateException(
                "cannot write attribute in current state");
        
        out.writeAttribute(name, value);
    }
    
    private void closeOpenElement() throws IOException {
        out.writeEndEmptyElement();
        
        openElement = false;
        attributeWriteable = false;
    }
    
    private void preWrite() throws IOException {
        if (!started) throw new IllegalStateException(
                "header was not written since now");
        if (ended) throw new IllegalStateException("footer already writen");
        if (openElement) closeOpenElement();
    }
    
    private void postWrite(StyleCallback styleCallback) throws IOException {
        openElement = true;
        attributeWriteable = true;
        
        if (styleCallback != null) {
            out.writeAttribute("style", "");
            styleCallback.writeStyle(out);
        }
    }
    
    private void writeVector2iAttributes(Vector2d vector, String xAttribute,
            String yAttribute) throws IOException {
        out.writeAttribute(xAttribute, "" + vector.getX());
        out.writeAttribute(yAttribute, "" + vector.getY());
    }
    
    private void writePointAttributes(Vector2d point) throws IOException {
        writeVector2iAttributes(point, "x", "y");
    }
    
    private void writePoint1Attributes(Vector2d point) throws IOException {
        writeVector2iAttributes(point, "x1", "y1");
    }
    
    private void writePoint2Attributes(Vector2d point) throws IOException {
        writeVector2iAttributes(point, "x2", "y2");
    }
    
    private void writePointsAttributes(Collection<Vector2d> points)
            throws IOException {
        out.writeAttribute("points", "");
        
        for (Vector2d point : points) {
            out.write("" + point.getX());
            out.write(",");
            out.write("" + point.getY());
            // TODO: remove on last
            out.write(" ");
        }
    }
    
    private void writeCenterAttributes(Vector2d center) throws IOException {
        writeVector2iAttributes(center, "cx", "cy");
    }
    
    private void writeRadiusAttribute(double radius) throws IOException {
        writeRadiusAttributes(new Vector2d(radius));
    }
    
    private void writeRadiusAttributes(Vector2d radius) throws IOException {
        writeVector2iAttributes(radius, "rx", "ry");
    }
    
    private void writeRectangleAttributes(RectangleD rectangle)
            throws IOException {
        writePointAttributes(rectangle.getLeftTop());
        out.writeAttribute("width", "" + rectangle.getWidth());
        out.writeAttribute("height", "" + rectangle.getHeight());
    }
    
    public void writeLine(Vector2d point1, Vector2d point2) throws IOException {
        writeLine(point1, point2, null);
    }
    
    public void writeLine(Vector2d point1, Vector2d point2,
            StyleCallback styleCallback) throws IOException {
        preWrite();
        
        out.writeStartElement("line");
        writePoint1Attributes(point1);
        writePoint2Attributes(point2);
        
        postWrite(styleCallback);
    }
    
    public void writeRectange(RectangleD rectangle) throws IOException {
        writeRectange(rectangle, (StyleCallback) null);
    }
    
    public void writeRectange(RectangleD rectangle, StyleCallback styleCallback)
            throws IOException {
        preWrite();
        
        out.writeStartElement("rect");
        writeRectangleAttributes(rectangle);
        
        postWrite(styleCallback);
    }
    
    public void writeRectange(RectangleD rectangle, Vector2d radius)
            throws IOException {
        writeRectange(rectangle, radius, null);
    }
    
    public void writeRectange(RectangleD rectangle, Vector2d radius,
            StyleCallback styleCallback) throws IOException {
        preWrite();
        
        out.writeStartElement("rect");
        writeRectangleAttributes(rectangle);
        writeRadiusAttributes(radius);
        
        postWrite(styleCallback);
    }
    
    public void writeCircle(Vector2d center, double radius) throws IOException {
        writeCircle(center, radius, null);
    }
    
    public void writeCircle(Vector2d center, double radius,
            StyleCallback styleCallback) throws IOException {
        preWrite();
        
        out.writeStartElement("ellipse");
        writeCenterAttributes(center);
        writeRadiusAttribute(radius);
        
        postWrite(styleCallback);
    }
    
    public void writeEllipse(Vector2d center, Vector2d radius)
            throws IOException {
        writeEllipse(center, radius, null);
    }
    
    public void writeEllipse(Vector2d center, Vector2d radius,
            StyleCallback styleCallback) throws IOException {
        preWrite();
        
        out.writeStartElement("ellipse");
        writeCenterAttributes(center);
        
        postWrite(styleCallback);
    }
    
    public void writePolyLine(Collection<Vector2d> points) throws IOException {
        writePolyLine(points, null);
    }
    
    public void writePolyLine(Collection<Vector2d> points,
            StyleCallback styleCallback) throws IOException {
        preWrite();
        
        out.writeStartElement("polyline");
        writePointsAttributes(points);
        
        postWrite(styleCallback);
    }
    
    public void writePolygon(Collection<Vector2d> points) throws IOException {
        writePolygon(points, null);
    }
    
    public void writePolygon(Collection<Vector2d> points,
            StyleCallback styleCallback) throws IOException {
        preWrite();
        
        out.writeStartElement("polygon");
        writePointsAttributes(points);
        
        postWrite(styleCallback);
    }
    
    public void writeText(Vector2d point, String text) throws IOException {
        writeText(point, text, null);
    }
    
    public void writeText(Vector2d point, String text,
            StyleCallback styleCallback) throws IOException {
        preWrite();
        
        out.writeStartElement("text");
        writePointAttributes(point);
        
        postWrite(styleCallback);
        
        out.writeCharacters(text);
        
        out.writeEndElement("text");
        openElement = false;
        attributeWriteable = false;
    }
    
    public void flush() throws IOException {
        out.flush();
    }
    
    public void close() throws IOException {
        out.close();
    }
    
}