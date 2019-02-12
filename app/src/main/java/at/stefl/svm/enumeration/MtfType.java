package at.stefl.svm.enumeration;

import java.util.Map;

import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.object.ObjectTransformer;

public enum MtfType {
    
    CONVERSION_NONE(MtfTypeConstants.MTF_CONVERSION_NONE),
    CONVERSION_1BIT_THRESHOLD(MtfTypeConstants.MTF_CONVERSION_1BIT_THRESHOLD),
    CONVERSION_8BIT_GREYS(MtfTypeConstants.MTF_CONVERSION_8BIT_GREYS);
    
    private final int code;
    
    private static final ObjectTransformer<MtfType, Integer> CODE_KEY_GENERATOR = new ObjectTransformer<MtfType, Integer>() {
        
        @Override
        public Integer transform(MtfType value) {
            return value.code;
        }
    };
    
    private static final Map<Integer, MtfType> BY_CODE_MAP;
    
    static {
        BY_CODE_MAP = CollectionUtil.toHashMap(CODE_KEY_GENERATOR, values());
    }
    
    public static MtfType getByCode(int code) {
        return BY_CODE_MAP.get(code);
    }
    
    private MtfType(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
}