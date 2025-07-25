package com.foobnix.pdf.info;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppData;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.view.DragingDialogs;
import com.foobnix.sys.TempHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DictsHelper {

    public static int getHash(ActivityInfo activityInfo) {
        String s = activityInfo.name + activityInfo.packageName;
        return s.hashCode();
    }

    @NonNull
    public static List<ResolveInfo> resolveInfosList(Intent intent, PackageManager pm) {
        try {
            return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        } catch (Exception e) {
            LOG.e(e);
            try {
                return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
            } catch (Exception e1) {
                LOG.e(e1);
                return new ArrayList<>();
            }

        }
    }

    public static List<ResolveInfo> resolveInfosList(Context c, Intent intent) {
        try {
            return resolveInfosList(intent, c.getPackageManager());
        } catch (Exception e) {
            LOG.e(e);
            return new ArrayList<>();
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

        if (AppSP.get().isDouble || Dips.screenWidth() > Dips.screenHeight()) {

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

            DictItem e = new DictItem(name, type, item.activityInfo.packageName, icon);
            e.addHash(item.activityInfo);
            items.add(e);
        }

        return items;

    }

    public static List<DictItem> getAllResolveInfoAsDictItem1(Context c, String text) {
        List<DictItem> items = new ArrayList<DictItem>();
        items.addAll(getByType(c, resolveInfosList(c, getType0(text)), "type0"));
        items.addAll(getByType(c, resolveInfosList(c, getType1(text)), "type1"));
        items.addAll(getByType(c, resolveInfosList(c, getType2(text)), "type2"));
        items.addAll(getByType(c, resolveInfosList(c, getType3(text)), "type3"));

        return items;
    }

    public static List<DictItem> getOnlineDicts(Context c, String text) {
        List<DictItem> items = new ArrayList<DictItem>();
        Set<String> keySet = AppData.get().getWebDictionaries(text).keySet();
        for (String it : keySet) {
            items.add(new DictItem(it, "web", "", null));
        }
        keySet = AppData.get().getWebSearch(text).keySet();
        for (String it : keySet) {
            items.add(new DictItem(it, "web", "", null));
        }
        return items;
    }

    public static void runIntent(Activity c, FrameLayout anchor, String selectedText) {
        try {
            String dict = AppState.get().rememberDict1;
            LOG.d("runIntent-dict", selectedText, dict);
            final int dictHash = AppState.get().rememberDict1Hash;
            String dictName = DictItem.fetchDictName(AppState.get().rememberDict1);
            if (dict.startsWith("web")) {
                Map<String, String> dictionaries = AppData.get().getWebDictionaries(selectedText);
                String url = dictionaries.get(dictName);
                if (url == null) {
                    dictionaries = AppData.get().getWebSearch(selectedText);
                    url = dictionaries.get(dictName);
                }
                //Urls.open(c, url);
                DragingDialogs.dialogWebView(anchor, url);
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
                List<ResolveInfo> apps = resolveInfosList(c, intent);
                for (final ResolveInfo app : apps) {
                    // String name = app.activityInfo.loadLabel(c.getPackageManager()).toString();
                    if (dictHash == DictsHelper.getHash(app.activityInfo)) {
                        final ComponentName cName = new ComponentName(app.activityInfo.applicationInfo.packageName, app.activityInfo.name);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.setComponent(cName);
                        c.startActivity(intent);
                        LOG.d("dict-intent", intent);
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


    public static List<ResolveInfo> getAllResolveInfo(Context c, String text) {
        PackageManager pm = c.getPackageManager();

        Intent intentProccessCustom = getType0(text);
        Intent intentProccessText = getType1(text);
        Intent intentSearch = getType2(text);
        Intent intentSend = getType3(text);

        final List<ResolveInfo> proccessCustom = resolveInfosList(intentProccessCustom, pm);
        final List<ResolveInfo> proccessTextList = resolveInfosList(intentProccessText, pm);
        final List<ResolveInfo> searchList = resolveInfosList(intentSearch, pm);
        final List<ResolveInfo> sendList = resolveInfosList(intentSend, pm);

        final List<ResolveInfo> all = new ArrayList<ResolveInfo>();
        all.addAll(proccessCustom);
        all.addAll(proccessTextList);
        all.addAll(searchList);
        all.addAll(sendList);
        return all;
    }

    public static class DictItem {
        public String name;
        public String type;
        public String pkg;
        public int hash;
        public Drawable image;

        public DictItem(String name, String type, String pkg, Drawable image) {
            this.name = name;
            this.type = type;
            this.image = image;
            this.pkg = pkg;
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

        @Override
        public String toString() {
            return type + ":" + name;
        }

        public void addHash(ActivityInfo activityInfo) {
            hash = getHash(activityInfo);
        }

    }

}
