package at.stefl.svm.enumeration;

import java.util.Map;

import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.object.ObjectTransformer;

public enum HatchStyle {
    
    SINGLE(HatchStyleConstants.HATCH_SINGLE), DOUBLE(
            HatchStyleConstants.HATCH_DOUBLE), TRIPLE(
            HatchStyleConstants.HATCH_TRIPLE), FORCE_EQUAL_SIZE(
            HatchStyleConstants.HATCH_FORCE_EQUAL_SIZE);
    
    private static final ObjectTransformer<HatchStyle, Integer> CODE_KEY_GENERATOR = new ObjectTransformer<HatchStyle, Integer>() {
        
        @Override
        public Integer transform(HatchStyle value) {
            return value.code;
        }
    };
    
    private static final Map<Integer, HatchStyle> BY_CODE_MAP;
    
    static {
        BY_CODE_MAP = CollectionUtil.toHashMap(CODE_KEY_GENERATOR, values());
    }
    
    public static HatchStyle getByCode(int code) {
        return BY_CODE_MAP.get(code);
    }
    
    private final int code;
    
    private HatchStyle(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
}