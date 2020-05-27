package translations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class WhatsNewUpdateTexts {

    private static final String RECENT_PATH = "/home/data/git/LibreraReader/Builder/whatsnew/";
    // https://console.cloud.google.com/apis/credentials/key/0?project=seismic-bucksaw-120809

    public static Map<String, String> codes = new HashMap<String, String>();

    static {
        codes.put("en", "en-US");
        codes.put("de", "de-DE");
        codes.put("es", "es-ES");
       // codes.put("fr", "fr-FR,fr-CA");
        codes.put("fr", "fr-FR");
        codes.put("hi", "hi-IN");
        codes.put("it", "it-IT");
        codes.put("he", "iw-IL");
        codes.put("ja", "ja-JP");
        codes.put("ko", "ko-KR");
        //codes.put("pt", "pt-PT,pt-BR");
        codes.put("pt", "pt-PT");

        codes.put("tr", "tr-TR");
        codes.put("zh-rTW", "zh-TW");
        codes.put("zh-rCH", "zh-CH");

        codes.put("nl", "nl-NL");
        codes.put("no", "no-NO");
        codes.put("pl", "pl-PL");
        codes.put("sv", "sv-SE");
        codes.put("fi", "fi-FI");
        codes.put("hu", "hu-HU");

        codes.put("ru", "ru-RU");

    }

    public static String[] ln(String code) {
        return codes.get(code) != null ? codes.get(code).split(",") : new String[]{code};
    }

    public static void main(String[] args) throws IOException {
        //final List<String> asList = SyncTranslations.getAllLangCodes("/home/data/git/LibreraReader/app/src/main/res");
        final String[] asList = "ar, de-DE, fa, fr-FR, it-IT, ja-JP, ko-KR, pt-PT, ru-RU, th, tr-TR, vi, zh-CN, zh-TW".split(",");
        // final List<String> asList = Arrays.asList("zh-rCN", "zh-rTW");

        String recentEN = new String(Files.readAllBytes(Paths.get("/home/data/git/LibreraReader/Builder/whatsnew/recent.txt")));

        StringBuilder res = new StringBuilder();
        for (String ex : asList) {
            if(ex.contains("zh-")){
                ex = ex.trim();
            }else {
                ex = ex.trim().substring(0, 2);
            }
            System.out.println("[lang] |" + ex + "|");

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
            // Files.write(Paths.get(outRecnet), recentTR.getBytes());

            String[] lnx = ln(ex);
            for (String ln : lnx) {
                res.append("\n<" + ln + ">\n");
                res.append(translation);
                res.append("\n</" + ln + ">\n");
            }
        }

        recentEN = recentEN.replace("_", "");
        // Files.write(Paths.get(RECENT_PATH + "en" + ".txt"), recentEN.getBytes());

        String[] lnx = ln("en");
        for (String ln : lnx) {
            res.append("\n<" + ln + ">\n");
            res.append(recentEN);
            res.append("\n</" + ln + ">\n");
        }

        System.out.println("=======");
        System.out.println(res.toString());
        Files.write(Paths.get(RECENT_PATH + "all" + ".txt"), res.toString().getBytes());
    }

}
