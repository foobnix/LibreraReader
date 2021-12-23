package web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GenerateFAQ {

    public static void main(String[] args) throws Exception {
        // updateIndex("/home/data/git/LirbiReader/docs/wiki/faq", "Frequently asked
        // questions1", 1);
        // updateIndex("/home/data/git/LirbiReader/docs/wiki/stories", "Stories",
        // 1);

        WikiTranslate.translate("/home/data/git/LirbiReader/docs/wiki/faq", "it");
    }

    public static void updateIndex(final String in, String pageTitle) throws Exception {
        File outFile = new File(in, "index.md");
        final PrintWriter out = new PrintWriter(outFile);
        out.println("---");
        out.println("layout: main");
        out.println("info: this file is generated automatically, please do not modify it");
        out.println("---");
        out.println("");
        out.println("# " + pageTitle);
        out.println("");

        File list = new File(in);
        List<File> files = Arrays.asList(list.listFiles());
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                return Long.compare(t1.lastModified(), file.lastModified());
            }
        });

        for (File file : files) {
            if (file.isDirectory()) {
                File child = new File(file, "index.md");
                String title = getTitle(child).trim();

                String line = String.format("* [%s](/wiki/%s/%s)", title, outFile.getParentFile().getName(), file.getName());
                System.out.println(line);
                out.println(line);
            }
        }
        out.close();
    }

    public static String getTitle(File file) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(file));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("#")) {
                    String result = line.replace("#", "");
                    input.close();
                    return result;
                }
            }
            input.close();
        } catch (Exception e) {
        }
        throw new IllegalArgumentException("Title not found: " + file.getPath());
    }

}
