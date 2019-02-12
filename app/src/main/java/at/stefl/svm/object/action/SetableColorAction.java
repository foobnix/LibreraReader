package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public abstract class SetableColorAction extends ColorAction {
    
    private boolean set;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [color=");
        builder.append(getColor());
        builder.append(", set=");
        builder.append(set);
        builder.append("]");
        return builder.toString();
    }
    
    public boolean isSet() {
        return set;
    }
    
    public void setSet(boolean set) {
        this.set = set;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        super.serializeContent(out);
        out.writeBoolean(set);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        super.deserializeContent(in, version, length);
        set = in.readBoolean();
    }
    
}