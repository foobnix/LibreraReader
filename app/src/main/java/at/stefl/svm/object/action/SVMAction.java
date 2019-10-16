package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.enumeration.ActionType;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.object.SVMVersionObject;

public abstract class SVMAction extends SVMVersionObject {
    
    public ActionType getActionType() {
        return ActionType.getByClass(getClass());
    }
    
    @Override
    public SVMAction deserialize(SVMDataInputStream in) throws IOException {
        return (SVMAction) super.deserialize(in);
    }
    
}