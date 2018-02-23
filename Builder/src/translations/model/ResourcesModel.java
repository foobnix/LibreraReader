package translations.model;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * @author Ivan Ivanenko
 * 
 */
@Root(name="resources")
public class ResourcesModel {
    
    @ElementList(inline = true, required = false)
    private List<StringArray> stringArrays = new ArrayList<>();
    
    @ElementList(name="string", inline=true)
    private List<StringModel> strings = new ArrayList<StringModel>();
    
    public List<StringModel> getStrings() {
        return strings;
    }

    public void setStrings(final List<StringModel> strings) {
        this.strings = strings;
    }

    public List<StringArray> getStringArrays() {
        return stringArrays;
    }

    public void setStringArrays(final List<StringArray> stringArrays) {
        this.stringArrays = stringArrays;
    }
    
    

}
