package translations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhatsNewUpdateTexts {

    private static final String RECENT_PATH = "/home/ivan-dev/git/LirbiReader/EbookaPDF/assets/whatsnew/";

    public static Map<String, String> codes = new HashMap<String, String>();
    static {
        codes.put("en", "en-US");
        codes.put("de", "de-DE");
        codes.put("es", "es-ES");
        codes.put("fr", "fr-FR");
        codes.put("hi", "hi-IN");
        codes.put("it", "it-IT");
        codes.put("he", "iw-IL");
        codes.put("ja", "ja-JP");
        codes.put("ko", "ko-KR");
        codes.put("pt", "pt-PT");
        codes.put("ru", "ru-RU");
        codes.put("tr", "tr-TR");
        codes.put("zh", "zh-TW");

        codes.put("nl", "nl-NL");
        codes.put("no", "no-NO");
        codes.put("pl", "pl-PL");
        codes.put("sv", "sv-SE");
        codes.put("fi", "fi-FI");

    }

    public static String ln(String code) {
        return codes.get(code) != null ? codes.get(code) : code;
    }

    public static void main(String[] args) throws IOException {
        final List<String> asList = SyncTranslations.getAllLangCodes("/home/ivan-dev/git/LirbiReader/EBookDroid/res");
        // final List<String> asList = Arrays.asList("ru");

        String recentEN = new String(Files.readAllBytes(Paths.get("/home/ivan-dev/git/LirbiReader/EbookaPDF/assets/recent.txt")));

        StringBuilder res = new StringBuilder();
        for (String ex : asList) {
            String outRecnet = RECENT_PATH + ex + ".txt";

            String translation = "";

            String recentTR = GoogleTranslation.translate(recentEN, ex);
            translation = translation.replace("___", "");
            recentTR = recentTR.replace("_ ", "_");
            recentTR = recentTR.replace("_", "\n");
            recentTR = recentTR.replace("*", "\n*");
            recentTR = recentTR.replace("&#39;", "’");
            recentTR = recentTR.replace(" &quot;", "");
            recentTR = recentTR.replace("&quot;", "");
            recentTR = recentTR.replaceAll("^\n", "");
            translation = SyncTranslations.upperCase(recentTR);
            Files.write(Paths.get(outRecnet), recentTR.getBytes());

            res.append("\n<" + ln(ex) + ">\n");
            res.append(translation);
            res.append("\n</" + ln(ex) + ">\n");
        }

        recentEN = recentEN.replace("_", "");
        Files.write(Paths.get(RECENT_PATH + "en" + ".txt"), recentEN.getBytes());

        String ex = "en";
        res.append("\n<" + ln(ex) + ">\n");
        res.append(recentEN);
        res.append("\n</" + ln(ex) + ">\n");

        System.out.println("=======");
        System.out.println(res.toString());
        Files.write(Paths.get(RECENT_PATH + "all" + ".txt"), res.toString().getBytes());
    }

}
