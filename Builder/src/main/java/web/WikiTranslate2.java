package web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import translations.GoogleTranslation;

public class WikiTranslate2 {


    public static void main(String[] args) throws Exception {


        String root = "/home/data/git/LibreraReader/docs/wiki";
        syncPaths(root, "ru");


    }

    public static String traslateMD(String in, String ln) throws IOException {
        if (in.trim().length() == 0) {
            return in;
        }

        in = in.replace("__", "**");

        List<String> ignoreLines = Arrays.asList("[<]", "|", "{", "<", "!", "---", "# 7.", "# 8.");

        for (String pr : ignoreLines) {
            if (in.startsWith(pr)) {
                return in;
            }
        }

        Map<String, String> map = new HashMap<>();
        map.put("# ", "{1} ");
        map.put("** ", "{2} ");
        map.put(" **", " {3}");
        map.put("* ", "{4}");
        map.put("**", "{6}");
        map.put("&nbsp;", "{7}");

        Map<String, String> reverse = new LinkedHashMap<>();
        reverse.put("{1} ", "# ");

        reverse.put(" {2}", "**");
        reverse.put("{3} ", "**");

        reverse.put(" {6}", "**");
        reverse.put("{6} ", "**");

        reverse.put("{4} ", "* ");

        reverse.put("{7}", "&nbsp;");

        reverse.put("] (", "](");
        reverse.put("&#39;", "'");
        reverse.put(" / ", "/");
        reverse.put("(/ ", "(/");
        reverse.put(" /)", "/)");

        // System.out.println(in);
        for (String key : map.keySet()) {
            in = in.replace(key, map.get(key));
        }
        // System.out.println(in);

        if (in.contains("](/")) {// ulr
            int index = in.indexOf("](/");
            String url = in.substring(index + 1, in.indexOf(")") + 1);

            url = url.replace("/)", ")").replace(")", "/" + ln + ")");

            System.out.println("url:" + url);
            reverse.put("   {5}", url);
            reverse.put(" {5}", url);
            in = in.substring(0, index + 1) + "{5}" + in.substring(in.indexOf(")", index) + 1);
        }

        String line = GoogleTranslation.translate(in, "ru", ln);
        line=line.replace("（","(").replace("）",")");

        for (String key : reverse.keySet()) {
            line = line.replace(key, reverse.get(key));
        }
        System.out.println(line);
        return line;
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
                if (line.startsWith("version :")) {
                    throw new IllegalArgumentException(path);
                }
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
        // System.out.println("Tranlate: " + root);

        File index = new File(root, "source-ru.md");
        File ru = new File(root, ln + ".md");

        int inVersion = getVersion(index.getPath());

        int outVersion = -1;
        if (ru.isFile()) {
            outVersion = getVersion(ru.getPath());
        }

        // System.out.println("Version in " + inVersion);
        // System.out.println("Version ou " + outVersion);

        if ((inVersion == 0 && outVersion != -1) || inVersion == outVersion) {
            // System.out.println("[Skip]");
            return;
        }

        System.out.println("Tranlate: " + root);
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

                line = traslateMD(line, ln);

            }
            out.println(line);

        }
        input.close();
        out.close();
        System.out.println("done");

    }

}
