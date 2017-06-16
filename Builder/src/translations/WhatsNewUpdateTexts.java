package translations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class WhatsNewUpdateTexts {

    private static final String RECENT_PATH = "/home/ivan-dev/git/LirbiReader/EbookaPDF/assets/whatsnew/";

    public static void main(String[] args) throws IOException {
        final List<String> asList = SyncTranslations.getAllLangCodes("/home/ivan-dev/git/LirbiReader/EBookDroid/res");
        // final List<String> asList = Arrays.asList("ru");

        String recentEN = new String(Files.readAllBytes(Paths.get("/home/ivan-dev/git/LirbiReader/EbookaPDF/assets/recent.txt")));

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
            translation = SyncTranslations.upperCase(translation);
            Files.write(Paths.get(outRecnet), recentTR.getBytes());

        }

        recentEN = recentEN.replace("_", "");
        Files.write(Paths.get(RECENT_PATH + "en" + ".txt"), recentEN.getBytes());

    }

}
