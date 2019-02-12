package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class TextLanguageAction extends SVMAction {
    
    private int language;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TextLanguageAction [language=");
        builder.append(language);
        builder.append("]");
        return builder.toString();
    }
    
    public int getLanguage() {
        return language;
    }
    
    public void setLanguage(int language) {
        this.language = language;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeUnsignedShort(language);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        language = in.readUnsignedShort();
    }
    
}