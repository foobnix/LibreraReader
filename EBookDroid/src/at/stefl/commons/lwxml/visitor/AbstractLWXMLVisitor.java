package at.stefl.commons.lwxml.visitor;

public abstract class AbstractLWXMLVisitor implements LWXMLVisitor {
    
    private LWXMLHost currentHost;
    
    public synchronized LWXMLHost getCurrentHost() {
        return currentHost;
    }
    
    @Override
    public synchronized void visitHost(LWXMLHost host) {
        if (host != currentHost) throw new IllegalArgumentException();
        this.currentHost = host;
    }
    
    @Override
    public void visitEnd() {
        stopVisiting();
    }
    
    @Override
    public void visitProcessingInstruction(String target, String data) {}
    
    @Override
    public void visitComment(String text) {}
    
    @Override
    public void visitStartElement(String name) {}
    
    @Override
    public void visitEndAttributeList() {}
    
    @Override
    public void visitEndElement(String name) {}
    
    @Override
    public void visitAttribute(String name, String value) {}
    
    @Override
    public void visitCharacters(String text) {}
    
    @Override
    public void visitCDATA(String text) {}
    
    public synchronized void stopVisiting() {
        currentHost = null;
    }
    
}