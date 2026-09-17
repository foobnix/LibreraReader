import paths.LocalPaths;
import java.util.Collections;
import java.util.List;

import translations.SyncTranslations;

public class PrintLangCodes {
    public static void main(String[] args) {
        final List<String> asList = SyncTranslations.getAllLangCodes(LocalPaths.repo("app/src/main/res"));
        Collections.sort(asList);

        for (final String lang : asList) {
            System.out.print("\"" + lang + "\",");
        }
    }

}
