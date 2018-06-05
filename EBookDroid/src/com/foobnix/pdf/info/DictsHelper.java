package com.foobnix.pdf.info;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.TempHolder;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.Toast;

public class DictsHelper {

    public static class DictItem {
        public String name;
        public String type;
        public Drawable image;

        public DictItem(String name, String type, Drawable image) {
            this.name = name;
            this.type = type;
            this.image = image;
        }

        @Override
        public String toString() {
            return type + ":" + name;
        }

        public static String fetchDictName(String format) {
            try {
                if (format.contains(":")) {
                    return format.substring(format.indexOf(":") + 1);
                } else {
                    return format;
                }
            } catch (Exception e) {
                return format;
            }

        }

    }

    public static Intent getType1(String selecteText) {
        final Intent intentProccessText = new Intent();
        intentProccessText.setAction(Intent.ACTION_PROCESS_TEXT);
        intentProccessText.putExtra(Intent.EXTRA_TEXT, selecteText);
        intentProccessText.putExtra(Intent.EXTRA_PROCESS_TEXT, selecteText);
        intentProccessText.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, selecteText);
        intentProccessText.setType("text/plain");
        return intentProccessText;
    }

    public static Intent getType2(String selecteText) {
        final Intent intentSearch = new Intent();
        intentSearch.setAction(Intent.ACTION_SEARCH);
        intentSearch.putExtra(SearchManager.QUERY, selecteText);
        intentSearch.putExtra(Intent.EXTRA_TEXT, selecteText);
        return intentSearch;
    }

    public static Intent getType3(String selecteText) {
        final Intent intentSend = new Intent();
        intentSend.setAction(Intent.ACTION_SEND);
        intentSend.putExtra(Intent.EXTRA_TEXT, selecteText);
        intentSend.setType("text/plain");
        return intentSend;
    }

    public static Intent getType0(String selecteText) {
        final Intent intentCustom = new Intent();
        intentCustom.setAction("colordict.intent.action.SEARCH");

        intentCustom.putExtra("EXTRA_QUERY", selecteText);
        updateExtraGoldenDict(intentCustom);

        LOG.d("intentCustom1", intentCustom, intentCustom.getExtras());

        return intentCustom;
    }

    public static void updateExtraGoldenDict(final Intent intentCustom) {
        intentCustom.putExtra("EXTRA_HEIGHT", Dips.screenHeight() * 3 / 4);
        intentCustom.putExtra("EXTRA_GRAVITY", Gravity.BOTTOM);
        intentCustom.putExtra("EXTRA_FULLSCREEN", false);

        if (AppState.get().isDouble || Dips.screenWidth() > Dips.screenHeight()) {

            if (TempHolder.get().textFromPage == 1) {
                intentCustom.putExtra("EXTRA_GRAVITY", Gravity.BOTTOM | Gravity.RIGHT);
            } else if (TempHolder.get().textFromPage == 2) {
                intentCustom.putExtra("EXTRA_GRAVITY", Gravity.BOTTOM | Gravity.LEFT);
            } else {
                intentCustom.putExtra("EXTRA_GRAVITY", Gravity.BOTTOM | Gravity.CENTER);
            }
            intentCustom.putExtra("EXTRA_WIDTH", Dips.screenWidth() / 2);
        } else {
            intentCustom.putExtra("EXTRA_GRAVITY", Gravity.BOTTOM);
        }
    }

    public static List<DictItem> getByType(Context c, List<ResolveInfo> all, String type) {
        List<DictItem> items = new ArrayList<DictItem>();
        for (ResolveInfo item : all) {
            Drawable icon = null;
            try {
                icon = c.getPackageManager().getApplicationIcon(item.activityInfo.packageName);
            } catch (NameNotFoundException e) {

            }
            String name = item.activityInfo.loadLabel(c.getPackageManager()).toString();

            items.add(new DictItem(name, type, icon));
        }

        return items;

    }

    public static List<DictItem> getAllResolveInfoAsDictItem1(Context c, String text) {
        List<DictItem> items = new ArrayList<DictItem>();
        items.addAll(getByType(c, getByIntent(c, getType0(text), text), "type0"));
        items.addAll(getByType(c, getByIntent(c, getType1(text), text), "type1"));
        items.addAll(getByType(c, getByIntent(c, getType2(text), text), "type2"));
        items.addAll(getByType(c, getByIntent(c, getType3(text), text), "type3"));

        return items;
    }

    public static List<DictItem> getOnlineDicts(Context c, String text) {
        List<DictItem> items = new ArrayList<DictItem>();
        Set<String> keySet = AppState.getDictionaries(text).keySet();
        for (String it : keySet) {
            items.add(new DictItem(it, "web", null));
        }
        return items;
    }

    public static void runIntent(Activity c, String selectedText) {
        try {
            String dict = AppState.get().rememberDict;
            String dictName = DictItem.fetchDictName(AppState.get().rememberDict);
            if (dict.startsWith("web")) {
                Map<String, String> dictionaries = AppState.getDictionaries(selectedText);
                String url = dictionaries.get(dictName);
                Urls.open(c, url);
            }
            if (dict.startsWith("type")) {
                Intent intent = null;
                if (dict.startsWith("type0")) {
                    intent = getType0(selectedText);
                }
                if (dict.startsWith("type1")) {
                    intent = getType1(selectedText);
                }
                if (dict.startsWith("type2")) {
                    intent = getType2(selectedText);
                }
                if (dict.startsWith("type3")) {
                    intent = getType3(selectedText);
                }
                List<ResolveInfo> apps = getByIntent(c, intent, selectedText);
                for (final ResolveInfo app : apps) {
                    String name = app.activityInfo.loadLabel(c.getPackageManager()).toString();
                    if (name.equals(dictName)) {
                        final ComponentName cName = new ComponentName(app.activityInfo.applicationInfo.packageName, app.activityInfo.name);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.setComponent(cName);
                        c.startActivity(intent);
                        // c.overridePendingTransition(0, 0);
                        return;
                    }
                }
                Toast.makeText(c, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();

            }
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(c, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
        }

    }

    public static List<ResolveInfo> getByIntent(Context c, Intent intent, String text) {
        PackageManager pm = c.getPackageManager();
        final List<ResolveInfo> items = pm.queryIntentActivities(intent, 0);
        return items;
    }

    public static List<ResolveInfo> getAllResolveInfo(Context c, String text) {
        PackageManager pm = c.getPackageManager();

        Intent intentProccessCustom = getType0(text);
        Intent intentProccessText = getType1(text);
        Intent intentSearch = getType2(text);
        Intent intentSend = getType3(text);

        final List<ResolveInfo> proccessCustom = pm.queryIntentActivities(intentProccessCustom, 0);
        final List<ResolveInfo> proccessTextList = pm.queryIntentActivities(intentProccessText, 0);
        final List<ResolveInfo> searchList = pm.queryIntentActivities(intentSearch, 0);
        final List<ResolveInfo> sendList = pm.queryIntentActivities(intentSend, 0);

        final List<ResolveInfo> all = new ArrayList<ResolveInfo>();
        all.addAll(proccessCustom);
        all.addAll(proccessTextList);
        all.addAll(searchList);
        all.addAll(sendList);
        return all;
    }

}
