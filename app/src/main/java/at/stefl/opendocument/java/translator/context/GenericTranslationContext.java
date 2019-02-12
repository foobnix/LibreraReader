package at.stefl.opendocument.java.translator.context;

import at.stefl.opendocument.java.odf.OpenDocument;
import at.stefl.opendocument.java.translator.settings.TranslationSettings;
import at.stefl.opendocument.java.translator.style.DocumentStyle;

public abstract class GenericTranslationContext<D extends OpenDocument, S extends DocumentStyle>
        extends TranslationContext {
    
    private final Class<D> documentClass;
    private final Class<S> styleClass;
    
    private D document;
    private S style;
    private TranslationSettings settings;
    
    public GenericTranslationContext(Class<D> documentClass, Class<S> styleClass) {
        this.documentClass = documentClass;
        this.styleClass = styleClass;
    }
    
    @Override
    public D getDocument() {
        return document;
    }
    
    @Override
    public S getStyle() {
        return style;
    }
    
    @Override
    public TranslationSettings getSettings() {
        return settings;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void setDocument(OpenDocument document) {
        if (document == null) throw new NullPointerException();
        if (!documentClass.isAssignableFrom(document.getClass())) throw new IllegalArgumentException();
        this.document = (D) document;
        super.setDocument(document);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void setStyle(DocumentStyle style) {
        if (style == null) throw new NullPointerException();
        if (!styleClass.isAssignableFrom(style.getClass())) throw new IllegalArgumentException();
        this.style = (S) style;
    }
    
    @Override
    public void setSettings(TranslationSettings settings) {
        this.settings = new TranslationSettings(settings);
    }
    
}