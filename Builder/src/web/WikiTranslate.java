package web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import translations.GoogleTranslation;

public class WikiTranslate {

    public static void main(String[] args) throws Exception {

        GenerateFAQ.updateIndex("/home/ivan-dev/git/LirbiReader/docs/wiki/faq", "Frequently asked questions", 2);
        GenerateFAQ.updateIndex("/home/ivan-dev/git/LirbiReader/docs/wiki/stories", "Stories", 2);
        GenerateFAQ.updateIndex("/home/ivan-dev/git/LirbiReader/docs/wiki/manual", "Guide", 2);

        String root = "/home/ivan-dev/git/LirbiReader/docs/wiki";
        // String root = "/home/ivan-dev/git/LirbiReader/docs/_includes";
        syncPaths(root, "ru");
        syncPaths(root, "fr");
        syncPaths(root, "de");
        syncPaths(root, "it");
        syncPaths(root, "pt");
        syncPaths(root, "es");
        syncPaths(root, "zh");
        syncPaths(root, "ar");
    }

    public static void syncPaths(String path, final String ln) throws Exception {

        File root = new File(path);
        File[] listFiles = root.listFiles();
        if (listFiles == null) {
            return;
        }
        for (File file : listFiles) {
            if (file.isFile() && file.getName().equals("index.md")) {
                System.out.println("Find: " + file.getPath());
                translate(file.getParent(), ln);
            } else {
                syncPaths(file.getPath(), ln);
            }
        }

    }

    public static int getVersion(String path) throws Exception {

        BufferedReader input = new BufferedReader(new FileReader(path));
        String line;
        try {
            while ((line = input.readLine()) != null) {
                if (line.startsWith("version:")) {
                    return Integer.parseInt(line.replace("version:", "").trim());
                }
            }
            return 0;
        } finally {
            input.close();
        }

    }

    public static void translate(String root, String ln) throws Exception {
        System.out.println("Tranlate: " + root);

        File index = new File(root, "index.md");
        File ru = new File(root, ln + ".md");

        int inVersion = getVersion(index.getPath());

        int outVersion = -1;
        if (ru.isFile()) {
            outVersion = getVersion(ru.getPath());
        }

        System.out.println("Version in " + inVersion);
        System.out.println("Version ou " + outVersion);

        if ((inVersion == 0 && outVersion != -1) || inVersion == outVersion) {
            System.out.println("[Skip]");
            return;
        }

        List<String> ignoreLines = Arrays.asList("[<]", "|", "{", "<", "!", "---");
        List<String> preLines = Arrays.asList("# ", "## ", "### ", "* ", "> ", "1. ", "2. ", "3. ");

        PrintWriter out = new PrintWriter(ru);

        BufferedReader input = new BufferedReader(new FileReader(index));
        String line;
        int header = 0;

        boolean first = true;
        while ((line = input.readLine()) != null) {

            if (first && !line.equals("---")) {
                header = 3;
                first = false;

            }

            first = false;
            if (line.equals("---")) {
                header++;
            }
            if (header >= 2) {

                boolean isIgnore = false;
                String prefix = "";

                for (String txt : ignoreLines) {
                    if (line.startsWith(txt)) {
                        isIgnore = true;
                        break;
                    }
                }
                for (String txt : preLines) {
                    if (line.startsWith(txt)) {
                        prefix = txt;
                        break;
                    }
                }

                if (!isIgnore && line.trim().length() != 0) {
                    line = line.replace(prefix, "");
                    // adsf **sadf** asdf
                    line = line.replace("**", "__");

                    line = line.replace(" __", " @# ");
                    line = line.replace("__ ", " #@ ");

                    line = line.replaceAll("__$", " @#");
                    line = line.replaceAll("[*]{2}$", " @#");

                    String url = null;
                    if (line.startsWith("[")) {
                        int div = line.indexOf("](");
                        url = line.substring(div + 2, line.length() - 1);
                        line = line.substring(1, div);
                    }

                    if (url != null) {
                        line = GoogleTranslation.translate(line, ln);
                        line = String.format("* [%s](%s)", line, url);
                    } else {
                        line = prefix + GoogleTranslation.translate(line, ln);
                    }

                    line = line.replace("@ # ", "__");
                    line = line.replace(" @ #", "__");
                    line = line.replace(" # @", "__");
                    line = line.replace(" @ @", "__");
                    if (line.contains("[")) {
                        line = line.replace(" /", "/");
                        line = line.replace("/ ", "/");
                        line = line.replace("] (", "](");
                    }

                }
                if (line.endsWith(")")) {
                    line = line.replace("/)", ")").replace(")", "/" + ln + ")");
                }

            }
            out.println(line);

        }
        input.close();
        out.close();
        System.out.println("done");

    }

}
