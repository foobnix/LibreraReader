package at.stefl.opendocument.java.translator.style;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import at.stefl.commons.io.StreamableStringMap;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.util.array.ArrayUtil;
import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.collection.OrderedPair;
import at.stefl.opendocument.java.css.StyleProperty;
import at.stefl.opendocument.java.translator.style.property.PropertyTranslator;
import at.stefl.opendocument.java.translator.style.property.StaticGeneralPropertyTranslator;

public class DefaultStyleElementTranslator extends
        StyleElementTranslator<DocumentStyle> {
    
    private static final String[] DIRECTION_SUFFIXES = { "", "-top", "-right",
            "-bottom", "-left" };
    
    private static final String NAME_ATTRIBUTE_NAME = "style:name";
    private static final String FAMILY_ATTRIBUTE_NAME = "style:family";
    private static final String PARENT_ATTRIBUTE_NAME = "style:parent-style-name";
    
    private static final Set<String> DEFAULT_PARENT_ATTRIBUTES = ArrayUtil
            .toCollection(new LinkedHashSet<String>(2), FAMILY_ATTRIBUTE_NAME,
                    PARENT_ATTRIBUTE_NAME);
    
    private static String getToAttributeNameByColon(String attribute) {
        int colonIndex = attribute.indexOf(':');
        return (colonIndex == -1) ? attribute : attribute
                .substring(colonIndex + 1);
    }
    
    public DefaultStyleElementTranslator() {
        this(DEFAULT_PARENT_ATTRIBUTES);
    }
    
    public DefaultStyleElementTranslator(Set<String> parentAttributes) {
        this.parentAttributes = new LinkedHashSet<String>(parentAttributes);
    }
    
    private final Set<String> parentAttributes;
    
    private final StreamableStringMap<PropertyTranslator> attributeTranslatorMap = new StreamableStringMap<PropertyTranslator>();
    
    public void addPropertyTranslator(String attribute) {
        addPropertyTranslator(attribute, getToAttributeNameByColon(attribute));
    }
    
    public void addPropertyTranslator(String attribute, String property) {
        addPropertyTranslator(attribute, new StaticGeneralPropertyTranslator(
                property));
    }
    
    public void addPropertyTranslator(String attribute,
            PropertyTranslator translator) {
        if (attribute == null) throw new NullPointerException();
        if (translator == null) throw new NullPointerException();
        
        attributeTranslatorMap.put(attribute, translator);
    }
    
    public void addDirectionPropertyTranslator(String attribute,
            PropertyTranslator translator) {
        for (String directionPrefix : DIRECTION_SUFFIXES) {
            addPropertyTranslator(attribute + directionPrefix, translator);
        }
    }
    
    public void addDirectionPropertyTranslator(String attribute) {
        addDirectionPropertyTranslator(attribute,
                getToAttributeNameByColon(attribute));
    }
    
    public void addDirectionPropertyTranslator(String attribute, String property) {
        for (String directionPrefix : DIRECTION_SUFFIXES) {
            addPropertyTranslator(attribute + directionPrefix, property
                    + directionPrefix);
        }
    }
    
    public void addParentAttribute(String attribute) {
        parentAttributes.add(attribute);
    }
    
    public void removePropertyTranslator(String attribute) {
        attributeTranslatorMap.remove(attribute);
    }
    
    public void removeDirectionPropertyTranslator(String attribute) {
        for (String directionPrefix : DIRECTION_SUFFIXES) {
            removePropertyTranslator(attribute + directionPrefix);
        }
    }
    
    public void removeParentAttribute(String attribute) {
        parentAttributes.remove(attribute);
    }
    
    protected String getStyleName(Map<String, String> attributes) {
        String name = attributes.get(NAME_ATTRIBUTE_NAME);
        String family = attributes.get(FAMILY_ATTRIBUTE_NAME);
        
        if (name == null) return family;
        return name;
    }
    
    protected Set<String> getParentStyles(Map<String, String> attributes) {
        Set<String> result = new HashSet<String>();
        CollectionUtil.getNotNull(attributes, parentAttributes, result);
        return result;
    }
    
    @Override
    public void translate(LWXMLReader in, DocumentStyle out) throws IOException {
        // TODO: optimize: parse only needed attributes
        Map<String, String> attributes = LWXMLUtil.parseAllAttributes(in);
        
        String name = getStyleName(attributes);
        if (name == null) return;
        Set<String> parents = getParentStyles(attributes);
        out.addStyleInheritance(name, parents);
        
        out.writeClass(name);
        
        loop:
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                OrderedPair<String, PropertyTranslator> match = attributeTranslatorMap
                        .match(in);
                
                if (match != null) {
                    String attributeName = match.getElement1();
                    PropertyTranslator translator = match.getElement2();
                    
                    String attributeValue = in.readFollowingValue();
                    StyleProperty property = translator.translate(
                            attributeName, attributeValue);
                    if (property == null) break;
                    out.writeProperty(property);
                }
                
                break;
            case END_DOCUMENT:
                break loop;
            default:
                break;
            }
        }
    }
    
}