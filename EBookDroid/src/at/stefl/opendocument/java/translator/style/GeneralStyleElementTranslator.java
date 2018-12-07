package at.stefl.opendocument.java.translator.style;

import com.foobnix.android.utils.LOG;

import at.stefl.opendocument.java.css.StyleProperty;
import at.stefl.opendocument.java.translator.style.property.PropertyTranslator;

public class GeneralStyleElementTranslator extends DefaultStyleElementTranslator {

    static class MyEm implements PropertyTranslator {

        @Override
        public StyleProperty translate(String name, String value) {
            try {
                if (value.contains("pt")) {
                    LOG.d("StyleProperty before", "font-size", value);
                    float f = Float.parseFloat(value.replace("pt", ""));
                    float em = Math.min(Math.max(1, f / 12), 1.2f);
                    String emValue = em + "em";
                    LOG.d("StyleProperty after", "font-size", emValue);
                    return new StyleProperty("font-size", "1em");
                }
            } catch (Exception e) {
                LOG.e(e);
            }

            return new StyleProperty("font-size", value);
        }

    }

    public GeneralStyleElementTranslator() {
        // addPropertyTranslator("fo:text-align");
        // addDirectionPropertyTranslator("fo:margin");
        // addDirectionPropertyTranslator("fo:padding");
        // addDirectionPropertyTranslator("fo:border");
        // addPropertyTranslator("style:column-width", "width");
        // addPropertyTranslator("style:row-height", "height");
        // addDirectionPropertyTranslator("fo:border", new BorderPropertyTranslator());
        // addPropertyTranslator("fo:margin", new MarginPropertyTranslator());

        // addPropertyTranslator("fo:font-size", new MyEm());
        addPropertyTranslator("style:font-name", "font-family");
        addPropertyTranslator("fo:font-weight");
        addPropertyTranslator("fo:font-style");
        // addPropertyTranslator("fo:font-size");
        // addPropertyTranslator("fo:text-shadow");
        addPropertyTranslator("fo:color");
        // addPropertyTranslator("fo:background-color");
        // addPropertyTranslator("style:vertical-align");
        // addPropertyTranslator("style:text-underline-style", new
        // UnderlinePropertyTranslator());
        // addPropertyTranslator("style:text-line-through-style", new
        // LineThroughPropertyTranslator());
        // addPropertyTranslator("style:text-position", new
        // VerticalAlignPropertyTranslator());

        // addPropertyTranslator("style:width");
        // addPropertyTranslator("style:height");

        // odp
        // addPropertyTranslator("draw:fill-color", "background-color");

        // svg translation
        // addPropertyTranslator("draw:fill-color", "fill");
        // addPropertyTranslator("svg:stroke-color", "stroke");

        // addPropertyTranslator("fo:page-width", "width");
        // addPropertyTranslator("fo:page-height", "height");
    }

}