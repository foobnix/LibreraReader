package at.stefl.commons.lwxml.visitor;

public interface LWXMLVisitor {
    
    public void visitHost(LWXMLHost host);
    
    public void visitEnd();
    
    public void visitProcessingInstruction(String target, String data);
    
    public void visitComment(String text);
    
    public void visitStartElement(String name);
    
    public void visitEndAttributeList();
    
    public void visitEndElement(String name);
    
    public void visitAttribute(String name, String value);
    
    public void visitCharacters(String text);
    
    public void visitCDATA(String text);
    
}