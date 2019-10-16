package at.stefl.commons.util;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: improve
public class QuickPattern {
    
    private Pattern pattern;
    private int group;
    
    private Matcher lastMatcher;
    
    public QuickPattern(Pattern pattern) {
        this(pattern, -1);
    }
    
    public QuickPattern(String regex) {
        this(regex, -1);
    }
    
    public QuickPattern(Pattern pattern, int group) {
        this.pattern = pattern;
        this.group = group;
    }
    
    public QuickPattern(String regex, int group) {
        this(Pattern.compile(regex), group);
    }
    
    public QuickPattern(String regex, int flags, int group) {
        this(Pattern.compile(regex, flags), group);
    }
    
    public Pattern getPattern() {
        return pattern;
    }
    
    public int getGroup() {
        return group;
    }
    
    public Matcher getLastMatcher() {
        return lastMatcher;
    }
    
    public String matchGroup(String string) {
        lastMatcher = pattern.matcher(string);
        if (!lastMatcher.matches()) return null;
        return lastMatcher.group(group);
    }
    
    public String[] matchGroups(String string) {
        lastMatcher = pattern.matcher(string);
        if (!lastMatcher.matches()) return null;
        
        String[] result = new String[lastMatcher.groupCount()];
        for (int i = 1; i <= lastMatcher.groupCount(); i++)
            result[i] = lastMatcher.group(i);
        return result;
    }
    
    public String findGroup(String string) {
        return findGroup(string, 0);
    }
    
    public String findGroup(String string, int offset) {
        lastMatcher = pattern.matcher(string);
        if (!lastMatcher.find(offset)) return null;
        return lastMatcher.group(group);
    }
    
    public String[] findGroups(String string) {
        lastMatcher = pattern.matcher(string);
        if (!lastMatcher.find()) return null;
        
        String[] result = new String[lastMatcher.groupCount()];
        for (int i = 1; i <= lastMatcher.groupCount(); i++)
            result[i - 1] = lastMatcher.group(i);
        return result;
    }
    
    public List<String> findGroupAll(String string) {
        List<String> result = new LinkedList<String>();
        
        int offset = 0;
        while (true) {
            String find = findGroup(string, offset);
            if (find == null) break;
            result.add(find);
            offset = lastMatcher.end();
        }
        
        return result;
    }
}