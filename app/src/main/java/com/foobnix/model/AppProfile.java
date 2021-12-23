package com.foobnix.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.drive.GFile;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.DragingPopup;
import com.foobnix.pdf.info.wrapper.PasswordState;
import com.foobnix.pdf.search.view.AsyncProgressResultToastTask;
import com.foobnix.ui2.AppDB;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppProfile {

    public static final String PROFILE_PREFIX = "profile.";
    public static final String DEVICE_PREFIX = "device.";
    public static final File DOWNLOADS_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    public static final String DEVICE_MODEL = DEVICE_PREFIX + Build.MODEL.replace(" ", "_");
    public static final String APP_STATE_JSON = "app-State.json";
    public static final String APP_CSS_JSON = "app-CSS.json";
    public static final String APP_RECENT_JSON = "app-Recent.json";
    public static final String APP_EXCLUDE_JSON = "app-Exclude.json";
    public static final String APP_FAVORITE_JSON = "app-Favorite.json";
    public static final String APP_BOOKMARKS_JSON = "app-Bookmarks.json";
    public static final String APP_PROGRESS_JSON = "app-Progress.json";
    public static final String APP_TAGS_JSON = "app-Tags.json";
    public static final String SYNC_FOLDER_ROOT_KEY = "syncFolderRoot";
    public static final String APP_PROFILE_SP = "AppProfile";


    public static File SYNC_FOLDER_ROOT;
    public static File SYNC_FOLDER_BOOKS;
    public static File SYNC_FOLDER_DICT;
    public static File SYNC_FOLDER_PROFILE;
    public static File SYNC_FOLDER_DEVICE_PROFILE;
    public static File syncRecent;
    public static File syncFavorite;
    public static File syncExclude;
    public static File syncState;
    public static File syncTags;
    public static File syncCSS;
    public static File syncPlaylist;
    public static File syncProgress;
    public static File syncBookmarks;

    public static File syncFontFolder;
    public static File syncDownloadFolder;

    public static File FONT_LOCAL_ZIP;

    public static String profile = "";

    public synchronized static void init(Context c) {

        if (c == null) {
            LOG.d("AppProfile init null");
            return;
        }
        AppSP.get().init(c);

        if (!Android6.canWrite(c)) {
            return;
        }

        if (profile.equals(getCurrent(c))) {
            LOG.d("AppProfile skip", profile);
            return;
        }
        profile = getCurrent(c);
        AppDB.get().open(c, "db-"+AppSP.get().rootPath.hashCode()+"-"+profile);
        LOG.d("AppProfile init", profile);



        SYNC_FOLDER_ROOT = new File(AppSP.get().rootPath);
        SYNC_FOLDER_BOOKS = new File(SYNC_FOLDER_ROOT, "Books");
        SYNC_FOLDER_DICT = new File(SYNC_FOLDER_ROOT, "dict");
        FONT_LOCAL_ZIP = new File(SYNC_FOLDER_ROOT, "fonts.zip.pack");
        syncFontFolder = new File(SYNC_FOLDER_ROOT, "Fonts");
        syncDownloadFolder = new File(SYNC_FOLDER_ROOT, "Downloads");

        SYNC_FOLDER_PROFILE = new File(SYNC_FOLDER_ROOT, PROFILE_PREFIX + getCurrent(c));
        SYNC_FOLDER_DEVICE_PROFILE = new File(SYNC_FOLDER_PROFILE, DEVICE_MODEL);
        SYNC_FOLDER_DEVICE_PROFILE.mkdirs();

        syncFavorite = new File(SYNC_FOLDER_DEVICE_PROFILE, APP_FAVORITE_JSON);
        syncExclude = new File(SYNC_FOLDER_DEVICE_PROFILE, APP_EXCLUDE_JSON);
        syncTags = new File(SYNC_FOLDER_DEVICE_PROFILE, APP_TAGS_JSON);
        syncPlaylist = new File(SYNC_FOLDER_DEVICE_PROFILE, "playlists");
        syncBookmarks = new File(SYNC_FOLDER_DEVICE_PROFILE, APP_BOOKMARKS_JSON);
        syncProgress = new File(SYNC_FOLDER_DEVICE_PROFILE, APP_PROGRESS_JSON);
        syncRecent = new File(SYNC_FOLDER_DEVICE_PROFILE, APP_RECENT_JSON);

        syncState = new File(SYNC_FOLDER_DEVICE_PROFILE, APP_STATE_JSON);
        syncCSS = new File(SYNC_FOLDER_DEVICE_PROFILE, APP_CSS_JSON);

        final boolean isLoaded = AppState.get().loadInit(c);
        if (isLoaded) {
            AppState.get().load(c);
        }
        TintUtil.init();
        BookCSS.get().load1(c);

        PasswordState.get().load(c);
        DragingPopup.loadCache(c);
        ExtUtils.init(c);
    }


    public static List<File> getAllFiles(String name) {


        List<File> list = new ArrayList<>();
        if (AppProfile.SYNC_FOLDER_PROFILE == null) {
            return list;
        }

        final File[] files = AppProfile.SYNC_FOLDER_PROFILE.listFiles();
        if (files == null) {
            return list;
        }
        for (File f : files) {
            if (f.isDirectory() && f.getName().startsWith(DEVICE_PREFIX)) {
                File file = new File(f, name);
                if (file.isFile()) {
                    list.add(file);
                    LOG.d("getAllFiles", file);
                }
            }
        }

        return list;
    }

    public static Drawable getProfileColorDrawable(Context c, String profile) {
        GradientDrawable background = (GradientDrawable) c.getResources().getDrawable(R.drawable.bg_circular);
        AppState s = new AppState();
        File syncState = new File(AppProfile.SYNC_FOLDER_ROOT, PROFILE_PREFIX + profile + "/" + DEVICE_MODEL + "/" + APP_STATE_JSON);
        IO.readObj(syncState, s);
        background.setColor(s.tintColor);
        return background;
    }

    public static Drawable getProfileColorDrawable(Context c, int color) {
        GradientDrawable background = (GradientDrawable) c.getResources().getDrawable(R.drawable.bg_circular);
        background.setColor(color);
        return background;
    }


    public static synchronized void save(Context a) {
        if (a == null) {
            return;
        }
        if (!Android6.canWrite(a)) {
            return;
        }
        if (TxtUtils.isNotEmpty(profile)) {
            DragingPopup.saveCache(a);
            PasswordState.get().save(a);
            AppState.get().save(a);
            BookCSS.get().save(a);
            AppSP.get().save();
        }
    }

    public static String getCurrent(Context c) {
        return AppSP.get().currentProfile;
    }

    public static void saveCurrent(Context c, String name) {
        AppSP.get().currentProfile = name;
        save(c);

    }


    public static List<String> getAllProfiles() {
        final File[] files = SYNC_FOLDER_ROOT.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> res = new ArrayList<>();
        for (File file : files) {
            if (file.getName().startsWith(PROFILE_PREFIX)) {
                res.add(file.getName().replace(PROFILE_PREFIX, ""));
            }
        }
        Collections.sort(res, String.CASE_INSENSITIVE_ORDER);
        return res;
    }

    public static void ceateProfiles(Context c, String name) {
        name = name.replace(" ", "");
        final File profileFolder = new File(SYNC_FOLDER_ROOT, PROFILE_PREFIX + name + "/" + DEVICE_MODEL);
        profileFolder.mkdirs();


        File state = new File(profileFolder, APP_STATE_JSON);
        File css = new File(profileFolder, APP_CSS_JSON);

        final AppState appState = new AppState();
        appState.defaults(c);
        IO.writeObjAsync(state, appState);
        final BookCSS bookCss = new BookCSS();
        bookCss.resetToDefault(c);
        IO.writeObjAsync(css, bookCss);
        LOG.d("Profile Created");

    }

    public static void deleteProfiles(Activity a, String
            name, ResultResponse<Boolean> result) {
        new AsyncProgressResultToastTask(a, result) {

            @Override
            protected Boolean doInBackground(Object... objects) {
                try {
                    final File file = new File(SYNC_FOLDER_ROOT, PROFILE_PREFIX + name);
                    GFile.deleteRemoteFile(file);
                    ExtUtils.deleteRecursive(file);
                    //GFile.runSyncService(a);
                } catch (Exception e) {
                    LOG.e(e);
                    return false;
                }
                return true;
            }

        }.execute();

    }


    public static void showDialog(Activity a, ResultResponse<String> onclick) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        // builder.setTitle(R.string.tag);

        View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_tags, null, false);

        final ListView list = (ListView) inflate.findViewById(R.id.listView1);
        final TextView add = (TextView) inflate.findViewById(R.id.addTag);
        TxtUtils.underline(add, a.getString(R.string.new_profile));

        final List<String> profiles = getAllProfiles();


        final BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(a, R.layout.tag_item_text, profiles) {
            @Override
            public void populateView(View layout, final int position, final String tagName) {
                TextView text = layout.findViewById(R.id.text1);
                text.setText(tagName);

                ImageView delete = (ImageView) layout.findViewById(R.id.delete1);
                TintUtil.setTintImageWithAlpha(delete, Color.GRAY);
                if (tagName.equals(getCurrent(a))) {
                    delete.setVisibility(View.GONE);
                } else {
                    delete.setVisibility(View.VISIBLE);
                }


                delete.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AlertDialogs.showOkDialog(a, a.getString(R.string.do_you_want_to_delete_) + " " + tagName, new Runnable() {

                            @Override
                            public void run() {

                                deleteProfiles(a, tagName, new ResultResponse<Boolean>() {
                                    @Override
                                    public boolean onResultRecive(Boolean result) {
                                        if (result) {
                                            profiles.clear();
                                            profiles.addAll(getAllProfiles());
                                            notifyDataSetChanged();
                                        }
                                        return false;
                                    }
                                });

                            }
                        });

                    }
                });

            }
        };
        add.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addDialog(a, new Runnable() {

                    @Override
                    public void run() {
                        profiles.clear();
                        profiles.addAll(getAllProfiles());
                        adapter.notifyDataSetChanged();

                    }
                });
            }
        });

        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onclick.onResultRecive(profiles.get(position));
            }
        });

        builder.setView(inflate);

        builder.setNegativeButton(R.string.close, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });


        AlertDialog create = builder.create();
        create.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.close(a);
                Keyboards.hideNavigation(a);

            }
        });
        create.show();


    }

    public static void addDialog(final Activity a, final Runnable onRefresh) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.profile);

        final EditText edit = new EditText(a);
        edit.setHint(R.string.name);

        builder.setView(edit);

        builder.setNegativeButton(R.string.cancel, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        builder.setPositiveButton(R.string.add, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        final AlertDialog create = builder.create();
        create.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = edit.getText().toString().trim();
                text = text.replace(" ", "");

                if (TxtUtils.isEmpty(text)) {
                    Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (getAllProfiles().contains(text)) {
                    Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }
                create.dismiss();

                ceateProfiles(a, text);
                GFile.runSyncService(a);

                onRefresh.run();

                Keyboards.close(edit);
                Keyboards.hideNavigation((Activity) a);


            }
        });
    }

    public static void clear() {
        profile = "";
        AppState.get().isLoaded = false;
    }
}
