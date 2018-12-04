package at.stefl.svm.object.basic;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.commons.util.PrimitiveUtil;
import at.stefl.svm.enumeration.TextEncoding;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.SVMVersionObject;

public class FontDefinition extends SVMVersionObject {
    
    private String familyName;
    private String styleName;
    private Vector2i size;
    private int charset;
    private int family;
    private int pitch;
    private int weigth;
    private int underline;
    private int strikeout;
    private int italic;
    private int language;
    private int width;
    private int orientation;
    private boolean wordline;
    private boolean outline;
    private boolean shadow;
    private byte kerning;
    
    private byte relief;
    private int cjkLanguage;
    private boolean vertical;
    private int emphasisMark;
    
    private int overline;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FontDefinition [familyName=");
        builder.append(familyName);
        builder.append(", styleName=");
        builder.append(styleName);
        builder.append(", size=");
        builder.append(size);
        builder.append(", charset=");
        builder.append(charset);
        builder.append(", family=");
        builder.append(family);
        builder.append(", pitch=");
        builder.append(pitch);
        builder.append(", weigth=");
        builder.append(weigth);
        builder.append(", underline=");
        builder.append(underline);
        builder.append(", strikeout=");
        builder.append(strikeout);
        builder.append(", italic=");
        builder.append(italic);
        builder.append(", language=");
        builder.append(language);
        builder.append(", width=");
        builder.append(width);
        builder.append(", orientation=");
        builder.append(orientation);
        builder.append(", wordline=");
        builder.append(wordline);
        builder.append(", outline=");
        builder.append(outline);
        builder.append(", shadow=");
        builder.append(shadow);
        builder.append(", kerning=");
        builder.append(kerning);
        builder.append(", relief=");
        builder.append(relief);
        builder.append(", cjkLanguage=");
        builder.append(cjkLanguage);
        builder.append(", vertical=");
        builder.append(vertical);
        builder.append(", emphasisMark=");
        builder.append(emphasisMark);
        builder.append(", overline=");
        builder.append(overline);
        builder.append("]");
        return builder.toString();
    }
    
    public String getFamilyName() {
        return familyName;
    }
    
    public String getStyleName() {
        return styleName;
    }
    
    public Vector2i getSize() {
        return size;
    }
    
    public int getCharset() {
        return charset;
    }
    
    public TextEncoding getTextEncoding() {
        return TextEncoding.getByCode(getCharset());
    }
    
    public int getFamily() {
        return family;
    }
    
    public int getPitch() {
        return pitch;
    }
    
    public int getWeigth() {
        return weigth;
    }
    
    public int getUnderline() {
        return underline;
    }
    
    public int getStrikeout() {
        return strikeout;
    }
    
    public int getItalic() {
        return italic;
    }
    
    public int getLanguage() {
        return language;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getOrientation() {
        return orientation;
    }
    
    public boolean isWordline() {
        return wordline;
    }
    
    public boolean isOutline() {
        return outline;
    }
    
    public boolean isShadow() {
        return shadow;
    }
    
    public byte getKerning() {
        return kerning;
    }
    
    public byte getRelief() {
        return relief;
    }
    
    public int getCJKLanguage() {
        return cjkLanguage;
    }
    
    public boolean isVertical() {
        return vertical;
    }
    
    public int getEmphasisMark() {
        return emphasisMark;
    }
    
    public int getOverline() {
        return overline;
    }
    
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }
    
    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }
    
    public void setSize(Vector2i size) {
        this.size = size;
    }
    
    public void setCharset(int charset) {
        PrimitiveUtil.checkUnsignedShort(charset);
        this.charset = charset;
    }
    
    public void setTextEncoding(TextEncoding textEncoding) {
        setCharset(textEncoding.getCode());
    }
    
    public void setFamily(int family) {
        PrimitiveUtil.checkUnsignedShort(family);
        this.family = family;
    }
    
    public void setPitch(int pitch) {
        PrimitiveUtil.checkUnsignedShort(pitch);
        this.pitch = pitch;
    }
    
    public void setWeigth(int weigth) {
        PrimitiveUtil.checkUnsignedShort(weigth);
        this.weigth = weigth;
    }
    
    public void setUnderline(int underline) {
        PrimitiveUtil.checkUnsignedShort(underline);
        this.underline = underline;
    }
    
    public void setStrikeout(int strikeout) {
        PrimitiveUtil.checkUnsignedShort(strikeout);
        this.strikeout = strikeout;
    }
    
    public void setItalic(int italic) {
        PrimitiveUtil.checkUnsignedShort(italic);
        this.italic = italic;
    }
    
    public void setLanguage(int language) {
        PrimitiveUtil.checkUnsignedShort(language);
        this.language = language;
    }
    
    public void setWidth(int width) {
        PrimitiveUtil.checkUnsignedShort(width);
        this.width = width;
    }
    
    public void setOrientation(int orientation) {
        PrimitiveUtil.checkUnsignedShort(orientation);
        this.orientation = orientation;
    }
    
    public void setWordline(boolean wordline) {
        this.wordline = wordline;
    }
    
    public void setOutline(boolean outline) {
        this.outline = outline;
    }
    
    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }
    
    public void setKerning(byte kerning) {
        this.kerning = kerning;
    }
    
    public void setRelief(byte relief) {
        this.relief = relief;
    }
    
    public void setCJKLanguage(int cjkLanguage) {
        PrimitiveUtil.checkUnsignedShort(cjkLanguage);
        this.cjkLanguage = cjkLanguage;
    }
    
    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }
    
    public void setEmphasisMark(int emphasis) {
        PrimitiveUtil.checkUnsignedShort(emphasis);
        this.emphasisMark = emphasis;
    }
    
    public void setOverline(int overline) {
        PrimitiveUtil.checkUnsignedShort(overline);
        this.overline = overline;
    }
    
    @Override
    public int getVersion() {
        return 3;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        // TODO: replace?
        // out.writeUnicodeOrAsciiString(familyName);
        // out.writeUnicodeOrAsciiString(styleName);
        out.writeUnsignedShortPrefixedAsciiString(familyName);
        out.writeUnsignedShortPrefixedAsciiString(styleName);
        out.writeSize(size);
        out.writeUnsignedShort(charset);
        out.writeUnsignedShort(family);
        out.writeUnsignedShort(pitch);
        out.writeUnsignedShort(weigth);
        out.writeUnsignedShort(underline);
        out.writeUnsignedShort(strikeout);
        out.writeUnsignedShort(italic);
        out.writeUnsignedShort(language);
        out.writeUnsignedShort(width);
        out.writeUnsignedShort(orientation);
        out.writeBoolean(wordline);
        out.writeBoolean(outline);
        out.writeBoolean(shadow);
        out.writeByte(kerning);
        
        // version 2
        out.writeByte(relief);
        out.writeUnsignedShort(cjkLanguage);
        out.writeBoolean(vertical);
        out.writeUnsignedShort(emphasisMark);
        
        // version 3
        out.writeUnsignedShort(overline);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        // TODO: replace?
        // familyName = in.readUnicodeOrAsciiString();
        // styleName = in.readUnicodeOrAsciiString();
        familyName = in.readUnsignedShortPrefixedAsciiString();
        styleName = in.readUnsignedShortPrefixedAsciiString();
        size = in.readSize();
        charset = in.readUnsignedShort();
        family = in.readUnsignedShort();
        pitch = in.readUnsignedShort();
        weigth = in.readUnsignedShort();
        underline = in.readUnsignedShort();
        strikeout = in.readUnsignedShort();
        italic = in.readUnsignedShort();
        language = in.readUnsignedShort();
        width = in.readUnsignedShort();
        orientation = in.readUnsignedShort();
        wordline = in.readBoolean();
        outline = in.readBoolean();
        shadow = in.readBoolean();
        kerning = in.readByte();
        
        if (version >= 2) {
            relief = in.readByte();
            cjkLanguage = in.readUnsignedShort();
            vertical = in.readBoolean();
            emphasisMark = in.readUnsignedShort();
            
            if (version >= 3) {
                overline = in.readUnsignedShort();
            }
        }
    }
    
}