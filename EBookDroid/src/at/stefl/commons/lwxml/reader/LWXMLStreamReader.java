package at.stefl.commons.lwxml.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.Charset;

import at.stefl.commons.io.ApplyFilterReader;
import at.stefl.commons.io.CharFilter;
import at.stefl.commons.io.CharStreamUtil;
import at.stefl.commons.io.FullyReader;
import at.stefl.commons.io.UntilCharReader;
import at.stefl.commons.io.UntilCharSequenceReader;
import at.stefl.commons.io.UntilFilterReader;
import at.stefl.commons.lwxml.LWXMLConstants;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.util.InaccessibleSectionException;

// TODO: improve code
// TODO: improve malformed xml handling
// TODO: use xml escaping decoder
public class LWXMLStreamReader extends LWXMLReader {
    
    // TODO: remove
    private static final String DEFAULT_CHARSET = "UTF-8";
    
    private static final int PUSHBACK_BUFFER_SIZE = 1;
    
    private static final CharFilter WHITESPACE_FILTER = new CharFilter() {
        @Override
        public boolean accept(char c) {
            return !LWXMLConstants.isWhitespace(c);
        }
    };
    
    private final CharFilter startElementFilter = new CharFilter() {
        @Override
        public boolean accept(char c) {
            if (LWXMLConstants.isWhitespace(c)) return false;
            
            switch (c) {
            case '/':
            case '>':
                try {
                    LWXMLStreamReader.this.in.unread(c);
                    return false;
                } catch (IOException e) {
                    throw new InaccessibleSectionException();
                }
            }
            
            return true;
        }
    };
    
    private static final char[] CDATA_CHARS = "[CDATA[".toCharArray();
    // TODO: remove
    private static final char[] DOCTYPE_CHARS = "DOCTYPE".toCharArray();
    
    private boolean closed;
    
    private final PushbackReader in;
    private final FullyReader fin;
    
    private final UntilCharReader endElementIn;
    private final UntilCharSequenceReader commentIn;
    private final UntilCharSequenceReader cdataIn;
    private final UntilFilterReader processingInstructionTargetIn;
    private final UntilCharSequenceReader processingInstructionDataIn;
    private final UntilFilterReader startElementIn;
    private final UntilCharReader attributeNameIn;
    private final UntilCharReader attributeValueIn;
    private final UntilCharReader characterIn;
    
    private LWXMLEvent lastEvent;
    private long eventNumber = -1;
    
    private boolean handleAttributeList;
    private boolean handleEndEmptyElement;
    
    private Reader eventReader;
    
    // TODO: remove
    public LWXMLStreamReader(InputStream in) {
        this(in, Charset.forName(DEFAULT_CHARSET));
    }
    
    // TODO: remove
    public LWXMLStreamReader(InputStream in, Charset charset) {
        this(new BufferedReader(new InputStreamReader(in, charset)));
    }
    
    public LWXMLStreamReader(Reader in) {
        this.in = new PushbackReader(in, PUSHBACK_BUFFER_SIZE);
        this.fin = new FullyReader(this.in);
        
        endElementIn = new UntilCharReader(new ApplyFilterReader(fin,
                WHITESPACE_FILTER), '>');
        commentIn = new UntilCharSequenceReader(fin, "-->");
        cdataIn = new UntilCharSequenceReader(fin, "]]>");
        processingInstructionTargetIn = new UntilFilterReader(fin,
                WHITESPACE_FILTER);
        processingInstructionDataIn = new UntilCharSequenceReader(fin, "?>");
        startElementIn = new UntilFilterReader(fin, startElementFilter);
        attributeNameIn = new UntilCharReader(new ApplyFilterReader(fin,
                WHITESPACE_FILTER), '=');
        attributeValueIn = new UntilCharReader(fin, '"');
        characterIn = new UntilCharReader(this.in, '<');
    }
    
    @Override
    public LWXMLEvent getCurrentEvent() {
        return lastEvent;
    }
    
    @Override
    public long getCurrentEventNumber() {
        return eventNumber;
    }
    
    @Override
    public LWXMLEvent readEvent() throws IOException {
        return lastEvent = readNextEventImpl();
    }
    
    private LWXMLEvent readNextEventImpl() throws IOException {
        if (closed) return LWXMLEvent.END_DOCUMENT;
        if (eventReader != null) CharStreamUtil.flushCharwise(eventReader);
        
        eventNumber++;
        
        if (lastEvent != null) {
            switch (lastEvent) {
            case PROCESSING_INSTRUCTION_TARGET:
                handleProcessingInstructionData();
                return LWXMLEvent.PROCESSING_INSTRUCTION_DATA;
            case ATTRIBUTE_NAME:
                handleAttributeValue();
                return LWXMLEvent.ATTRIBUTE_VALUE;
            case CHARACTERS:
                return handleElement();
            default:
                break;
            }
        }
        
        if (handleAttributeList) return handleAttributeList();
        if (handleEndEmptyElement) {
            handleEndEmptyElement();
            return LWXMLEvent.END_EMPTY_ELEMENT;
        }
        
        int read = in.read();
        switch (read) {
        case -1:
            close();
            return LWXMLEvent.END_DOCUMENT;
        case '<':
            return handleElement();
        default:
            in.unread(read);
            handleCharacters();
            return LWXMLEvent.CHARACTERS;
        }
    }
    
    private LWXMLEvent handleElement() throws IOException {
        int c = in.read();
        
        switch (c) {
        case -1:
            close();
            return LWXMLEvent.END_DOCUMENT;
        case '/':
            handleEndElement();
            return LWXMLEvent.END_ELEMENT;
        case '!':
            return handleCallsign();
        case '?':
            handleProcessingInstructionTarget();
            return LWXMLEvent.PROCESSING_INSTRUCTION_TARGET;
        default:
            in.unread(c);
            handleStartElement();
            return LWXMLEvent.START_ELEMENT;
        }
    }
    
    // TODO: another way w/o new?
    private void handleEndElement() throws IOException {
        endElementIn.reset();
        eventReader = endElementIn;
    }
    
    private void handleEndEmptyElement() throws IOException {
        handleEndEmptyElement = false;
        eventReader = null;
    }
    
    // TODO: improve
    private LWXMLEvent handleCallsign() throws IOException {
        int c = fin.read();
        
        switch (c) {
        case '-':
            if (fin.read() != '-') throw new LWXMLReaderException(
                    "malformend tag: comment was expected");
            
            handleComment();
            return LWXMLEvent.COMMENT;
        case '[':
            if (!CharStreamUtil.matchChars(in, CDATA_CHARS, 1)) throw new LWXMLReaderException(
                    "malformed tag: cdata was expected");
            
            handleCDATA();
            return LWXMLEvent.CDATA;
        case 'D':
            // TODO: remove
            if (!CharStreamUtil.matchChars(in, DOCTYPE_CHARS, 1)) throw new LWXMLReaderException(
                    "malformed tag: doctype expected");
            
            // TODO: improve
            CharStreamUtil.flushUntilChar(fin, '>');
            return readEvent();
        default:
            throw new LWXMLReaderException(
                    "malformed tag: comment or cdata was expected");
        }
    }
    
    private void handleComment() throws IOException {
        commentIn.reset();
        eventReader = commentIn;
    }
    
    private void handleCDATA() throws IOException {
        cdataIn.reset();
        eventReader = cdataIn;
    }
    
    private void handleProcessingInstructionTarget() throws IOException {
        processingInstructionTargetIn.reset();
        eventReader = processingInstructionTargetIn;
    }
    
    private void handleProcessingInstructionData() throws IOException {
        CharStreamUtil.flushUntilFilter(in, WHITESPACE_FILTER);
        
        processingInstructionDataIn.reset();
        eventReader = processingInstructionDataIn;
    }
    
    private void handleStartElement() throws IOException {
        handleAttributeList = true;
        startElementIn.reset();
        eventReader = startElementIn;
    }
    
    private LWXMLEvent handleAttributeList() throws IOException {
        CharStreamUtil.flushUntilFilter(in, WHITESPACE_FILTER);
        int c = fin.read();
        
        switch (c) {
        case '/':
            // TODO: flush whitespace?
            if (fin.read() != '>') throw new LWXMLReaderException(
                    "malformed tag: expected '>'");
            handleEndEmptyElement = true;
        case '>':
            handleAttributeList = false;
            eventReader = null;
            return LWXMLEvent.END_ATTRIBUTE_LIST;
        default:
            in.unread(c);
            handleAttributeName();
            return LWXMLEvent.ATTRIBUTE_NAME;
        }
    }
    
    private void handleAttributeName() throws IOException {
        attributeNameIn.reset();
        eventReader = attributeNameIn;
    }
    
    // TODO: handle malformed xml
    private void handleAttributeValue() throws IOException {
        CharStreamUtil.flushUntilChar(in, '"');
        
        attributeValueIn.reset();
        eventReader = attributeValueIn;
    }
    
    private void handleCharacters() throws IOException {
        characterIn.reset();
        eventReader = characterIn;
    }
    
    @Override
    public int read() throws IOException {
        if (eventReader == null) return -1;
        return eventReader.read();
    }
    
    @Override
    public void close() throws IOException {
        if (closed) return;
        
        closed = true;
        eventReader = null;
        
        in.close();
    }
    
}