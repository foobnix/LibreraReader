package at.stefl.svm.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import at.stefl.commons.io.ByteStreamUtil;
import at.stefl.svm.enumeration.ActionType;
import at.stefl.svm.enumeration.SVMConstants;
import at.stefl.svm.object.SVMHeader;
import at.stefl.svm.object.action.FontAction;
import at.stefl.svm.object.action.SVMAction;

public class SVMReader {
    
    private final SVMDataInputStream in;
    
    private boolean headerRead;
    
    public SVMReader(SVMDataInputStream in) {
        this.in = in;
    }
    
    public SVMReader(InputStream in) {
        this(new SVMDataInputStream(in));
    }
    
    public SVMHeader readHeader() throws IOException {
        if (headerRead) throw new IllegalStateException("header already read");
        if (!ByteStreamUtil.matchBytes(in, SVMConstants.MAGIC_NUMBER)) throw new IllegalStateException(
                "uncorrect magic number");
        SVMHeader result = new SVMHeader().deserialize(in);
        headerRead = true;
        return result;
    }
    
    public SVMAction readAction() throws IOException {
        if (!headerRead) readHeader();
        
        int actionCode;
        
        try {
            actionCode = in.readUnsignedShort();
        } catch (EOFException e) {
            return null;
        }
        
        ActionType actionType = ActionType.getByCode(actionCode);
        SVMAction result = actionType.newActionObject().deserialize(in);
        
        if (result instanceof FontAction) {
            in.setDefaultEncoding(((FontAction) result).getFontDefinition()
                    .getTextEncoding());
        }
        
        return result;
    }
    
}