package web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class GenerateFAQ {

    static String ROOT = "/home/ivan-dev/git/LirbiReader/docs/wiki/faq";

    public static void main(String[] args) throws IOException {

        PrintWriter out = new PrintWriter(new File(ROOT, "index.md"));
        out.println("---");
        out.println("layout: main");
        out.println("---");
        out.println("[<](/wiki/)");
        out.println("");
        out.println("# Frequently asked questions");
        out.println("");

        File list = new File(ROOT);
        for (File file : list.listFiles()) {
            if (file.isDirectory()) {
                File child = new File(file, "index.md");
                String title = getTitle(child).trim();

                String line = String.format("* [%s](/wiki/faq/%s)", title, file.getName());
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
        throw new IllegalArgumentException("Title not found");
    }

}
