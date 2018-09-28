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
        String string = "brightness-control-and-blue-light-filter";
        translate("/home/ivan-dev/git/LirbiReader/docs/wiki/faq/" + string + "/", "ru");
    }

    public static void translate(String root, String ln) throws Exception {
        System.out.println("Tranlate: " + root);

        File index = new File(root, "index.md");
        File ru = new File(root, ln + ".md");

        List<String> ignoreLines = Arrays.asList("[<]", "|", "{", "<");
        List<String> preLines = Arrays.asList("# ", "## ", "### ", "* ", "> ", "1. ", "2. ", "3. ");

        PrintWriter out = new PrintWriter(ru);

        BufferedReader input = new BufferedReader(new FileReader(index));
        String line;
        int skip2 = 0;
        while ((line = input.readLine()) != null) {
            if (skip2 >= 2) {

                boolean isIgnore = false;
                String prefix = "";

                if (line.startsWith("[<]") && !line.endsWith(ln)) {
                    line = line.replace("/)", ")").replace(")", "/" + ln + ")");
                }

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
                    line = line.replace(" **", " @# ");
                    line = line.replace("** ", " #@ ");

                    line = line.replace(" __", " @# ");
                    line = line.replace("__ ", " #@ ");

                    line = line.replaceAll("__$", " @#");
                    line = line.replaceAll("[*]{2}$", " @#");

                    line = prefix + GoogleTranslation.translate(line, ln);

                    line = line.replace("@ # ", "__");
                    line = line.replace(" @ #", "__");
                    line = line.replace(" # @", "__");

                }
            }
            out.println(line);

            if (line.equals("---")) {
                skip2++;
            }

        }
        input.close();
        out.close();
        System.out.println("done");

    }



}
