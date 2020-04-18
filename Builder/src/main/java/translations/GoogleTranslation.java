package translations;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class GoogleTranslation {

    private static final String KEY = "AIzaSyB-JpTJYLsj-wZiUOMHusU0QF-EbxfXFC-Wk7M";

    public static void main(String[] args) throws JSONException, IOException {
        String ln = translate("sun", "zh");
    }

    public static String translate(String inputOriginal, String toLang) throws JSONException, IOException {
        return translate(inputOriginal, "en", toLang);
    }

    public static String translate(String inputOriginal, String from, String toLang) throws JSONException, IOException {
        String input = URLEncoder.encode(inputOriginal, "UTF-8");
        String url = "https://www.googleapis.com/language/translate/v2?key=" + KEY.replace("-","") + "&q=" + input + "&source=" + from + "&target=" + toLang;

        // System.out.println(url);
        JSONObject json = readJsonFromUrl(url);
        String translate = json.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");

        translate = translate.replace("\u200b", " ");

        System.out.println("[en]" + inputOriginal);
        System.out.println("[" + toLang + "]" + translate);
        System.out.println();
        // System.out.println(translate);

        try {
            Thread.sleep(250);
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
        try {

            URL is = new URL(url);
            HttpURLConnection urlcon = (HttpURLConnection) is.openConnection();
            urlcon.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            System.setProperty("http.agent", "Chrome");

            BufferedReader rd = new BufferedReader(new InputStreamReader(urlcon.getInputStream(), Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            rd.close();
            return json;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new RuntimeException();
        }
    }

}
