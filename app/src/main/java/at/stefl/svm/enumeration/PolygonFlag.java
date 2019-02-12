package at.stefl.svm.enumeration;

import java.util.Map;

import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.object.ObjectTransformer;

public enum PolygonFlag {
    
    NORMAL(PolygonFlagConstants.POLY_NORMAL), SMOOTH(
            PolygonFlagConstants.POLY_SMOOTH), CONTROL(
            PolygonFlagConstants.POLY_CONTROL), SYMMETRY(
            PolygonFlagConstants.POLY_SYMMTR);
    
    private static final ObjectTransformer<PolygonFlag, Byte> CODE_KEY_GENERATOR = new ObjectTransformer<PolygonFlag, Byte>() {
        
        @Override
        public Byte transform(PolygonFlag value) {
            return value.code;
        }
    };
    
    private static final Map<Byte, PolygonFlag> BY_CODE_MAP;
    
    static {
        BY_CODE_MAP = CollectionUtil.toHashMap(CODE_KEY_GENERATOR, values());
    }
    
    public static PolygonFlag getActionTypeByCode(int code) {
        return BY_CODE_MAP.get(code);
    }
    
    private final byte code;
    
    private PolygonFlag(byte code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
}