package com.foobnix.pdf.info.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import com.foobnix.pdf.info.wrapper.UITab;
import android.util.TypedValue;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.foobnix.pdf.search.activity.msg.OpenDirMessage;
import com.foobnix.ui2.fragment.UIFragment;
import com.foobnix.ui2.fragment.PrefFragment2;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Environment;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.UI;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.model.MyPath;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExportConverter;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.ExtFilter;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.presentation.BrowserAdapter;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.pdf.search.view.AsyncProgressResultToastTask;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.MainTabs2;

import net.lingala.zip4j.exception.ZipException;

import org.greenrobot.eventbus.EventBus;
import org.librera.JSONArray;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PrefDialogs {

    public static final String EXPORT_BACKUP_ZIP = "-librera-backup.zip";
    private static String lastPaht;

    private static String getLastPath() {
        try {
            return lastPaht = lastPaht != null && new File(lastPaht).isDirectory() ? lastPaht : Environment.getExternalStorageDirectory().getPath();
        } catch (Exception e) {
            return lastPaht = "/";
        }
    }

    /**
     * Adds one folder to the library paths. The root, an external SD card and a path already
     * in the list are each refused with a word on why, so the caller only has to say where to
     * put the folder it picked.
     *
     * @return true when the list gained the path.
     */
    public static boolean addSearchPath(final Context a, final String nPath) {
        if ("/".equals(nPath)) {
            Toast.makeText(a, String.format("[ / ] %s", a.getString(R.string.incorrect_value)), Toast.LENGTH_LONG).show();
            return false;
        }
        for (String str : JsonDB.get(BookCSS.get().searchPathsJson)) {
            if (str != null && str.trim().length() != 0 && nPath.equals(str)) {
                Toast.makeText(a, String.format("[ %s == %s ] %s", nPath, str,
                        a.getString(R.string.this_directory_is_already_in_the_list)), Toast.LENGTH_LONG).show();
                return false;
            }
        }
        BookCSS.get().searchPathsJson = JsonDB.add(BookCSS.get().searchPathsJson, nPath);
        return true;
    }

    /**
     * The library folders, to add to and take from, and the library rebuilt from them. The
     * preferences panel opens it from its list of folders, and the library from the update button
     * on its top bar. Adding a folder keeps the dialog up with the folder in its list, so the
     * folders can be put right first and the library rebuilt once, with Update.
     *
     * @param onChanges run each time the list changes, for whoever keeps it on screen or on disk
     * @param onScan    run by Update, to rebuild the library from the folders now listed
     */
    public static void chooseFolderDialog(final FragmentActivity a, final Runnable onChanges, final Runnable onScan) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        final View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_library_folders, null, false);
        builder.setView(view);
        final AlertDialog create = builder.create();

        final int accent = TxtUtils.getLinkTextColor(a);
        final LinearLayout folders = view.findViewById(R.id.libraryFolders);

        final Runnable[] showFolders = new Runnable[1];
        showFolders[0] = () -> {
            folders.removeAllViews();
            List<String> paths = JsonDB.get(BookCSS.get().searchPathsJson);
            Collections.sort(paths);
            for (final String path : paths) {
                folders.addView(folderRow(a, folders, path, accent, () -> AlertDialogs.showDialog(a,
                        a.getString(R.string.remove_library_folder) + "\n\n" + path, a.getString(R.string.remove), () -> {
                            BookCSS.get().searchPathsJson = JsonDB.remove(BookCSS.get().searchPathsJson, path);
                            showFolders[0].run();
                            onChanges.run();
                        }), () -> {
                    create.dismiss();
                    PrefFragment2.closeLeftMenu(a);
                    openLibraryFolder(a, path);
                }));
            }
            folders.setVisibility(paths.isEmpty() ? View.GONE : View.VISIBLE);
        };
        showFolders[0].run();

        tintAddCards(view, accent);

        view.findViewById(R.id.addLibraryFolder).setOnClickListener(v -> {
            // The chooser browses the device itself, and without the permission to read it there
            // is nothing in it to pick: the permission is asked for instead, as the panel does.
            if (!Android6.canWrite(a)) {
                Android6.checkPermissions(a, false);
                return;
            }
            ChooserDialogFragment.chooseFolder(a, BookCSS.get().dirLastPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                @Override
                public boolean onResultRecive(String nPath, Dialog dialog) {
                    if (addSearchPath(a, nPath)) {
                        showFolders[0].run();
                        onChanges.run();
                    }
                    dialog.dismiss();
                    return false;
                }
            });
        });

        // The system's picker answers the activity, which adds the folder and rebuilds the
        // library itself; this dialog would only go on listing the folders without it.
        view.findViewById(R.id.addLibraryStorage).setOnClickListener(v -> {
            create.dismiss();
            MainTabs2.startAddLibraryStorage(a);
        });

        TextView cancel = view.findViewById(R.id.libraryFoldersCancel);
        cancel.setTextColor(accent);
        cancel.setOnClickListener(v -> create.dismiss());

        TextView update = view.findViewById(R.id.libraryFoldersUpdate);
        asFilledButton(update);
        update.setOnClickListener(v -> {
            create.dismiss();
            onScan.run();
        });

        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(a);
            }
        });
        create.show();
    }

    /**
     * The library folders' main action, a pill filled in the theme colour - the colour the panel's
     * headers and the chooser's buttons are filled in - as the dialog and the panel both draw it.
     */
    public static void asFilledButton(TextView button) {
        GradientDrawable pill = new GradientDrawable();
        // A black theme colour on a dark theme would fill the button with the ground it stands
        // on: it is drawn as a white ring there instead. On the light theme black still shows.
        boolean darkTheme = AppState.get().appTheme == AppState.THEME_DARK
                || AppState.get().appTheme == AppState.THEME_DARK_OLED;
        if (darkTheme && Color.luminance(TintUtil.color) < 0.02f) {
            pill.setColor(Color.TRANSPARENT);
            pill.setStroke(Dips.dpToPx(1), Color.WHITE);
        } else {
            pill.setColor(TintUtil.color);
        }
        button.setTextColor(Color.WHITE);
        pill.setCornerRadius(Dips.dpToPx(20));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), pill, null));
    }

    /** The marks on the add folder and add storage cards, in the theme colour. */
    public static void tintAddCards(View root, int accent) {
        ((ImageView) root.findViewById(R.id.addLibraryFolderIcon)).setColorFilter(accent);
        ((ImageView) root.findViewById(R.id.addLibraryStorageIcon)).setColorFilter(accent);
    }

    /** One folder of the library: its mark in a round of the theme colour, its name, where it is, and the button that drops it. */
    public static View folderRow(Context c, LinearLayout parent, String path, int accent, Runnable onRemove,
                                 Runnable onOpen) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.library_folder_item, parent, false);
        String[] namePlace = folderNameAndPlace(c, path);

        GradientDrawable round = new GradientDrawable();
        round.setShape(GradientDrawable.OVAL);
        round.setColor((accent & 0x00FFFFFF) | 0x26000000);
        row.findViewById(R.id.folderMark).setBackground(round);

        ImageView icon = row.findViewById(R.id.folderIcon);
        // Marked as it was added, with the mark of the card that adds it: a folder the app reads
        // itself (Add folder), or one handed over through the system's picker (Add storage)
        icon.setImageResource(ExtUtils.isExteralSD(path) ? R.drawable.ic_lf_storage : R.drawable.ic_lf_folder);
        icon.setColorFilter(accent);

        ((TextView) row.findViewById(R.id.folderName)).setText(namePlace[0]);
        TextView place = row.findViewById(R.id.folderPlace);
        place.setText(namePlace[1]);
        place.setVisibility(TxtUtils.isEmpty(namePlace[1]) ? View.GONE : View.VISIBLE);

        // A folder handed over through the picker that can no longer be read says so; a tap on
        // it asks for it again (see openLibraryFolder). With the permission it is read as the
        // folder it stands for, and needs no grant.
        boolean readableAsPath = Android6.canWrite(c) && ExtUtils.getSafTreePath(path) != null;
        if (!readableAsPath && MainTabs2.isLostLibraryStorage(c, path)) {
            row.findViewById(R.id.folderNoAccess).setVisibility(View.VISIBLE);
            icon.setAlpha(0.5f);
        }

        ImageView remove = row.findViewById(R.id.folderRemove);
        remove.setColorFilter(place.getCurrentTextColor());
        remove.setContentDescription(c.getString(R.string.remove) + " " + namePlace[0]);
        remove.setOnClickListener(v -> onRemove.run());

        // The row itself opens the folder, to see what the library finds in it
        TypedValue pressed = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, pressed, true);
        row.setBackgroundResource(pressed.resourceId);
        row.setOnClickListener(v -> onOpen.run());
        return row;
    }

    /**
     * Shows a library folder in the Folder tab. A folder handed over through the system's picker
     * is shown as the folder it stands for once the app may read the storage itself, as the
     * folder chooser shows it.
     */
    public static void openLibraryFolder(Activity a, String path) {
        String target = path;
        if (ExtUtils.isExteralSD(path) && Android6.canWrite(a)) {
            String file = ExtUtils.getSafTreePath(path);
            if (file != null) {
                target = file;
            }
        }
        // A folder handed over through the picker whose grant is gone - after a reinstall, say -
        // cannot be opened: it is asked for again instead of showing nothing
        if (ExtUtils.isExteralSD(target) && MainTabs2.isLostLibraryStorage(a, target)) {
            MainTabs2.askToRegrantLibraryStorage(a, target);
            return;
        }
        // The Folder tab listens only while it is the tab on screen, so the folder is also left
        // where it looks when it comes up: as its last folder, with the lists marked as changed
        AppState.get().displayPath = target;
        BookCSS.get().dirLastPath = target;
        TempHolder.listHash++;
        LocalBroadcastManager.getInstance(a)
                             .sendBroadcast(new Intent(UIFragment.INTENT_TINT_CHANGE).putExtra(MainTabs2.EXTRA_PAGE_NUMBER,
                                     UITab.getCurrentTabIndex(UITab.BrowseFragment)));
        EventBus.getDefault().post(new OpenDirMessage(target));
    }

    /**
     * A library folder as the dialog names it: the folder's own name, and where it is - the
     * storage it is on, then the folders it sits in - so {@code /storage/emulated/0/Download/Books}
     * and {@code primary:Download/Books} handed over through the picker both read "Books" in
     * "Internal storage › Download". A whole storage is named by the storage, with its path under
     * it.
     *
     * @return name, place, and the path when the folder is a whole storage (unused by the row)
     */
    static String[] folderNameAndPlace(Context c, String path) {
        final String internal = c.getString(R.string.internal_storage);
        String storage;
        String inside;
        String whole;
        if (ExtUtils.isExteralSD(path)) {
            String id = ExtUtils.getExtSDDisplayName(c, path);
            int colon = id.indexOf(':');
            if (id.startsWith("content:") || colon < 0) {
                return new String[]{id, "", null};
            }
            String volume = id.substring(0, colon);
            storage = "primary".equals(volume) ? internal : volume;
            inside = id.substring(colon + 1);
            whole = "";
        } else {
            String root = Environment.getExternalStorageDirectory().getPath();
            if (path.equals(root) || path.startsWith(root + "/")) {
                storage = internal;
                inside = path.substring(root.length());
            } else if (path.startsWith("/storage/") && path.length() > "/storage/".length()) {
                String rest = path.substring("/storage/".length());
                int slash = rest.indexOf('/');
                storage = slash < 0 ? rest : rest.substring(0, slash);
                inside = slash < 0 ? "" : rest.substring(slash);
            } else {
                File file = new File(path);
                String parentPath = file.getParent();
                return new String[]{TxtUtils.isEmpty(file.getName()) ? path : file.getName(),
                        parentPath == null ? "" : parentPath, null};
            }
            whole = path;
        }
        List<String> parts = new ArrayList<>();
        for (String part : inside.split("/")) {
            if (part.length() > 0) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            return new String[]{storage, whole, whole};
        }
        StringBuilder place = new StringBuilder(storage);
        for (int i = 0; i < parts.size() - 1; i++) {
            place.append(" \u203A ").append(parts.get(i));
        }
        return new String[]{parts.get(parts.size() - 1), place.toString(), null};
    }

    public static void importDialog(final FragmentActivity activity) {
        String sampleName = ExportSettingsManager.getSampleJsonConfigName(activity, EXPORT_BACKUP_ZIP);

        ChooserDialogFragment.chooseFile(activity, sampleName).setOnSelectListener(new ResultResponse2<String, Dialog>() {

            @Override
            public boolean onResultRecive(String result1, Dialog result2) {

                new AsyncProgressResultToastTask(activity) {

                    @Override
                    protected Boolean doInBackground(Object... objects) {
                        try {
                            if (result1.endsWith(".json") || result1.endsWith(".txt")) {
                                ExportConverter.covertJSONtoNew(activity, new File(result1));
                            } else if (result1.endsWith(EXPORT_BACKUP_ZIP)) {
                                ExportConverter.unZipFolder(new File(result1), AppProfile.SYNC_FOLDER_ROOT);
                            } else {
                                return false;
                            }
                            return true;
                        } catch (Exception e) {
                            LOG.e(e);
                            return false;

                        }

                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);
                        result2.dismiss();
                        if (result) {
                            AppProfile.clear();
                            //AppProfile.init(activity);
                            activity.finish();
                            MainTabs2.startActivity(activity, TempHolder.get().currentTab);
                        }

                    }
                }.execute();


                return false;
            }

        });

    }

    public static boolean isBookSeriviceIsRunning(Activity a) {
        if (BooksService.isRunning) {
            Toast.makeText(a, R.string.please_wait_books_are_being_processed_, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    public static void migrationDialog(final FragmentActivity a) {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Dips.DP_5, Dips.DP_5, Dips.DP_5, Dips.DP_5);


        root.addView(UI.text(a, "-----------------------", Color.RED));
        root.addView(UI.text(a, a.getString(R.string.please_backup1), Color.RED));
        root.addView(UI.text(a, "-----------------------", Color.RED));


        final EditText from = new EditText(a);
        from.setWidth(Dips.DP_150);
        from.setText("/storage/XXXX-AAAA/");
        from.setSingleLine();


        final EditText to = new EditText(a);
        to.setWidth(Dips.DP_150);
        to.setText("/storage/XXXX-BBBB/");
        to.setSingleLine();

        root.addView(UI.bText(a, a.getString(R.string.old_path)));


        List<SimpleMeta> allRecent = AppData.get().getAllRecentSimple();
        Set<String> allFrom = new HashSet<>();

        for (SimpleMeta it : allRecent) {
            String path = it.path;
            int endIndex = path.indexOf("/", 15);
            if (endIndex != -1) {
                String rel = path.substring(0, endIndex);
                allFrom.add(rel + "/");
            }
        }

        for (String s : allFrom) {
            if (TxtUtils.isNotEmpty(s)) {
                TextView t = UI.uText(a, s);
                t.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        from.setText(s);
                    }
                });
                root.addView(t);
            }

        }
        root.addView(from);
        root.addView(UI.bText(a, a.getString(R.string.new_path)));
        List<String> allExternal = ExtUtils.getAllExternalStorages(a);
        allExternal.add(0, MyPath.INTERNAL_PREFIX);
        for (String s : allExternal) {
            if (TxtUtils.isNotEmpty(s)) {
                final String s1 = s + "/";
                TextView t = UI.uText(a, s1);
                t.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        to.setText(s1);
                    }
                });
                root.addView(t);
            }
        }
        root.addView(to);


        TextView r = UI.button(a, a.getString(R.string.start_migration));
        r.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String fromPath = from.getText().toString();
                String toPath = to.getText().toString();

                AsyncProgressTask<Boolean> task = new AsyncProgressTask<Boolean>() {
                    @Override
                    public Context getContext() {
                        return a;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);
                        TempHolder.listHash++;
                        EventBus.getDefault().post(new UpdateAllFragments());
                        if (result) {
                            Toast.makeText(a, R.string.success, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(a, R.string.fail, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    protected Boolean doInBackground(Object... objects) {
                        try {
                            final List<File> allFiles = AppProfile.getAllFiles(AppProfile.APP_RECENT_JSON);
                            for (File file : allFiles) {
                                JSONArray array = new JSONArray(IO.readString(file));
                                boolean hasChanges = false;
                                for (int i = 0; i < array.length(); i++) {
                                    final LinkedJSONObject it = array.getJSONObject(i);
                                    String path = it.getString(SimpleMeta.JSON_PATH);
                                    if (path.contains(fromPath)) {
                                        LOG.d("Migration-recent path-1", path);
                                        path = path.replace(fromPath, toPath);
                                        it.put(SimpleMeta.JSON_PATH, path);
                                        hasChanges = true;
                                        LOG.d("Migration-recent path-2", path);
                                    }

                                }
                                if (hasChanges) {
                                    IO.writeString(file, array.toString());
                                    LOG.d("Migration-recent write", file);
                                }
                            }
                        } catch (Exception e) {
                            return false;
                        }

                        try {
                            List<File> allFiles = AppProfile.getAllFiles(AppProfile.APP_BOOKMARKS_JSON);
                            for (File file : allFiles) {
                                LinkedJSONObject obj = IO.readJsonObject(file);
                                final Iterator<String> keys = obj.keys();
                                boolean hasChanges = false;
                                while (keys.hasNext()) {
                                    final String next = keys.next();
                                    final LinkedJSONObject it = obj.getJSONObject(next);

                                    String path = it.getString(SimpleMeta.JSON_PATH);
                                    if (path.startsWith(fromPath)) {
                                        LOG.d("Migration-BOOKMARK path-1", path);
                                        path = path.replace(fromPath, toPath);
                                        it.put(SimpleMeta.JSON_PATH, path);
                                        hasChanges = true;
                                        LOG.d("Migration-BOOKMARK path-2", path);
                                    }
                                }
                                if (hasChanges) {
                                    IO.writeString(file, obj.toString());
                                    LOG.d("Migration-BOOKMARK write ", file);
                                }

                            }
                        } catch (Exception e) {
                            return false;
                        }


                        return true;
                    }
                };
                task.execute();


            }
        });
        root.addView(UI.text(a, ""));
        root.addView(r);

        AlertDialogs.showViewDialog(a, root);

    }

    public static void exportDialog(final FragmentActivity activity) {
        String sampleName = ExportSettingsManager.getSampleJsonConfigName(activity, EXPORT_BACKUP_ZIP);
        ChooserDialogFragment.createFile(activity, sampleName).setOnSelectListener(new ResultResponse2<String, Dialog>() {

            @Override
            public boolean onResultRecive(String result1, Dialog result2) {
                File toFile = new File(result1);
                AppState.get().save(activity);

                new AsyncProgressResultToastTask(activity) {
                    @Override
                    protected Boolean doInBackground(Object... objects) {
                        try {
                            ExportConverter.zipFolder(AppProfile.SYNC_FOLDER_ROOT, toFile);
                            return true;
                        } catch (ZipException e) {
                            return false;
                        } finally {
                            activity.runOnUiThread(() -> result2.dismiss());
                        }
                    }
                }.execute();


                return false;
            }
        });

    }

    private void imExDialog(final Activity a, final int resSelectId, final String sampleName, final ResultResponse<File> onResult) {
        if (isBookSeriviceIsRunning(a)) {
            return;
        }
        List<String> browseexts = Arrays.asList(".json", ".txt", ".ttf", ".otf");

        final BrowserAdapter adapter = new BrowserAdapter(a, new ExtFilter(browseexts));
        adapter.setCurrentDirectory(new File(getLastPath()));

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.choose_);

        final EditText text = new EditText(a);

        text.setText(sampleName);
        int p = Dips.dpToPx(5);
        text.setPadding(p, p, p, p);
        text.setSingleLine();
        text.setEllipsize(TruncateAt.END);
        if (resSelectId == R.string.export_) {
            text.setEnabled(true);
        } else {
            text.setEnabled(false);
        }

        final TextView pathText = new TextView(a);
        pathText.setText(new File(getLastPath()).toString());
        pathText.setPadding(p, p, p, p);
        pathText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pathText.setTextSize(16);
        pathText.setSingleLine();
        pathText.setEllipsize(TruncateAt.END);

        final ListView list = new ListView(a);
        list.setMinimumHeight(1000);
        list.setMinimumWidth(600);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final File file = new File(adapter.getItem(position).getPath());
                if (file.isDirectory()) {
                    lastPaht = file.getPath();
                    adapter.setCurrentDirectory(file);
                    pathText.setText(file.getPath());
                    list.setSelection(0);
                } else {
                    pathText.setText(file.getName());
                }
            }
        });

        LinearLayout inflate = (LinearLayout) LayoutInflater.from(a).inflate(R.layout.frame_layout, null, false);

        list.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
        pathText.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));

        ImageView home = new ImageView(a);
        home.setImageResource(R.drawable.glyphicons_21_home);
        TintUtil.setTintImageWithAlpha(home);
        home.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final File file = Environment.getExternalStorageDirectory();
                if (file.isDirectory()) {
                    lastPaht = file.getPath();
                    adapter.setCurrentDirectory(file);
                    pathText.setText(file.getPath());
                    list.setSelection(0);
                } else {
                    pathText.setText(file.getName());
                }

            }
        });

        inflate.addView(home);
        inflate.addView(pathText);
        inflate.addView(list);
        inflate.addView(text);
        builder.setView(inflate);

        builder.setPositiveButton(resSelectId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                File toFile = null;
                if (resSelectId == R.string.export_) {
                    toFile = new File(lastPaht, text.getText().toString());
                } else {
                    toFile = new File(lastPaht, pathText.getText().toString());
                }
                onResult.onResultRecive(toFile);
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void selectFileDialog(final Context a, List<String> browseexts, File path, final com.foobnix.android.utils.ResultResponse<String> onChoose) {

        final BrowserAdapter adapter = new BrowserAdapter(a, new ExtFilter(browseexts));
        if (path.isFile()) {
            String absolutePath = path.getAbsolutePath();
            String filePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
            adapter.setCurrentDirectory(new File(filePath));
        } else {
            adapter.setCurrentDirectory(path);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.choose_);

        final EditText text = new EditText(a);

        text.setText(path.getName());
        int p = Dips.dpToPx(5);
        text.setPadding(p, p, p, p);
        text.setSingleLine();
        text.setEllipsize(TruncateAt.END);
        text.setEnabled(true);

        final TextView pathText = new TextView(a);
        pathText.setText(path.getPath());
        pathText.setPadding(p, p, p, p);
        pathText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pathText.setTextSize(16);
        pathText.setSingleLine();
        pathText.setEllipsize(TruncateAt.END);

        final ListView list = new ListView(a);
        list.setMinimumHeight(1000);
        list.setMinimumWidth(600);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final File file = new File(adapter.getItem(position).getPath());
                if (file.isDirectory()) {
                    adapter.setCurrentDirectory(file);
                    pathText.setText(file.getPath());
                    list.setSelection(0);
                } else {
                    text.setText(file.getName());
                }
            }
        });

        LinearLayout inflate = (LinearLayout) LayoutInflater.from(a).inflate(R.layout.frame_layout, null, false);

        list.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
        pathText.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));

        inflate.addView(pathText);
        inflate.addView(list);
        inflate.addView(text);
        builder.setView(inflate);

        builder.setPositiveButton(R.string.select, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = text.getText().toString();
                if (name == null || name.trim().length() == 0) {
                    Toast.makeText(a, "Invalid File name", Toast.LENGTH_SHORT).show();
                    return;
                }
                File toFile = new File(adapter.getCurrentDirectory(), name);

                onChoose.onResultRecive(toFile.getAbsolutePath());
                dialog.dismiss();

            }
        });

    }

}
