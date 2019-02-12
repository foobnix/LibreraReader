package at.stefl.svm.enumeration;

import java.util.Map;

import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.object.ObjectTransformer;

public enum GradientStyle {
    
    LINEAR(GradientStyleConstants.GRADIENT_STYLE_LINEAR), AXIAL(
            GradientStyleConstants.GRADIENT_STYLE_AXIAL), RADIAL(
            GradientStyleConstants.GRADIENT_STYLE_RADIAL), ELLIPTICAL(
            GradientStyleConstants.GRADIENT_STYLE_ELLIPTICAL), SQUARE(
            GradientStyleConstants.GRADIENT_STYLE_SQUARE), RECT(
            GradientStyleConstants.GRADIENT_STYLE_RECT), FORCE_EQUAL_SIZE(
            GradientStyleConstants.GRADIENT_STYLE_FORCE_EQUAL_SIZE);
    
    private static final ObjectTransformer<GradientStyle, Integer> CODE_KEY_GENERATOR = new ObjectTransformer<GradientStyle, Integer>() {
        
        @Override
        public Integer transform(GradientStyle value) {
            return value.code;
        }
    };
    
    private static final Map<Integer, GradientStyle> BY_CODE_MAP;
    
    static {
        BY_CODE_MAP = CollectionUtil.toHashMap(CODE_KEY_GENERATOR, values());
    }
    
    public static GradientStyle getGradientStyleByCode(int code) {
        return BY_CODE_MAP.get(code);
    }
    
    private final int code;
    
    private GradientStyle(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
}