package com.foobnix.pdf.info.widget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.BookType;
import org.ebookdroid.ui.viewer.ViewerActivity;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentWrapperUI;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

public class ShareDialog {

    public static void showArchive(final Activity a, final File file, final Runnable onDeleteAction) {
        if (ExtUtils.isNotValidFile(file)) {
            Toast.makeText(a, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }

        List<String> items = new ArrayList<String>();

        if (!ExtUtils.isImageOrEpub(file)) {
            items.add(a.getString(R.string.convert_to) + " EPUB");
            items.add(a.getString(R.string.convert_to) + " PDF");
        }
        items.add(a.getString(R.string.open_with));
        items.add(a.getString(R.string.send_file));
        items.add(a.getString(R.string.delete));
        items.add(a.getString(R.string.file_info));

        final String chooseTitle = file != null ? file.getPath() : a.getString(R.string.choose_);

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.choose_)//
                .setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        int i = 0;
                        if (!ExtUtils.isImageOrEpub(file)) {
                            if (which == i++) {
                                showsItemsDialog(a, chooseTitle, AppState.CONVERTERS.get("EPUB"));
                            }
                            if (which == i++) {
                                showsItemsDialog(a, chooseTitle, AppState.CONVERTERS.get("PDF"));
                            }
                        }
                        if (which == i++) {
                            ExtUtils.openWith(a, file);
                        } else if (which == i++) {
                            ExtUtils.sendFileTo(a, file);
                        } else if (which == i++) {
                            FileInformationDialog.dialogDelete(a, file, onDeleteAction);
                        } else if (which == i++) {
                            FileInformationDialog.showFileInfoDialog(a, file, onDeleteAction);
                        }

                    }
                });
        builder.show();
    };

    public static void showsItemsDialog(final Activity a, String title, final String[] items) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(title)//
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        Urls.open(a, items[which]);
                    }
                });

        AlertDialog dialog = builder.show();

    }

    public static void show(final Activity a, final File file, final Runnable onDeleteAction, final int page, final DocumentWrapperUI ui) {
        if (ExtUtils.isNotValidFile(file)) {
            Toast.makeText(a, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        final boolean isPDF = BookType.PDF.is(file.getPath());
        final boolean isLibrary = false;// a instanceof MainTabs2 ? false :
                                        // true;

        List<String> items = new ArrayList<String>();
        if (isPDF) {
            items.add(a.getString(R.string.make_text_reflow));
        }
        if (isLibrary) {
            items.add(a.getString(R.string.library));
        }
        items.add(a.getString(R.string.advanced_mode));
        items.add(a.getString(R.string.easy_mode));
        items.add(a.getString(R.string.music_mode));
        items.add(a.getString(R.string.open_with));
        items.add(a.getString(R.string.send_file));
        items.add(a.getString(R.string.export_bookmarks));
        items.add(a.getString(R.string.delete));
        items.add(a.getString(R.string.remove_from_library));
        items.add(a.getString(R.string.send_snapshot_of_the_page) + " " + (Math.max(page, 0) + 1) + "");
        items.add(a.getString(R.string.file_info));

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.choose_)//
                .setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        int i = 0;

                        if (isPDF && which == i++) {
                            ExtUtils.openPDFInTextReflow(a, file, page + 1);
                        }
                        if (isLibrary && which == i++) {
                            a.finish();
                            MainTabs2.startActivity(a, UITab.getCurrentTabIndex(UITab.SearchFragment));
                        }
                        if (which == i++) {
                            if (a instanceof ViewerActivity || a instanceof HorizontalViewActivity) {
                                a.finish();
                            }
                            AppState.get().isMusicianMode = false;
                            AppState.get().isAlwaysOpenAsMagazine = false;
                            ExtUtils.showDocumentWithoutDialog(a, file, page + 1);
                        } else if (which == i++) {
                            if (a instanceof ViewerActivity || a instanceof HorizontalViewActivity) {
                                a.finish();
                            }
                            AppState.get().isMusicianMode = false;
                            AppState.get().isAlwaysOpenAsMagazine = true;
                            ExtUtils.showDocumentWithoutDialog(a, file, page + 1);
                        } else if (which == i++) {
                            if (a instanceof ViewerActivity || a instanceof HorizontalViewActivity) {
                                a.finish();
                            }
                            AppState.get().isAlwaysOpenAsMagazine = false;
                            AppState.get().isMusicianMode = true;
                            if (ui != null) {
                                ui.initUI(a);
                                ui.showHide();
                            }
                            ExtUtils.showDocumentWithoutDialog(a, file, page);
                        } else if (which == i++) {
                            ExtUtils.openWith(a, file);
                        } else if (which == i++) {
                            ExtUtils.sendFileTo(a, file);
                        } else if (which == i++) {
                            ExtUtils.sendBookmarksTo(a, file);
                        } else if (which == i++) {
                            FileInformationDialog.dialogDelete(a, file, onDeleteAction);
                        } else if (which == i++) {
                            AppDB.get().deleteBy(file.getPath());
                            EventBus.getDefault().post(new UpdateAllFragments());
                        } else if (which == i++) {
                            ExtUtils.sharePage(a, file, page);
                        } else if (which == i++) {
                            FileInformationDialog.showFileInfoDialog(a, file, onDeleteAction);
                        }

                    }
                });
        builder.show();
    };

}
