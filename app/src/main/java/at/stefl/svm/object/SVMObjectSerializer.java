package at.stefl.svm.object;

import java.io.IOException;

import at.stefl.svm.io.SVMDataOutputStream;

public interface SVMObjectSerializer {
    
    public void serialize(SVMDataOutputStream out) throws IOException;
    
}