package at.stefl.svm.object;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;

public interface SVMObjectDeserializer {
    
    public SVMObject deserialize(SVMDataInputStream in) throws IOException;
    
}