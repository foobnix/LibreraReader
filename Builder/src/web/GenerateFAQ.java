package web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class GenerateFAQ {

    static String ROOT = "/home/ivan-dev/git/LirbiReader/docs/wiki/faq";

    public static void main(String[] args) throws Exception {

        updateIndex("index.md", "Frequently asked questions");
        updateIndex("ru.md", "Часто задаваемые вопросы");
    }

    private static void updateIndex(String in, String pageTitle) throws Exception {
        PrintWriter out = new PrintWriter(new File(ROOT, in));
        out.println("---");
        out.println("layout: main");
        out.println("---");
        out.println("[<](/wiki/)");
        out.println("");
        out.println("# " + pageTitle);
        out.println("");

        File list = new File(ROOT);
        for (File file : list.listFiles()) {
            if (file.isDirectory()) {
                File child = new File(file, in);
                String title = getTitle(child).trim();

                String line = String.format("* [%s](/wiki/faq/%s/%s)", title, file.getName(), in.replace(".md", ""));
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
