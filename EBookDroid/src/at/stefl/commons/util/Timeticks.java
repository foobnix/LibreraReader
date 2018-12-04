package at.stefl.commons.util;

import at.stefl.commons.util.string.StringUtil;

public class Timeticks {
    
    private final long millis;
    
    public Timeticks() {
        millis = 0;
    }
    
    public Timeticks(long millis) {
        this.millis = millis;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        long tmp = millis;
        builder.insert(0, "." + StringUtil.fillFront((tmp % 1000) + "", '0', 3));
        tmp /= 1000;
        builder.insert(0, ":" + StringUtil.fillFront((tmp % 60) + "", '0', 2));
        tmp /= 60;
        builder.insert(0, ":" + StringUtil.fillFront((tmp % 60) + "", '0', 2));
        tmp /= 60;
        builder.insert(0, tmp % 60);
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof Timeticks)) return false;
        Timeticks timeTicks = (Timeticks) obj;
        
        return millis == timeTicks.millis;
    }
    
    @Override
    public int hashCode() {
        return new Long(millis).hashCode();
    }
    
    public long getMillis() {
        return millis;
    }
    
}