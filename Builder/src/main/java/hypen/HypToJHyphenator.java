package hypen;

import java.nio.file.*;
import java.util.*;

public class HypToJHyphenator {

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(
                Path.of("/Users/ivanivanenko/git/LibreraReader/Builder/src/main/java/hypen/hyph_sr.dic"),
                java.nio.charset.StandardCharsets.UTF_8
        );

        Map<Integer, StringBuilder> map = new HashMap<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("LEFTHYPHENMIN")) continue;
            if (line.startsWith("RIGHTHYPHENMIN")) continue;

            int len = line.length();

            map.computeIfAbsent(len, k -> new StringBuilder())
               .append(line);
        }

        System.out.println("HashMap<Integer, String> patterns = new HashMap<>();");
        for (var e : map.entrySet()) {
            System.out.println(
                "put(" + e.getKey() + ", \"" +
                e.getValue().toString() + "\");"
            );
        }
    }
}
