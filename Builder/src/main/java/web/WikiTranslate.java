package web;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import translations.GoogleTranslation;

public class WikiTranslate {

    static JSONObject cache;

    public static void main(String[] args) throws Exception {

        int version = 29;
        GenerateFAQ.updateIndex("/home/ivan-dev/git/LibreraReader/docs/wiki/faq", "Frequently asked questions", version);

        String root = "/home/ivan-dev/git/LibreraReader/docs/wiki";


        File file = new File("/home/ivan-dev/git/LibreraReader/Builder/cache.json");
        cache = new JSONObject(readString(file));

        try {
            syncPaths(root, "ru");
            syncPaths(root, "fr");
            syncPaths(root, "de");
            syncPaths(root, "it");
            syncPaths(root, "pt");
            syncPaths(root, "es");
            syncPaths(root, "zh");
            syncPaths(root, "ar");
        } finally {
            writeString(file, cache.toString());
        }


    }

    public static String readString(File file) {

        try {
            if (!file.exists()) {
                return "{}";
            }
            StringBuilder builder = new StringBuilder();
            String aux = "";
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((aux = reader.readLine()) != null) {
                builder.append(aux);
            }
            reader.close();
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";

    }

    public static boolean writeString(File file, String string) {

        try {
            if (string == null) {
                string = "";
            }
            new File(file.getParent()).mkdirs();

            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(string.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String traslateMD(String in, String ln) throws IOException {
        if (in.startsWith("[<](/wiki/)")) {
            return "[<](/wiki/" + ln + ")";
        }
        String key = in + ln;
        if (cache.has(key)) {
            return cache.getString(key);
        }
        String res = traslateMDInner(in, ln);
        cache.put(key, res);
        return res;
    }

    private static String traslateMDInner(String in, String ln) throws IOException {
        if (in.trim().length() == 0) {
            return in;
        }


        in = in.replace("__", "**");

        List<String> ignoreLines = Arrays.asList("[<]", "|", "{", "<", "!", "---", "# 7.", "# 8.");

        boolean findCode = false;
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
        map.put("###", "{8}");
        map.put("##", "{9}");

        Map<String, String> reverse = new LinkedHashMap<>();
        reverse.put("{1} ", "# ");

        reverse.put(" {2}", "**");
        reverse.put("{3} ", "**");

        reverse.put(" {6}", "**");
        reverse.put("{6} ", "**");

        reverse.put("{4} ", "* ");

        reverse.put("{7}", "&nbsp;");
        reverse.put("{8}", "###");
        reverse.put("{9}", "##");

        reverse.put("] (", "](");
        reverse.put("&#39;", "'");
        reverse.put(" / ", "/");
        reverse.put("(/ ", "(/");
        reverse.put(" /)", "/)");

        // System.out.println(in);
        for (String key : map.keySet()) {
            in = in.replace(key, map.get(key));
        }

        //System.out.println(in);

        if (in.contains("](/")) {// ulr
            int index = in.indexOf("](/");
            String url = in.substring(index + 1, in.lastIndexOf(")") + 1);

            url = url.replace("/)", ")").replace(")", "/" + ln + ")");

            System.out.println("url:" + url);
            reverse.put("   {5}", url);
            reverse.put(" {5}", url);
            in = in.substring(0, index + 1) + "{5}" + in.substring(in.indexOf(")", index) + 1);
        }

        String line = GoogleTranslation.translate(in, ln);
        line = line.replace("（", "(").replace("）", ")");
        if (line.startsWith("&gt;")) {
            line = line.replace("&gt;", ">");
        }
        line = line.replace("]]", "]");


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

        File index = new File(root, "index.md");
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
        boolean findCode = false;
        while ((line = input.readLine()) != null) {

            if (line.trim().equals("```")) {
                findCode = !findCode;
                if (findCode == false) {
                    out.println(line);
                    continue;
                }
            }

            if (findCode) {
                out.println(line);
                continue;
            }

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
