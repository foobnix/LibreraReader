package translations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

public class GoogleTranslation {

    private static final String KEY = "AIzaSyBJpTJYLsjwZiUOMHusU0QFEbxfXFCWk7M";
    // private static final String KEY =
    // "AIzaSyBjxDSZeTdavOAGFbM185GpHVPn142Gm1o";
    // https://console.cloud.google.com/apis/credentials/key/0?project=seismic-bucksaw-120809

    public static void main(String[] args) throws JSONException, IOException {
        String ln = translate("sun", "zh");
    }

    public static String translate(String inputOriginal, String toLang) throws JSONException, IOException {
        String input = URLEncoder.encode(inputOriginal, "UTF-8");
        // https://www.googleapis.com/language/translate/v2?key=AIzaSyBJpTJYLsjwZiUOMHusU0QFEbxfXFCWk7M&q=demo&source=en&target=ru
        String url = "https://www.googleapis.com/language/translate/v2?key=" + KEY + "&q=" + input + "&source=en&target=" + toLang;
        // System.out.println(url);
        JSONObject json = readJsonFromUrl(url);
        String translate = json.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");

        translate = translate.replace("\u200b", " ");

        System.out.println("[en]" + inputOriginal);
        System.out.println("[" + toLang + "]" + translate);
        System.out.println();
        // System.out.println(translate);

        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
        }

        return translate;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws JSONException {
        InputStream is = null;
        try {
            is = new URL(url).openStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new RuntimeException();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
