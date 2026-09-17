package paths;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Where the Builder tools find things, so no machine's own paths are written into the source:
 * files of this repo are found from the checkout the tool is run in, and folders outside it
 * are named in the global ~/.gradle/gradle.properties (or an environment variable of the same
 * name in capitals, e.g. LIBRERA_PROJECTS_DIR for librera_projects_dir).
 */
public class LocalPaths {

    private static File repo;
    private static Properties gradle;

    /** The LibreraReader checkout: the folder with settings.gradle.kts, looked for up from the working directory. */
    public static synchronized File repo() {
        if (repo == null) {
            File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
            while (dir != null && !new File(dir, "settings.gradle.kts").isFile()) {
                dir = dir.getParentFile();
            }
            if (dir == null) {
                throw new IllegalStateException("Run from inside the LibreraReader checkout: settings.gradle.kts not found up from " + System.getProperty("user.dir"));
            }
            repo = dir;
        }
        return repo;
    }

    /** A path inside the checkout, e.g. repo("app/src/main/res/"); a trailing slash is kept. */
    public static String repo(String relative) {
        return join(repo().getPath(), relative);
    }

    /** A folder named in the global gradle.properties or the environment; fails with the name to set when it is missing. */
    public static String property(String name) {
        String value = System.getenv(name.toUpperCase());
        if (value == null || value.trim().isEmpty()) {
            value = gradle().getProperty(name);
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " is not set in " + gradleFile() + " (or " + name.toUpperCase() + " in the environment)");
        }
        return value.trim();
    }

    /** A path under a folder named in the global gradle.properties, e.g. property("librera_dev_dir", "cache.json"). */
    public static String property(String name, String relative) {
        return join(property(name), relative);
    }

    private static String join(String dir, String relative) {
        String path = new File(dir, relative).getPath();
        return relative.endsWith("/") ? path + "/" : path;
    }

    private static File gradleFile() {
        String gradleHome = System.getenv("GRADLE_USER_HOME");
        return new File(gradleHome != null ? gradleHome : System.getProperty("user.home") + "/.gradle", "gradle.properties");
    }

    private static synchronized Properties gradle() {
        if (gradle == null) {
            gradle = new Properties();
            File file = gradleFile();
            if (file.isFile()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    gradle.load(reader);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot read " + file, e);
                }
            }
        }
        return gradle;
    }
}
