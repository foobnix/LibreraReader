package translations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class DescriptionTranslation {

    private static final String TR_PATH = "/home/data/git/LibreraReader/Builder/Description/about/";
    private static final String RECENT_PATH = "/home/data/git/LibreraReader/Builder/Description/recent/";

    public static void main(String[] args) throws IOException {
        // final List<String> asList = Arrays.asList("ar", "de", "es", "fr",
        // "it", "ja", "pt", "ru", "sk", "tr", "uk", "zh", "nl");
        final List<String> asList = Arrays.asList("ru");

        boolean translateDescription = false;
        boolean translateRecent = true;

        String aboutEN = new String(Files.readAllBytes(Paths.get("/home/data/git/LibreraReader/Builder/Description/about.txt")));
        String recentEN = new String(Files.readAllBytes(Paths.get("/home/data/git/LibreraReader/Builder/Description/recent.txt")));

        for (String ex : asList) {
            String outAbout = TR_PATH + ex + ".txt";
            String outRecnet = RECENT_PATH + ex + ".txt";

            String translation = "";

            if (translateDescription) {
                translation = GoogleTranslation.translate(aboutEN, ex);

                translation = translation.replace("Libri", "Lirbi");
                translation = translation.replace("_ ", "\n");
                translation = translation.replace("_", "\n");
                // translation = translation.replace(")", "\n)");
                translation = translation.replace("&#39;", "’");
                translation = SyncTranslations.upperCase(translation);
                Files.write(Paths.get(outAbout), translation.getBytes());
            }

            if (translateRecent) {
                String recentTR = GoogleTranslation.translate(recentEN, ex);
                translation = translation.replace("___", "");
                translation = translation.replace("Libri", "Lirbi");
                recentTR = recentTR.replace("_ ", "\n");
                recentTR = recentTR.replace("_", "\n");
                recentTR = recentTR.replace("&#39;", "’");
                translation = SyncTranslations.upperCase(translation);
                Files.write(Paths.get(outRecnet), recentTR.getBytes());
            }

        }
        aboutEN = aboutEN.replace("_", "");
        Files.write(Paths.get(TR_PATH + "en" + ".txt"), aboutEN.getBytes());

        recentEN = recentEN.replace("_", "");
        Files.write(Paths.get(RECENT_PATH + "en" + ".txt"), recentEN.getBytes());

    }

}
