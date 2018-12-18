package at.stefl.svm.enumeration;

import java.util.HashSet;
import java.util.Set;

public enum MirrorFlag {
    
    HORIZONTALLY(MirrorFlagConstants.MTF_MIRROR_HORZ), VERTICAL(
            MirrorFlagConstants.MTF_MIRROR_VERT);
    
    private final int mask;
    
    public static Set<MirrorFlag> getMirrorFlagsByCode(int code) {
        MirrorFlag[] flags = values();
        Set<MirrorFlag> result = new HashSet<MirrorFlag>(flags.length);
        
        for (int i = 0; i < flags.length; i++) {
            if ((code & flags[i].mask) != 0) result.add(flags[i]);
        }
        
        return result;
    }
    
    public static int getCodeByMirrorFlags(Set<MirrorFlag> flags) {
        int result = 0;
        
        for (MirrorFlag flag : flags) {
            result |= flag.mask;
        }
        
        return result;
    }
    
    private MirrorFlag(int mask) {
        this.mask = mask;
    }
    
    public int getMask() {
        return mask;
    }
    
}