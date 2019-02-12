package helper;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ivan Ivanenko
 * 
 */
public class TextToEnum {
    
    public static void main(String[] args) {
        InputStream resourceAsStream = JsonToModel.class.getResourceAsStream("models.txt");
        String json = JsonToModel.convertStreamToString(resourceAsStream);

        Map<String, String> map = new LinkedHashMap<String, String>();
        String[] split = json.split("\r");
        
        for (int i=1; i<split.length; i+=2) {
            String line = split[i];
            String name = line.split("\t")[0];
            String value = line.split("\t")[1];
            System.out.println(name + "{");
            System.out.println("@Override");
            System.out.println("public int getIntValue(){");
            System.out.println("return "+ value + ";");
            System.out.print("}},//");
        }
    }

}
