package translations.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * @author Ivan Ivanenko
 * 
 */
@Root(name="string-array")
public class StringArray {
    
    @Attribute
    private String name;
    
    @ElementList(inline=true, entry="item")
    private List<String> item;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getItem() {
        return item;
    }

    public void setItem(List<String> item) {
        this.item = item;
    }
    
    
    

}
