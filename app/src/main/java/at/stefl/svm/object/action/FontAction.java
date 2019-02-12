package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.basic.FontDefinition;

public class FontAction extends SVMAction {
    
    private FontDefinition fontDefinition;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FontAction [fontDefinition=");
        builder.append(fontDefinition);
        builder.append("]");
        return builder.toString();
    }
    
    public FontDefinition getFontDefinition() {
        return fontDefinition;
    }
    
    public void setFontDefinition(FontDefinition fontDefinition) {
        this.fontDefinition = fontDefinition;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        fontDefinition.serialize(out);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        fontDefinition = new FontDefinition();
        fontDefinition.deserialize(in);
    }
    
}