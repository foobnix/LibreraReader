package at.stefl.opendocument.java.translator.style.property;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.stefl.opendocument.java.css.StyleAbsoluteUnit;
import at.stefl.opendocument.java.css.StyleProperty;

public class BorderPropertyTranslator implements PropertyTranslator {
    
    private static final double DEFAULT_MM_LIMIT = 0.262;
    
    private static final Map<StyleAbsoluteUnit, Double> DEFAULT_LIMIT_MAP = createLimitMapByMM(DEFAULT_MM_LIMIT);
    
    private static Map<StyleAbsoluteUnit, Double> createLimitMapByMM(
            double mmLimit) {
        Map<StyleAbsoluteUnit, Double> result = new EnumMap<StyleAbsoluteUnit, Double>(
                StyleAbsoluteUnit.class);
        
        for (StyleAbsoluteUnit lengthSpecification : StyleAbsoluteUnit.values()) {
            double conversionFactor = StyleAbsoluteUnit.MM
                    .getConversionFactor(lengthSpecification);
            result.put(lengthSpecification, mmLimit * conversionFactor);
        }
        
        return result;
    }
    
    private static final Pattern SIZE_PATTERN = Pattern
            .compile("(\\d+(\\.\\d+)?)\\s*?(\\w*)");
    
    private final Map<StyleAbsoluteUnit, Double> limitMap;
    
    private BorderPropertyTranslator(Map<StyleAbsoluteUnit, Double> limitMap) {
        this.limitMap = limitMap;
    }
    
    public BorderPropertyTranslator() {
        this(DEFAULT_LIMIT_MAP);
    }
    
    public BorderPropertyTranslator(double mmLimit) {
        this(createLimitMapByMM(mmLimit));
    }
    
    @Override
    public StyleProperty translate(String name, String value) {
        int colonIndex = name.indexOf(':');
        if (colonIndex != -1) name = name.substring(colonIndex + 1);
        
        Matcher matcher = SIZE_PATTERN.matcher(value);
        
        if (matcher.find()) {
            double unitValue = Double.parseDouble(matcher.group(1));
            String symbol = matcher.group(3);
            StyleAbsoluteUnit unit = StyleAbsoluteUnit.getBySymbol(symbol);
            
            // TODO: log unknown symbol
            if (unit != null) {
                double limit = limitMap.get(unit);
                
                if (unitValue < limit) {
                    value = value.substring(0, matcher.start()) + "1px"
                            + value.substring(matcher.end());
                }
            }
        }
        
        return new StyleProperty(name, value);
    }
    
}