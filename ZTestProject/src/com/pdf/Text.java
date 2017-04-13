package com.pdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class Text {

    public static void main(String[] args) throws Exception {

        // String ROOT =
        // "/mount/extHDD/help/Dropbox/Projects/BookTestingDB/PDF/";
        String ROOT = "/mount/extHDD/help/Книги/PDFs";
        File[] listFiles = new File(ROOT).listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".pdf");
            }
        });

        for (File file : listFiles) {
            System.out.println("BOOK: " + file.getName());
            extractTitleAuthor(file.getPath());
        }

    }

    private static void extractTitleAuthor(String path) throws Exception {
        System.out.println("PATH: " + path);
        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        String line;
        String author = null;
        String title = null;

        boolean findTitle = false;
        boolean findAuthor = false;
        while ((line = input.readLine()) != null) {

            /* Second type */
            if (findAuthor && line.contains("rdf:li")) {
                findAuthor = false;
                author = line.replace("</rdf:li>", "");
                author = author.substring(author.indexOf(">") + 1);
                continue;
            }

            if (findTitle && line.contains("rdf:li")) {
                findTitle = false;
                title = line.replace("</rdf:li>", "");
                title = title.substring(title.indexOf(">") + 1);
                continue;
            }

            if (title == null && line.contains("<dc:title>")) {
                findTitle = true;
                continue;
            }

            if (author == null && line.contains("<dc:creator>")) {
                findAuthor = true;
                continue;
            }
            if (author != null && title != null) {
                break;
            }

        }
        input.close();

        if (title != null && title.contains("�")) {
            title = "";
        }

        if (author != null && author.contains("�")) {
            author = "";
        }

        System.out.println("Author:" + author);
        System.out.println("Title:" + title);
        System.out.println("");
        System.out.println("");
    }

    public static String getValue(String string, String key) {
        try {
            String keyFull = "/" + key + "(";
            int indexOf = string.indexOf(keyFull);
            if (indexOf >= 0) {
                int beginData = indexOf + keyFull.length();
                int lastIndex = string.indexOf(")", beginData);
                return string.substring(beginData, lastIndex);
            }
        } catch (Exception e) {
        }
        return "";
    }

}
