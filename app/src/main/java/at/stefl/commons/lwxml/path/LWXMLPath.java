package at.stefl.commons.lwxml.path;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LWXMLPath {
    
    private static final String NODE_SEPARATOR = "/";
    private static final Pattern PATTERN = Pattern
            .compile("(.+?)(\\[(.+)\\])?$");
    
    private final List<LWXMLNodeIdentifier> nodeIdentifierList;
    
    private LWXMLPath(List<LWXMLNodeIdentifier> nodeIdentifierList,
            boolean dummy) {
        this.nodeIdentifierList = nodeIdentifierList;
    }
    
    public LWXMLPath(List<LWXMLNodeIdentifier> nodeIdentifierList) {
        this.nodeIdentifierList = new ArrayList<LWXMLNodeIdentifier>(
                nodeIdentifierList);
    }
    
    public LWXMLPath(String path) {
        nodeIdentifierList = new LinkedList<LWXMLNodeIdentifier>();
        
        for (String segment : path.split(NODE_SEPARATOR)) {
            Matcher matcher = PATTERN.matcher(segment);
            
            if (!matcher.matches()) throw new IllegalArgumentException(
                    "invalid syntax");
            
            String node = matcher.group(1);
            int index = (matcher.group(3) == null) ? 0 : Integer
                    .parseInt(matcher.group(3));
            LWXMLNodeIdentifier nodeIdentifier = new LWXMLNodeIdentifier(node,
                    index);
            nodeIdentifierList.add(nodeIdentifier);
        }
    }
    
    public LWXMLPath(LWXMLPath path, LWXMLPath child) {
        nodeIdentifierList = new ArrayList<LWXMLNodeIdentifier>(path.getDepth()
                + child.getDepth());
        
        nodeIdentifierList.addAll(path.nodeIdentifierList);
        nodeIdentifierList.addAll(child.nodeIdentifierList);
    }
    
    public LWXMLPath(LWXMLPath path, String child) {
        this(path, new LWXMLPath(child));
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        for (LWXMLNodeIdentifier nodeIdentifier : nodeIdentifierList) {
            builder.append(NODE_SEPARATOR);
            builder.append(nodeIdentifier);
        }
        
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof LWXMLPath)) return false;
        LWXMLPath path = (LWXMLPath) obj;
        
        return nodeIdentifierList.equals(path.nodeIdentifierList);
    }
    
    @Override
    public int hashCode() {
        return nodeIdentifierList.hashCode();
    }
    
    public int getDepth() {
        return nodeIdentifierList.size();
    }
    
    public LWXMLNodeIdentifier getNodeIdentifier(int index) {
        return nodeIdentifierList.get(index);
    }
    
    public List<LWXMLNodeIdentifier> getNodeIdentifierList() {
        return new ArrayList<LWXMLNodeIdentifier>(nodeIdentifierList);
    }
    
    public LWXMLPath append(LWXMLPath child) {
        return new LWXMLPath(this, child);
    }
    
    public LWXMLPath subPath(int start) {
        return subPath(start, getDepth());
    }
    
    public LWXMLPath subPath(int start, int end) {
        if (start < 0) throw new IndexOutOfBoundsException(
                "start cannot be less than 0");
        if (end < 0) throw new IndexOutOfBoundsException(
                "start cannot be less than 0");
        if (start > end) throw new IllegalArgumentException(
                "end must be greater than start");
        
        return new LWXMLPath(nodeIdentifierList.subList(start, end), true);
    }
    
    public boolean startsWith(LWXMLPath path) {
        if (path.getDepth() > getDepth()) throw new IllegalArgumentException(
                "the argument is too big");
        
        for (int i = 0; i < path.getDepth(); i++) {
            if (!getNodeIdentifier(i).equals(path.getNodeIdentifier(i))) return false;
        }
        
        return true;
    }
    
    public boolean endsWith(LWXMLPath path) {
        if (path.getDepth() > getDepth()) throw new IllegalArgumentException(
                "the argument is too big");
        
        for (int i = 1; i <= path.getDepth(); i++) {
            if (!getNodeIdentifier(getDepth() - i).equals(
                    path.getNodeIdentifier(path.getDepth() - i))) return false;
        }
        
        return true;
    }
    
}