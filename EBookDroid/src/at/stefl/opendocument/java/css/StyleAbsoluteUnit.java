package at.stefl.opendocument.java.css;

import java.util.Map;

import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.object.ObjectTransformer;

public enum StyleAbsoluteUnit {
    
    IN("in", 0.0254), CM("cm", 0.01), MM("mm", 0.001), PT("pt", 0.000352778),
    PC("pc", 0.004233336);
    
    private static final ObjectTransformer<StyleAbsoluteUnit, String> SYMBOL_KEY_GENERATOR = new ObjectTransformer<StyleAbsoluteUnit, String>() {
        
        @Override
        public String transform(StyleAbsoluteUnit value) {
            return value.symbol;
        }
    };
    
    private static final Map<String, StyleAbsoluteUnit> BY_SYMBOL_MAP;
    
    static {
        BY_SYMBOL_MAP = CollectionUtil
                .toHashMap(SYMBOL_KEY_GENERATOR, values());
    }
    
    public static StyleAbsoluteUnit getBySymbol(String unit) {
        return BY_SYMBOL_MAP.get(unit.toLowerCase());
    }
    
    public static double getConversionFactor(StyleAbsoluteUnit from,
            StyleAbsoluteUnit to) {
        return from.getConversionFactor(to);
    }
    
    private final String symbol;
    private final double meterFactor;
    
    private StyleAbsoluteUnit(String symbol, double meterFactor) {
        this.symbol = symbol;
        this.meterFactor = meterFactor;
    }
    
    @Override
    public String toString() {
        return symbol;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public double getMeterFactor() {
        return meterFactor;
    }
    
    public double getConversionFactor(StyleAbsoluteUnit to) {
        return meterFactor / to.meterFactor;
    }
    
}