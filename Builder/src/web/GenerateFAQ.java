package web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class GenerateFAQ {

    public static void main(String[] args) throws Exception {
        // updateIndex("/home/ivan-dev/git/LirbiReader/docs/wiki/faq", "Frequently asked
        // questions1", 1);
        // updateIndex("/home/ivan-dev/git/LirbiReader/docs/wiki/stories", "Stories",
        // 1);

        WikiTranslate.translate("/home/ivan-dev/git/LirbiReader/docs/wiki/faq", "it");
    }

    public static void updateIndex(final String in, String pageTitle, int version) throws Exception {
        File outFile = new File(in, "index.md");
        final PrintWriter out = new PrintWriter(outFile);
        out.println("---");
        out.println("layout: main");
        out.println("version: " + version);
        out.println("---");
        out.println("[<](/wiki/)");
        out.println("");
        out.println("# " + pageTitle);
        out.println("");

        File list = new File(in);
        for (File file : list.listFiles()) {
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
