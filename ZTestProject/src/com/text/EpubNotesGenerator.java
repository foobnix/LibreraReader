package com.text;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EpubNotesGenerator {

    public static void main(String[] args) throws IOException {
        String path = "/home/ivan-dev/dev/workspace/pdf4/ZTestProject/input/Komarovskiy_Zdorove.epub";

        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(path)));
        in.mark(Integer.MAX_VALUE);
        ZipInputStream zipInputStream = new ZipInputStream(in);

        ZipEntry nextEntry = null;

        Map<String, String> idName = new HashMap<String, String>();
        Map<String, String> idValue = new HashMap<String, String>();

        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String name = nextEntry.getName().toLowerCase();
            if (name.endsWith("html") || name.endsWith("htm")) {
                System.out.println("===" + name);
                Document parse = Jsoup.parse(zipInputStream, null, "");
                // <a href="contentnotes0.html#n1" id="back_n1">
                Elements select = parse.select("a[href]");
                for (int i = 0; i < select.size(); i++) {
                    Element item = select.get(i);
                    String text = item.text();
                    if (item.attr("href").contains("#") && ((text.contains("[") && text.contains("]")) || text.contains("{") && text.contains("}"))) {
                        String attr = item.attr("href");
                        attr = attr.substring(attr.indexOf('#') + 1);
                        System.out.println(attr + "->" + text);
                        idName.put(attr, text);
                    }
                }

                Elements ids = parse.select("a[id],span[id],div[id]");
                for (int i = 0; i < ids.size(); i++) {
                    Element item = ids.get(i);
                    String id = item.attr("id");
                    String value = item.text();
                    System.out.println("id:" + id + " value:" + value);
                    idValue.put(id, value);
                }

            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<notes>");
        for (String id : idName.keySet()) {
            String value = idName.get(id);
            String line = String.format("<item id=\"%s\">%s</item>", value, idValue.get(id));
            builder.append(line);
            if (line.length() > 101) {
                System.out.println(line.substring(0, 100));
            }
        }
        builder.append("</notes>");

    }
}
