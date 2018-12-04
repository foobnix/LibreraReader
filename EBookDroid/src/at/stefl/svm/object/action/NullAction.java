package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class NullAction extends SVMAction {
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NullAction []");
        return builder.toString();
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {}
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {}
    
}