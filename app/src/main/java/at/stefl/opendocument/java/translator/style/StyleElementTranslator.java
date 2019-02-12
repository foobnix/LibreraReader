package at.stefl.opendocument.java.translator.style;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLReader;

public abstract class StyleElementTranslator<T extends DocumentStyle> {
    
    public abstract void translate(LWXMLReader in, T out) throws IOException;
    
}