package translations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class TextTranslations {
    private static final String PATH = "/home/data/git/LibreraReader/Builder/Description/text/";

    public static void main(String[] args) throws IOException {
        final List<String> asList = Arrays.asList("ar", "de", "es", "fr", "it", "ja", "pt", "ru", "sk", "tr", "uk", "zh", "nl");
        // final List<String> asList = Arrays.asList("ru", "uk");

        String textPath = new String(Files.readAllBytes(Paths.get("/home/data/git/LibreraReader/Builder/Description/text.txt")));

        for (String ex : asList) {
            String outAbout = PATH + ex + ".txt";

            String translation = GoogleTranslation.translate(textPath, ex);

            translation = translation.replace("___", "");
            translation = translation.replace("Libri", "Lirbi");
            translation = translation.replace("_ ", "\n");
            translation = translation.replace("_", "\n");
            translation = translation.replace("&#39;", "’");

            Files.write(Paths.get(outAbout), translation.getBytes());

        }
        textPath = textPath.replace("_", "");
        Files.write(Paths.get(PATH + "en" + ".txt"), textPath.getBytes());

    }
}
