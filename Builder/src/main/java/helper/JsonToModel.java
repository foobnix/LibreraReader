package helper;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Ivan Ivanenko
 * 
 */
public class JsonToModel {

    public static void main(String[] args) throws JSONException {
        InputStream resourceAsStream = JsonToModel.class.getResourceAsStream("models.txt");
        String json = convertStreamToString(resourceAsStream).trim().replace("\n", "").replace("\r", "");
        // System.out.println(json);

        JSONObject o = new JSONObject(json);

        print(o,"ROOT");

    }

    public static void print(JSONObject obj, String root) {
        JSONArray names = obj.names();
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i < names.length(); i++) {
            Object name = names.get(i);
            Object object = obj.get("" + name);

            // System.out.println(name);
            // System.out.println(object.getClass());
            if (object instanceof JSONObject) {
                print((JSONObject) object, name.toString());
                // System.out.println("============");
            }
            if (object instanceof JSONArray) {
                JSONArray arr = (JSONArray) object;

                if (arr != null && arr.length() >= 1 && arr.get(0) instanceof JSONObject) {
                    JSONObject jsonObject = arr.getJSONObject(0);
                    // System.out.println("============");
                    print(jsonObject,name.toString());
                    // System.out.println("============");
                }

            }
            if (object.toString().equals("null")) {
                map.put(name.toString(), "String");
            } else if (object instanceof String) {
                if (name.toString().contains("Date")) {
                    map.put(name.toString(), "DateTime");
                } else {
                    map.put(name.toString(), "String");
                }
            } else if (object instanceof Integer) {
                // printMethod("String", name.toString());
                map.put(name.toString(), "Integer");
            } else if (object instanceof Double) {
                // printMethod("Double", name.toString());
                map.put(name.toString(), "Double");
            } else if (object instanceof Boolean) {
                // printMethod("Double", name.toString());
                map.put(name.toString(), "Boolean");
            } else {
                if (name.toString().contains("Ids")) {
                    map.put(name.toString(), "List<Integer>");
                } else if (name.toString().contains("Id")) {
                    map.put(name.toString(), "Integer");
                } else {
                    map.put(name.toString(), "[MODEL]");
                }
            }
        }
        // printResultsGet(map);
        JsonToModel.printResultsGet(map, root);
        JsonToModel.printResultsPut(map);

    }

    public static void printResultsPut(Map<String, String> map) {
        System.out.println("@Override");
        System.out.println("public JSONObject toJson() throws JSONException {");
        System.out.println("JSONObject obj = new JSONObject();");

        for (String k : map.keySet()) {
            String name = k;
            String type = map.get(k);
            if (type.equals("Integer")) {
                type = "Int";
            }
            if (type.equals("DateTime")) {
                System.out.println(String.format("obj.put(\"%s\", %s.toString());", name, getJavaName(name)));
            } else if (type.equals("List<Integer>")) {
                System.out.println(String.format("obj.put(\"%s\",JsonUtils.asJson(%s));", name, getJavaName(name)));
            } else {
                System.out.println(String.format("obj.put(\"%s\",%s);", name, getJavaName(name)));
            }
        }
        System.out.println("return obj;");
        System.out.println("}");
        System.out.println("}");

        System.out.println();
        System.out.println("=============");
        System.out.println();
    }

    public static void printResultsGet(Map<String, String> map, String  root) {
        System.out.println(String.format("public class %s {", root));
        for (String k : map.keySet()) {
            String name = k;
            String type = map.get(k);
            if (type.equals("Double")) {
                type = "double";
            }

            if (type.equals("Integer")) {
                type = "int";
            }
            if (type.equals("Boolean")) {
                type = "boolean";
            }

            printMethod(type, name.toString());
        }
        
        //System.out.println("==== "+root +" ====");
        System.out.println("");
        System.out.println(String.format("  public %s(JSONObject obj) throws JSONException {", root));
        System.out.println("  if(obj==null){return;}");

        for (String k : map.keySet()) {
            String name = k;
            String type = map.get(k);
            if (type.equals("Integer")) {
                type = "Int";
            }

            if (type.equals("[MODEL]")) {
                System.out.println(String.format("this.%s = new %s(obj.optJSONObject(\"%s\"));", getJavaName(name), name, name));
            } else if (type.equals("DateTime")) {
                System.out.println(String.format("this.%s = DateTime.valueOf(obj.getString(\"%s\"));", getJavaName(name), name));
            } else if (type.equals("List<Integer>")) {
                System.out.println(String.format("this.%s = JsonUtils.asList(obj.getJSONArray(\"%s\"));", getJavaName(name), name));
            } else {
                System.out.println(String.format("this.%s = obj.get%s(\"%s\");", getJavaName(name), type, name));
            }
        }
        System.out.println("}");

        //System.out.println("============= " + root);
    }

    private static void printMethod(String type, String name) {
        if (type.equals("[MODEL]")) {
            System.out.println(String.format("private %s %s;", name, getJavaName(name)));
        } else {
            System.out.println(String.format("private %s %s;", type, getJavaName(name)));
        }
    }

    private static String getJavaName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
