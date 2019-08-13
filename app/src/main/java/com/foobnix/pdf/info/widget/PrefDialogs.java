package com.foobnix.pdf.info.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Typeface;
import android.net.Uri;
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
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExportConverter;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.ExtFilter;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.presentation.BrowserAdapter;
import com.foobnix.pdf.info.presentation.PathAdapter;
import com.foobnix.pdf.search.view.AsyncProgressResultToastTask;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.MainTabs2;

import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PrefDialogs {

    public static final String EXPORT_BACKUP_ZIP = "-export-backup.zip";
    private static String lastPaht;

    private static String getLastPath() {
        try {
            return lastPaht = lastPaht != null && new File(lastPaht).isDirectory() ? lastPaht : Environment.getExternalStorageDirectory().getPath();
        } catch (Exception e) {
            return lastPaht = "/";
        }
    }

    public static void chooseFolderDialog(final FragmentActivity a, final Runnable onChanges, final Runnable onScan) {

        final PathAdapter recentAdapter = new PathAdapter();
        recentAdapter.setPaths(BookCSS.get().searchPaths);

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.scan_device_for_new_books);

        final ListView list = new ListView(a);

        list.setAdapter(recentAdapter);

        builder.setView(list);

        builder.setPositiveButton(R.string.search, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                onScan.run();
            }
        });

        builder.setNeutralButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                ChooserDialogFragment.chooseFolder(a, BookCSS.get().dirLastPath).setOnSelectListener(new ResultResponse2<String, Dialog>() {
                    @Override
                    public boolean onResultRecive(String nPath, Dialog dialog) {

                        if (nPath.equals("/")) {
                            Toast.makeText(a, String.format("[ / ] %s", a.getString(R.string.incorrect_value)), Toast.LENGTH_LONG).show();
                            return false;
                        }
                        boolean isExists = false;
                        String existPath = "";
                        for (String str : BookCSS.get().searchPaths.split(",")) {
                            if (str != null && str.trim().length() != 0 && nPath.equals(str)) {
                                isExists = true;
                                existPath = str;
                                break;
                            }
                        }
                        if (ExtUtils.isExteralSD(nPath)) {
                            Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                        } else if (isExists) {
                            Toast.makeText(a, String.format("[ %s == %s ] %s", nPath, existPath, a.getString(R.string.this_directory_is_already_in_the_list)), Toast.LENGTH_LONG).show();
                        } else {
                            if (BookCSS.get().searchPaths.endsWith(",")) {
                                BookCSS.get().searchPaths = BookCSS.get().searchPaths + "" + nPath;
                            } else {
                                BookCSS.get().searchPaths = BookCSS.get().searchPaths + "," + nPath;
                            }
                        }
                        dialog.dismiss();
                        onChanges.run();
                        chooseFolderDialog(a, onChanges, onScan);
                        return false;
                    }

                });

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        recentAdapter.setOnDeleClick(new ResultResponse<Uri>() {

            @Override
            public boolean onResultRecive(Uri result) {
                String path = result.getPath();
                LOG.d("TEST", "Remove " + BookCSS.get().searchPaths);
                LOG.d("TEST", "Remove " + path);
                StringBuilder builder = new StringBuilder();
                for (String str : BookCSS.get().searchPaths.split(",")) {
                    if (str != null && str.trim().length() > 0 && !str.equals(path)) {
                        builder.append(str);
                        builder.append(",");
                    }
                }
                BookCSS.get().searchPaths = builder.toString();
                LOG.d("TEST", "Remove " + BookCSS.get().searchPaths);
                recentAdapter.setPaths(BookCSS.get().searchPaths);
                onChanges.run();
                return false;
            }
        });

        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(a);
            }
        });
        create.show();
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
