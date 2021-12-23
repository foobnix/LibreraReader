package hypen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class HypenGenerator {

    public static void main(String[] args) throws IOException {
        String path = "/home/data/git/Hyphenator/patterns/";
        StringBuilder out = new StringBuilder();

        for (String name : new File(path).list()) {
            File child = new File(path, name);
            // System.out.println(child);

            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(child)));
            


            String line;
            String language = null;
            String leftmin = null;
            String rightmin = null;

            boolean isBeginPatterns = false;
            while ((line = input.readLine()) != null) {
                if (line.contains("languages[")) {
                    int indexOf = line.indexOf("'");
                    language = line.substring(indexOf + 1, line.indexOf("'", indexOf + 1)).replace("-", "_");
                    // System.out.println("Lang: " + language);
                    continue;
                }
                if (line.contains("leftmin")) {
                    leftmin = line.replace("leftmin", "").replace(":", "").replace(",", "").trim();
                    // System.out.println("leftmin: " + leftmin);
                    continue;
                }
                if (line.contains("rightmin")) {
                    rightmin = line.replace("rightmin", "").replace(":", "").replace(",", "").trim();
                    // System.out.println("rightmin: " + rightmin);
                    out.append(String.format("%s(\"%s\",%s,%s, new HashMap<Integer, String>() {", language, language, leftmin, rightmin));
                    out.append("\n");
                    out.append(" {");
                    out.append("\n");
                    continue;
                }
                if (line.contains("patterns:") || line.contains("patterns :")) {
                    isBeginPatterns = true;
                    continue;
                }
                if (line.contains("patternChars")) {
                    isBeginPatterns = false;
                    break;
                }

                if (isBeginPatterns) {
                    if (line.contains("},")) {
                        out.append(" }");
                        out.append("\n");
                        out.append("}),");
                        out.append("\n");
                        break;
                    }
                    String[] split = line.split(": ");
                    String num = split[0].trim();
                    String pattern = split[1].replace(",", "").trim();
                    pattern = pattern.replace("unescape(", "").replace(")", "");
                    out.append(String.format("put(%s, %s);", num, pattern));
                    out.append("\n");
                }

            }
            input.close();
            

        }

        FileWriter fw = new FileWriter(new File("/home/data/git/LirbiReader/Builder/src/hypen/out.txt"));
        fw.write(out.toString());
        fw.close();
        System.out.println("done");

    }
}
