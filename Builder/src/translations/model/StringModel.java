package translations.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

/**
 * @author Ivan Ivanenko
 * 
 */
@Root(name = "string")
public class StringModel {

    @Attribute
    private String name;

    @Attribute(required = false)
    private Boolean formatted;

    @Text(empty = "", required = false)
    private String text;

    public StringModel() {

    }

    public StringModel(String name, String description) {
        this.name = name;
        this.text = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getFormatted() {
        return formatted;
    }

    public void setFormatted(Boolean formatted) {
        this.formatted = formatted;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
