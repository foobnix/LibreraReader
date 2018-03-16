package com.foobnix.pdf.info.widget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.BookType;
import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.DocumentWrapperUI;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
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

    public static void show(final Activity a, final File file, final Runnable onDeleteAction, final int page, final DocumentWrapperUI ui, final DocumentController dc) {
        if (!ExtUtils.isExteralSD(file.getPath()) && ExtUtils.isNotValidFile(file)) {
            Toast.makeText(a, R.string.file_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        final boolean isPDF = BookType.PDF.is(file.getPath());
        final boolean isLibrary = false;// a instanceof MainTabs2 ? false :
                                        // true;
        final boolean isMainTabs = a instanceof MainTabs2;

        List<String> items = new ArrayList<String>();

        if (isLibrary) {
            items.add(a.getString(R.string.library));
        }

        if (dc != null) {
            if (a instanceof VerticalViewActivity || AppState.get().isMusicianMode) {
                items.add(AppState.get().nameHorizontalMode);
            }
            if (a instanceof HorizontalViewActivity || AppState.get().isMusicianMode) {
                items.add(AppState.get().nameVerticalMode);
            }

            if (AppState.get().isMusicianMode == false) {
                items.add(AppState.get().nameMusicianMode);
            }
        }

        if (isPDF) {
            items.add(a.getString(R.string.make_text_reflow));
        }

        items.add(a.getString(R.string.open_with));
        items.add(a.getString(R.string.send_file));
        items.add(a.getString(R.string.export_bookmarks));
        if (isMainTabs) {
            items.add(a.getString(R.string.delete));
            items.add(a.getString(R.string.remove_from_library));
        }
        if (!isMainTabs) {
            items.add(a.getString(R.string.send_snapshot_of_the_page) + " " + (Math.max(page, 0) + 1) + "");
        }

        items.add(a.getString(R.string.tags));
        items.add(a.getString(R.string.file_info));

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        // builder.setTitle(R.string.choose_)//
        builder.setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                int i = 0;

                if (isLibrary && which == i++) {
                    a.finish();
                    MainTabs2.startActivity(a, UITab.getCurrentTabIndex(UITab.SearchFragment));
                }

                if (dc != null && (a instanceof HorizontalViewActivity || AppState.get().isMusicianMode) && which == i++) {
                    dc.onCloseActivityFinal(new Runnable() {

                        @Override
                        public void run() {
                            if (AppState.get().isMusicianMode) {
                                AppState.get().isAlwaysOpenAsMagazine = true;
                            } else {
                                AppState.get().isAlwaysOpenAsMagazine = false;
                            }
                            AppState.get().isMusicianMode = false;
                            ExtUtils.showDocumentWithoutDialog(a, file, page + 1);

                        }
                    });

                }
                if (dc != null && (a instanceof VerticalViewActivity || AppState.get().isMusicianMode) && which == i++) {
                    if (dc != null) {
                        dc.onCloseActivityFinal(new Runnable() {

                            @Override
                            public void run() {
                                if (AppState.get().isMusicianMode) {
                                    AppState.get().isAlwaysOpenAsMagazine = false;
                                } else {
                                    AppState.get().isAlwaysOpenAsMagazine = true;

                                }
                                AppState.get().isMusicianMode = false;
                                ExtUtils.showDocumentWithoutDialog(a, file, page + 1);
                            }
                        });
                    }
                }
                if (dc != null && AppState.get().isMusicianMode == false && which == i++) {
                    dc.onCloseActivityFinal(new Runnable() {

                        @Override
                        public void run() {
                            AppState.get().isMusicianMode = true;
                            AppState.get().isAlwaysOpenAsMagazine = false;
                            ExtUtils.showDocumentWithoutDialog(a, file, page + 1);
                        }
                    });
                }
                if (isPDF && which == i++) {
                    ExtUtils.openPDFInTextReflow(a, file, page + 1, dc);
                }

                if (which == i++) {
                    ExtUtils.openWith(a, file);
                } else if (which == i++) {
                    ExtUtils.sendFileTo(a, file);
                } else if (which == i++) {
                    ExtUtils.sendBookmarksTo(a, file);
                } else if (isMainTabs && which == i++) {
                    FileInformationDialog.dialogDelete(a, file, onDeleteAction);
                } else if (isMainTabs && which == i++) {
                    FileMeta load = AppDB.get().load(file.getPath());
                    if (load != null) {
                        load.setIsSearchBook(false);
                        AppDB.get().update(load);
                    }
                    EventBus.getDefault().post(new UpdateAllFragments());
                } else if (!isMainTabs && which == i++) {
                    if (dc != null) {
                        ExtUtils.sharePage(a, file, page, dc.getPageUrl(page).toString());
                    } else {
                        ExtUtils.sharePage(a, file, page, null);
                    }
                } else if (which == i++) {
                    Dialogs.showTagsDialog(a, file, null);
                } else if (which == i++) {
                    FileInformationDialog.showFileInfoDialog(a, file, onDeleteAction);
                }

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
    };

}
