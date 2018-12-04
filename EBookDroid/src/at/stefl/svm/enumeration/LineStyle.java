package at.stefl.svm.enumeration;

import java.util.Map;

import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.object.ObjectTransformer;

public enum LineStyle {
    
    NONE(LineStyleConstants.LINE_NONE), SOLID(LineStyleConstants.LINE_SOLID),
    DASH(LineStyleConstants.LINE_DASH), FORCE_EQUAL_SIZE(
            LineStyleConstants.LINE_FORCE_EQUAL_SIZE);
    
    private static final ObjectTransformer<LineStyle, Integer> CODE_KEY_GENERATOR = new ObjectTransformer<LineStyle, Integer>() {
        
        @Override
        public Integer transform(LineStyle value) {
            return value.code;
        }
    };
    
    private static final Map<Integer, LineStyle> BY_CODE_MAP;
    
    static {
        BY_CODE_MAP = CollectionUtil.toHashMap(CODE_KEY_GENERATOR, values());
    }
    
    public static LineStyle getByCode(int code) {
        return BY_CODE_MAP.get(code);
    }
    
    private final int code;
    
    private LineStyle(int code) {
        this.code = code;
    }
    
}