package helper;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Ivanenko
 * 
 */
public class TextToModel {

    public static void main(String[] args) {
        InputStream resourceAsStream = JsonToModel.class.getResourceAsStream("models.txt");
        String json = JsonToModel.convertStreamToString(resourceAsStream);

        Map<String, String> map = new LinkedHashMap<String, String>();
        String[] split = json.split("\r");

        for (String line : split) {
            if (line.contains("Public property")) {
                line = line.replace("Public property", "").trim();
                String type = "String";
                if(line.contains("Ids")  || line.endsWith("es")){
                    type = "List<Integer>";
                }else if (contains(line,Arrays.asList("Size","Id","Number","Quantity","Count","Duration"))) {
                    type = "Integer";
                }else if(line.contains("Is") || line.contains("Ascending")){
                    type = "Boolean";
                }else if(line.contains("Date")){
                    type = "DateTime";
                }else if(line.contains("Price") || line.contains("Increment") || line.contains("Cost")){
                    type = "Double";
                }
                       
                
                
                map.put(line, type);

            }
        }
        JsonToModel.printResultsGet(map,"Model");
        JsonToModel.printResultsPut(map);
    }
    private static boolean contains(String text, List<String> values){
        for(String value:values){
            if(text.contains(value)){
                return true;
            }
        }
        return false;
        
    }

}
