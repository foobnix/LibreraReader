package at.stefl.opendocument.java.odf;

import java.io.IOException;
import java.util.Set;

import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLIllegalEventException;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.util.array.ArrayUtil;

public class OpenDocumentUtil {
    
    private static final Set<String> POSSIBLY_EMPTY_ELEMENT_SET = ArrayUtil
            .toHashSet("text:p", "text:h");
    
    public static boolean isEmptyElement(LWXMLReader in) throws IOException {
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                String startElementName = in.readValue();
                
                if (POSSIBLY_EMPTY_ELEMENT_SET.contains(startElementName)) {
                    if (!isEmptyElement(in)) return false;
                } else {
                    return false;
                }
            case CHARACTERS:
                return false;
            case END_EMPTY_ELEMENT:
            case END_ELEMENT:
                return true;
            case END_DOCUMENT:
                throw new LWXMLIllegalEventException(event);
            default:
                break;
            }
        }
    }
    
    private OpenDocumentUtil() {}
    
}