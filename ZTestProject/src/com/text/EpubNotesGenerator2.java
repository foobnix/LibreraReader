package com.text;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EpubNotesGenerator2 {

    public static void main(String[] args) throws IOException {
        String path = "/home/ivan-dev/dev/workspace/pdf4/ZTestProject/input/Komarovskiy_Zdorove.epub";
        String path1 = "/home/ivan-dev/dev/workspace/pdf4/ZTestProject/input/outout.epub";
        String path2 = "/home/ivan-dev/dev/workspace/pdf4/ZTestProject/input/outout.epub";

        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(path)));
        in.mark(Integer.MAX_VALUE);
        ZipInputStream zipInputStream = new ZipInputStream(in);

        ZipEntry nextEntry = null;

        Map<String, String> textLink = new HashMap<String, String>();
        Set<String> files = new HashSet<String>();

        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String nameLow = nextEntry.getName().toLowerCase();
            if (nameLow.endsWith("html") || nameLow.endsWith("htm")) {
                System.out.println("- " + nameLow + " -");
                Document parse = Jsoup.parse(zipInputStream, null, "");
                Elements select = parse.select("a[href]");
                for (int i = 0; i < select.size(); i++) {
                    Element item = select.get(i);
                    String text = item.text();
                    if (item.attr("href").contains("#")) {
                        String attr = item.attr("href");
                        String file = attr.substring(0, attr.indexOf("#"));
                        if (file.isEmpty()) {
                            file = nextEntry.getName();
                        }

                        System.out.println(text + " -> " + attr + " [" + file);
                        textLink.put(attr, text);
                        files.add(file);
                        System.out.println("add file" + file + "|");
                    }
                }

            }
        }

        in.reset();
        zipInputStream = new ZipInputStream(in);

        Map<String, String> notes = new HashMap<String, String>();

        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String name = nextEntry.getName();
            for (String fileName : files) {
                if (name.endsWith(fileName)) {
                    System.out.println("file:  " + name);
                    Document parse = Jsoup.parse(zipInputStream, null, "");
                    Elements ids = parse.select("[id]");
                    for (int i = 0; i < ids.size(); i++) {
                        Element item = ids.get(i);
                        String id = item.attr("id");
                        String value = item.text();

                        if (value.trim().length() < 4) {
                            value = value + " " + parse.select("[id=" + id + "]+*").text();
                        }

                        // System.out.println("id:" + id + " value:" + value);
                        String fileKey = fileName + "#" + id;

                        String textKey = textLink.get(fileKey);

                        System.out.println(textKey + " " + value);
                        notes.put(textKey, value);

                    }
                }
            }
        }

    }
}
