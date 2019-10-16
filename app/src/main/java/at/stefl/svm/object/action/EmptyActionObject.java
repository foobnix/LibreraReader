package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public abstract class EmptyActionObject extends SVMAction {
    
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