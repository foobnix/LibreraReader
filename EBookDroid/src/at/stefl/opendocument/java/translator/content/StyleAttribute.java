package at.stefl.opendocument.java.translator.content;

public enum StyleAttribute {
    
    TEXT("text:style-name"), TABLE("table:style-name"), PRESENTATION(
            "presentation:style-name"), DRAW("draw:style-name"), DRAW_TEXT(
            "draw:text-style-name"), MASTER_PAGE("draw:master-page-name");
    
    private final String name;
    
    private StyleAttribute(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public String getName() {
        return name;
    }
    
}