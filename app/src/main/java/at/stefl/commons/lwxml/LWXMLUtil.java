package at.stefl.commons.lwxml;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.stefl.commons.io.CharStreamUtil;
import at.stefl.commons.io.StreamableStringMap;
import at.stefl.commons.io.StreamableStringSet;
import at.stefl.commons.lwxml.path.LWXMLNodeIdentifier;
import at.stefl.commons.lwxml.path.LWXMLPath;
import at.stefl.commons.lwxml.reader.LWXMLBranchReader;
import at.stefl.commons.lwxml.reader.LWXMLElementReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.reader.LWXMLStreamReader;
import at.stefl.commons.util.collection.OrderedPair;

// TODO: make use of EOFException
public class LWXMLUtil {
    
    public static void flush(LWXMLReader in) throws IOException {
        while (in.readEvent() != LWXMLEvent.END_DOCUMENT)
            ;
    }
    
    public static void flushBranch(LWXMLReader in) throws IOException {
        flush(new LWXMLBranchReader(in));
    }
    
    public static void flushElement(LWXMLReader in) throws IOException {
        flush(new LWXMLElementReader(in));
    }
    
    public static void flushUntilPath(InputStream in, LWXMLPath path)
            throws IOException {
        flushUntilPath(new LWXMLStreamReader(in), path);
    }
    
    public static void flushUntilPath(Reader in, LWXMLPath path)
            throws IOException {
        flushUntilPath(new LWXMLStreamReader(in), path);
    }
    
    public static void flushUntilPath(LWXMLReader in, LWXMLPath path)
            throws IOException {
        int depth = 0;
        int matchingIndex = 0;
        int nodeIndex = 0;
        
        while (true) {
            LWXMLEvent event = in.readEvent();
            if (event == LWXMLEvent.END_DOCUMENT) throw new EOFException();
            
            switch (event) {
            case START_ELEMENT:
                depth++;
                if (depth > (matchingIndex + 1)) break;
                
                LWXMLNodeIdentifier nodeIdentifier = path
                        .getNodeIdentifier(matchingIndex);
                if (!in.readValue().equals(nodeIdentifier.getElementName())) break;
                
                if (nodeIndex < nodeIdentifier.getIndex()) {
                    nodeIndex++;
                } else {
                    matchingIndex++;
                    nodeIndex = 0;
                    
                    if (matchingIndex >= path.getDepth()) return;
                }
                
                break;
            case END_EMPTY_ELEMENT:
            case END_ELEMENT:
                depth--;
                if (matchingIndex > depth) throw new EOFException();
                
                break;
            default:
                break;
            }
        }
    }
    
    public static void flushUntilEventNumber(LWXMLReader in, long eventNumber)
            throws IOException {
        while (true) {
            LWXMLEvent event = in.readEvent();
            if (event == LWXMLEvent.END_DOCUMENT) throw new EOFException();
            
            if (in.getCurrentEventNumber() >= eventNumber) return;
        }
    }
    
    public static void flushEmptyElement(LWXMLReader in) throws IOException {
        LWXMLEvent event = in.readEvent();
        if (event == LWXMLEvent.START_ELEMENT) event = in.readEvent();
        
        while (true) {
            switch (event) {
            case ATTRIBUTE_NAME:
            case ATTRIBUTE_VALUE:
            case END_ATTRIBUTE_LIST:
                break;
            case END_EMPTY_ELEMENT:
            case END_ELEMENT:
                return;
            default:
                throw new LWXMLIllegalEventException(event);
            }
            
            event = in.readEvent();
        }
    }
    
    public static void flushStartElement(LWXMLReader in) throws IOException {
        LWXMLEvent event = in.readEvent();
        if (event == LWXMLEvent.START_ELEMENT) event = in.readEvent();
        
        while (true) {
            switch (event) {
            case ATTRIBUTE_NAME:
            case ATTRIBUTE_VALUE:
                break;
            case END_ATTRIBUTE_LIST:
                return;
            default:
                throw new LWXMLIllegalEventException(event);
            }
            
            event = in.readEvent();
        }
    }
    
    public static void flushUntilEvent(LWXMLReader in, LWXMLEvent event)
            throws IOException {
        if (!event.hasValue()) throw new LWXMLIllegalEventException(event);
        
        while (true) {
            LWXMLEvent currentEvent = in.readEvent();
            if (currentEvent == LWXMLEvent.END_DOCUMENT) throw new EOFException();
            
            if (currentEvent == event) return;
        }
    }
    
    public static void flushUntilEventValue(LWXMLReader in, LWXMLEvent event,
            String value) throws IOException {
        if (!event.hasValue()) throw new LWXMLIllegalEventException(event);
        
        while (true) {
            LWXMLEvent currentEvent = in.readEvent();
            if (currentEvent == LWXMLEvent.END_DOCUMENT) throw new EOFException();
            
            if ((currentEvent == event) && in.readValue().equals(value)) return;
        }
    }
    
    public static void flushUntilStartElement(LWXMLReader in,
            String startElement) throws IOException {
        flushUntilEventValue(in, LWXMLEvent.START_ELEMENT, startElement);
    }
    
    public static void flushUntilEndElement(LWXMLReader in, String endElement)
            throws IOException {
        flushUntilEventValue(in, LWXMLEvent.END_ELEMENT, endElement);
    }
    
    public static LWXMLBranchReader getBranchReader(InputStream in,
            LWXMLPath path) throws IOException {
        return getBranchReader(new LWXMLStreamReader(in), path);
    }
    
    public static LWXMLBranchReader getBranchReader(Reader in, LWXMLPath path)
            throws IOException {
        return getBranchReader(new LWXMLStreamReader(in), path);
    }
    
    public static LWXMLBranchReader getBranchReader(LWXMLReader in,
            LWXMLPath path) throws IOException {
        flushUntilPath(in, path);
        return new LWXMLBranchReader(in);
    }
    
    public static String parseAttributeValue(InputStream in, LWXMLPath path,
            String attributeName) throws IOException {
        return parseAttributeValue(new LWXMLStreamReader(in), path,
                attributeName);
    }
    
    public static String parseAttributeValue(Reader in, LWXMLPath path,
            String attributeName) throws IOException {
        return parseAttributeValue(new LWXMLStreamReader(in), path,
                attributeName);
    }
    
    public static String parseAttributeValue(LWXMLReader in, LWXMLPath path,
            String attributeName) throws IOException {
        flushUntilPath(in, path);
        
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                if (!in.readValue().equals(attributeName)) continue;
                return in.readFollowingValue();
            case ATTRIBUTE_VALUE:
                break;
            case END_ATTRIBUTE_LIST:
                return null;
            default:
                break;
            }
        }
    }
    
    public static String parseFirstAttributeValue(InputStream in,
            String attributeName) throws IOException {
        return parseFirstAttributeValue(new LWXMLStreamReader(in),
                attributeName);
    }
    
    public static String parseFirstAttributeValue(Reader in,
            String attributeName) throws IOException {
        return parseFirstAttributeValue(new LWXMLStreamReader(in),
                attributeName);
    }
    
    public static String parseFirstAttributeValue(LWXMLReader in,
            String attributeName) throws IOException {
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                if (CharStreamUtil.matchChars(in, attributeName)) return in
                        .readFollowingValue();
            default:
                break;
            case END_DOCUMENT:
                return null;
            }
        }
    }
    
    public static List<String> parseAllAttributeValues(InputStream in,
            String attributeName) throws IOException {
        return parseAllAttributeValues(new LWXMLStreamReader(in), attributeName);
    }
    
    public static List<String> parseAllAttributeValues(Reader in,
            String attributeName) throws IOException {
        return parseAllAttributeValues(new LWXMLStreamReader(in), attributeName);
    }
    
    public static List<String> parseAllAttributeValues(LWXMLReader in,
            String attributeName) throws IOException {
        List<String> result = new LinkedList<String>();
        
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                if (in.readValue().equals(attributeName)) result.add(in
                        .readFollowingValue());
                break;
            case END_DOCUMENT:
                return result;
            default:
                break;
            }
        }
    }
    
    public static Map<String, String> parseAllAttributes(LWXMLReader in)
            throws IOException {
        Map<String, String> result = new HashMap<String, String>();
        
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                result.put(in.readValue(), in.readFollowingValue());
                break;
            case END_ATTRIBUTE_LIST:
                return result;
            default:
                throw new LWXMLIllegalEventException(event);
            }
        }
    }
    
    public static void parseAttributes(LWXMLReader in,
            StreamableStringSet attributes, Map<String, String> result)
            throws IOException {
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                String attribute = attributes.match(in);
                if (attribute == null) break;
                
                result.put(attribute, in.readFollowingValue());
                if (result.size() >= attributes.size()) return;
            case ATTRIBUTE_VALUE:
                break;
            case END_ATTRIBUTE_LIST:
                return;
            default:
                throw new LWXMLIllegalEventException(event);
            }
        }
    }
    
    public static void parseAttributes(LWXMLReader in,
            StreamableStringMap<String> map) throws IOException {
        int parsed = 0;
        
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                OrderedPair<String, String> match = map.match(in);
                if (match == null) break;
                
                map.put(match.getElement1(), in.readFollowingValue());
                if (++parsed >= map.size()) return;
            case ATTRIBUTE_VALUE:
                break;
            case END_ATTRIBUTE_LIST:
                return;
            default:
                throw new LWXMLIllegalEventException(event);
            }
        }
    }
    
    public static HashMap<String, String> parseAttributes(LWXMLReader in,
            StreamableStringSet attributes) throws IOException {
        HashMap<String, String> result = new HashMap<String, String>(
                attributes.size());
        parseAttributes(in, attributes, result);
        return result;
    }
    
    public static String parseSingleAttribute(LWXMLReader in,
            String attributeName) throws IOException {
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                if (CharStreamUtil.matchChars(in, attributeName)) return in
                        .readFollowingValue();
            case ATTRIBUTE_VALUE:
                break;
            case END_ATTRIBUTE_LIST:
                return null;
            default:
                throw new LWXMLIllegalEventException(event);
            }
        }
    }
    
    public static boolean isEmptyElement(LWXMLReader in) throws IOException {
        if (in.getCurrentEvent() != LWXMLEvent.END_ATTRIBUTE_LIST) flushStartElement(in);
        
        switch (in.readEvent()) {
        case END_EMPTY_ELEMENT:
        case END_ELEMENT:
            return true;
        default:
            break;
        }
        
        return false;
    }
    
    private LWXMLUtil() {}
    
}