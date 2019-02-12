package com.foobnix.pdf.info;

import java.util.LinkedHashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Https;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

public class ADSRemote {
    private static final String TIME = "time";
    private static final String ADS_ID = "id";
    private static final String CONFIG_JSON = "http://foobnix.com/welcome/config.json";
    private static final Random random = new Random();
    public static String adID;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void initID(final Context c, final ResultResponse<String> idResponse) {
        if (adID != null) {
            idResponse.onResultRecive(adID);
            return;
        }

        final SharedPreferences sp = c.getSharedPreferences("adsconfig", Context.MODE_PRIVATE);

        String idValue = sp.getString(ADS_ID, null);
        long time = sp.getLong(TIME, 0);

        final long currentTimeMillis = System.currentTimeMillis();
        long dif = currentTimeMillis - time;

        if (idValue != null && dif < TimeUnit.DAYS.toMillis(7)) {
            LOG.d("Get adId from cache", idValue);
            idResponse.onResultRecive(idValue);
            return;
        }

        new AsyncTask() {

            @Override
            protected Object doInBackground(Object... params) {
                try {
                    String result = Https.getUrlContents(CONFIG_JSON);
                    LOG.d("Result JSON", result);
                    return result;
                } catch (Exception e) {
                    LOG.e(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                if (result == null) {
                    LOG.d("No config result get default", AppsConfig.ADMOB_BANNER);
                    idResponse.onResultRecive(AppsConfig.ADMOB_BANNER);
                    return;
                }
                try {
                    LinkedHashMap<String, Integer> input = new LinkedHashMap<String, Integer>();
                    JSONObject object = new JSONObject(result.toString());
                    JSONArray array = object.getJSONArray(Apps.getPackageName(c));
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        input.put(item.getString("n"), item.getInt("v"));
                    }

                    String adID = getAdsID(input);
                    sp.edit().putString(ADS_ID, adID).putLong(TIME, currentTimeMillis).commit();
                    idResponse.onResultRecive(adID);
                    LOG.d("Get result from server", adID);
                    LOG.d("Finish");
                } catch (Exception e) {
                    LOG.e(e);
                    idResponse.onResultRecive(AppsConfig.ADMOB_BANNER);
                }
            };
        }.execute();

    }

    private static String getAdsID(LinkedHashMap<String, Integer> input) {

        int sum = 0;
        for (int i : input.values()) {
            sum += i;
        }
        int nextInt = random.nextInt(sum);

        int rangeSum = 0;
        for (String key : input.keySet()) {
            int value = input.get(key);
            if (nextInt >= rangeSum && nextInt < rangeSum + value) {
                return key;
            }
            rangeSum = rangeSum + value;
        }
        return input.keySet().iterator().next();
    }
}
