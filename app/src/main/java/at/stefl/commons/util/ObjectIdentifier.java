package at.stefl.commons.util;

import java.util.Arrays;

public class ObjectIdentifier {
    
    private static final long MAX_SUB_ID = 0xffffffffl;
    private static final long SUB_ID_MASK = 0xffffffffl;
    private static final int[] NULL_OID = { 0 };
    
    private final int[] value;
    
    public ObjectIdentifier() {
        value = NULL_OID;
    }
    
    public ObjectIdentifier(int[] oid) {
        value = Arrays.copyOf(oid, oid.length);
    }
    
    public ObjectIdentifier(String oid) {
        String[] subIDs = oid.split("\\.");
        
        if (oid.replaceAll("\\.", "").length() != (oid.length() - subIDs.length + 1)) throw new IllegalArgumentException(
                "Separators are malformed!");
        
        value = new int[subIDs.length];
        
        for (int i = 0; i < subIDs.length; i++) {
            long subID = Long.parseLong(subIDs[i]);
            
            if (subID < 0) throw new IllegalArgumentException(
                    "Sub ID cannot be negative!");
            if (subID > MAX_SUB_ID) throw new IllegalArgumentException(
                    "Sub ID cannot be greater than " + MAX_SUB_ID + "!");
            
            value[i] = (int) subID;
        }
    }
    
    public ObjectIdentifier(int[] parentOID, int[] childOID) {
        value = new int[parentOID.length + childOID.length];
        System.arraycopy(parentOID, 0, value, 0, parentOID.length);
        System.arraycopy(childOID, 0, value, parentOID.length, childOID.length);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < value.length; i++) {
            long subID = value[i] & SUB_ID_MASK;
            builder.append(subID);
            if (i < value.length - 1) builder.append(".");
        }
        
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof ObjectIdentifier)) return false;
        ObjectIdentifier oid = (ObjectIdentifier) obj;
        
        return Arrays.equals(value, oid.value);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
    
    public int[] getValue() {
        return Arrays.copyOf(value, value.length);
    }
    
    public boolean startsWith(ObjectIdentifier oid) {
        // TODO: or false?
        if (oid.value.length > value.length) throw new IllegalArgumentException(
                "The given OID is longer than this!");
        
        for (int i = 0; i < oid.value.length; i++) {
            if (value[i] != oid.value[i]) return false;
        }
        
        return true;
    }
    
    public ObjectIdentifier append(ObjectIdentifier oid) {
        return new ObjectIdentifier(value, oid.value);
    }
    
}