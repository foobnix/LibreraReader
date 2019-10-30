package test;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.System.out;

public class Test1 {
    public static void main(String[] args) {
        Map<String , String> list = new LinkedHashMap<>();
        list.put("a1","-a1");
        list.put("b1","-b1");
        list.put("a2","-a2");
        list.put("b2","-b2");
        list.put("a3","-a3");
        list.put("b3","-b3");

        out.println(list.keySet().getClass());
        for(String key:list.keySet()){
            out.println("key:"+key + " value:" + list.get(key));
        }

    }
}
