package translations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.simpleframework.xml.core.Persister;

import translations.model.ResourcesModel;
import translations.model.StringArray;
import translations.model.StringModel;

/**
 * @author Ivan Ivanenko
 * 
 */
public class SyncTranslations {

    static class Config {
        String name;
        String dropbox;// path to dropbox
        String project;// path to project res

        public Config(final String name, final String dropbox, final String project) {
            this.name = name;
            this.dropbox = dropbox;
            this.project = project;
        }

    }

    public static List<String> getAllLangCodes(String path) {
        File file = new File(path);
        String[] list = file.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.contains("large") || name.contains("v14") || name.contains("v21")) {
                    return false;
                }
                if (name.contains("values-")) {
                    return true;
                }
                return false;
            }
        });

        final List<String> asList = new ArrayList<>();
        for (String ln : list) {
            asList.add(ln.replace("values-", ""));
        }
        return asList;
    }

    private static Config IVAN = new Config("ivan", "", "/home/ivan-dev/git/pdf4/EBookDroid/res/");

    // run as "SyncTranslations user_name"
    public static void main(final String[] args) throws Exception {

        final Config config = IVAN;

        if (config == null) {
            throw new IllegalArgumentException("Input your name from list");
        }

        final String project = config.project;

        final String projectEN = project + "values/strings.xml";

        final List<String> asList = getAllLangCodes("/home/ivan-dev/git/pdf4/EBookDroid/res");

        // final List<String> asList = Arrays.asList("ru", "uk");

        normalize(projectEN);
        for (final String lang : asList) {
            final String projectRU = project + "values-" + lang + "/strings.xml";
            merge(projectEN, projectRU, true, lang);
        }

    }

    public static void copy(final String in, final String out) throws IOException {
        Files.copy(new File(in).toPath(), new File(out).toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("copy " + in + " to " + out);
    }

    public static void merge(final String inFile, final String outFile, final boolean markUntranslated, String lang) throws FileNotFoundException, Exception {
        System.out.println("markUntranlated begin" + outFile);

        final ResourcesModel in = load(inFile);
        final ResourcesModel out = normilize(load(outFile));

        for (final StringModel model : in.getStrings()) {
            if (!hasKey(out, model.getName())) {
                String text = model.getText();
                if (markUntranslated) {
                    text = unTranslated(text, lang);
                }
                text = normilizeText(text);
                model.setText(text);
                out.getStrings().add(model);
            }
        }

        final Iterator<StringModel> iterator = out.getStrings().iterator();
        while (iterator.hasNext()) {
            final StringModel next = iterator.next();
            if (!hasKey(in, next.getName())) {
                iterator.remove();
            }
            final boolean app = Arrays.asList("app_name_beta", "app_name_prod").contains(next.getName());
            if (!app && sameText(in, next.getName(), next.getText())) {
                // next.setText(unTranslated(next.getText()));
                // System.out.println(next.getText());
            }
        }

        save(outFile, out);
        System.out.println("markUntranlated end" + outFile);
    }

    private static ResourcesModel load(final String inFile) throws Exception, FileNotFoundException {
        final ResourcesModel read = new Persister().read(ResourcesModel.class, new FileInputStream(inFile));
        return read;
    }

    public static boolean hasKey(final ResourcesModel in, final String key) {
        for (final StringModel model : in.getStrings()) {
            if (model.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean sameText(final ResourcesModel in, final String key, final String text) {
        for (final StringModel model : in.getStrings()) {
            if (model.getName().equals(key) && model.getText().equals(text)) {
                return true;
            }
        }
        return false;
    }

    public static boolean saveValues(final ResourcesModel in, final ResourcesModel out, final String key) {
        for (final StringModel model : in.getStrings()) {
            if (model.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static void normalize(final String path) throws FileNotFoundException, Exception {
        System.out.println("normilize begin " + path);
        new File(path).mkdirs();
        final ResourcesModel original = load(path);

        normilize(original);
        save(path, original);
        System.out.println("normilize end " + path);

    }

    private static ResourcesModel normilize(final ResourcesModel original) {
        for (final StringArray model : original.getStringArrays()) {
            final List<String> item = model.getItem();
            for (int i = 0; i < item.size(); i++) {
                final String text = normilizeText(item.get(i));
                item.set(i, text);
            }

        }
        for (final StringModel model : original.getStrings()) {
            final String normilizeText = normilizeText(model.getText());
            model.setText(normilizeText);
        }
        return original;
    }

    private static void save(final String path, final ResourcesModel original) throws Exception, FileNotFoundException {
        sort(original);
        new Persister().write(original, new FileOutputStream(path));
    }

    public static void sort(final ResourcesModel in) {
        // Collections.sort(in.getStrings(), new ModelComparator());

    }

    static class ModelComparator implements Comparator<StringModel> {

        @Override
        public int compare(final StringModel o1, final StringModel o2) {
            return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        }
    }

    private static String normilizeText(String text) {
        text = text.trim();
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        text = upperCase(text);

        text = text.replace("\\'", "\'");
        text = text.replace("'", "\\'");
        text = text.replace(" & ", " &amp; ");
        text = text.replace("&#39;", "\\'");

        if (text.contains("<")) {
            text = "<![CDATA[" + text + "]]>";
        }
        return text.trim();
    }

    public static String upperCase(String text) {
        if (text.length() >= 1) {
            text = text.substring(0, 1).toUpperCase() + text.substring(1);
        }
        return text;
    }

    private static String unTranslated(String text, String lang) {
        if (!text.startsWith("[T]")) {
            // text = "[T]" + text;
            try {
                text = GoogleTranslation.translate(text, lang);
                text = normilizeText(text);
            } catch (JSONException | IOException e) {
                System.out.println("Can't translate " + text);
                e.printStackTrace();
            }
        }
        text = text.replace(" & ", " &amp; ");
        return text;
    }

}
