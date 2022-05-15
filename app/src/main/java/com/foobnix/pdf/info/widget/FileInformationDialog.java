package com.foobnix.pdf.info.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.BuildConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.ScaledImageView;
import com.foobnix.pdf.search.activity.msg.NotifyAllFragments;
import com.foobnix.pdf.search.view.AsyncProgressResultToastTask;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppDB.SEARCH_IN;
import com.foobnix.ui2.AppDB.SORT_BY;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.ebookdroid.BookType;
import org.ebookdroid.LibreraApp;
import org.ebookdroid.core.codec.CodecDocument;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileInformationDialog {

    static AlertDialog infoDialog;

    public static String showKeys(String line) {
        if (TxtUtils.isEmpty(line)) {
            return "";
        }
        String lines[] = line.split("[,;]");
        List<String> res = new ArrayList<String>();
        for (String it : lines) {
            if (TxtUtils.isNotEmpty(it)) {
                res.add(TxtUtils.firstUppercase(it.trim()));
            }
        }
        Collections.sort(res);

        return TxtUtils.joinList("\n", res);
    }

    public static void showFileInfoDialog(final Activity a, final File file, final Runnable onDeleteAction) {
        showFileInfoDialog(a, file, onDeleteAction, true);
    }

    public static void showFileInfoDialog(final Activity a, final File file, final Runnable onDeleteAction, boolean firstTime) {
        ADS.hideAdsTemp(a);

        final FileMeta fileMeta = AppDB.get().getOrCreate(file.getPath());


        LOG.d("FileMeta-State", fileMeta.getState(),fileMeta.getTitle());


        if (firstTime && TxtUtils.isEmpty(fileMeta.getTitle())) {
            new AsyncProgressResultToastTask(a, result -> {
                showFileInfoDialog(a, file, onDeleteAction, false);
                return false;
            }) {
                @Override
                protected Boolean doInBackground(Object... objects) {
                    FileMetaCore.reUpdateIfNeed(fileMeta);
                    return true;
                }
            }.execute();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(a);


        final View dialog = LayoutInflater.from(a).inflate(R.layout.dialog_file_info, null, false);

        final ImageView image = (ImageView) dialog.findViewById(R.id.cloudImage);
        boolean isCloud = Clouds.showHideCloudImage(image, fileMeta.getPath());
        if (!isCloud) {
            // image.setVisibility(View.VISIBLE);
            image.setImageResource(R.drawable.glyphicons_41_cloud_plus);
            TintUtil.setTintImageWithAlpha(image);
            image.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ShareDialog.showAddToCloudDialog(a, new File(fileMeta.getPath()));

                }
            });
        }

        TextView title = (TextView) dialog.findViewById(R.id.title);
        TextView author = (TextView) dialog.findViewById(R.id.author);
        TextView year = (TextView) dialog.findViewById(R.id.year);

        final TextView bookmarks = (TextView) dialog.findViewById(R.id.bookmarks);
        final TextView bookmarksSection = (TextView) dialog.findViewById(R.id.bookmarksSection);

        title.setText(fileMeta.getTitle());
        if (TxtUtils.isNotEmpty(fileMeta.getAuthor())) {
            author.setText(showKeys(fileMeta.getAuthor()));
        }

        year.setText("" + TxtUtils.nullToEmpty(fileMeta.getYear()));

        TextView pathView = (TextView) dialog.findViewById(R.id.path);
        pathView.setText(file.getPath());
        if (BuildConfig.DEBUG) {
            pathView.setText(file.getPath() + "\n" + LOG.ojectAsString(fileMeta));
        }


        ((TextView) dialog.findViewById(R.id.date)).setText(fileMeta.getDateTxt());
        ((TextView) dialog.findViewById(R.id.info)).setText(fileMeta.getExt());

        ((TextView) dialog.findViewById(R.id.publisher)).setText(fileMeta.getPublisher());
        ((TextView) dialog.findViewById(R.id.isbn)).setText(showKeys(fileMeta.getIsbn()));

        if (fileMeta.getPages() != null && fileMeta.getPages() != 0) {
            ((TextView) dialog.findViewById(R.id.size)).setText(fileMeta.getSizeTxt() + " (" + fileMeta.getPages() + ")");
        } else {
            ((TextView) dialog.findViewById(R.id.size)).setText(fileMeta.getSizeTxt());
        }

        ((TextView) dialog.findViewById(R.id.mimeType)).setText("" + ExtUtils.getMimeType(file));

        final TextView hypenLang = (TextView) dialog.findViewById(R.id.hypenLang);
        hypenLang.setText(DialogTranslateFromTo.getLanuageByCode(fileMeta.getLang()));
        if (fileMeta.getLang() == null) {
            ((View) hypenLang.getParent()).setVisibility(View.GONE);
        }

        List<AppBookmark> objects = BookmarksData.get().getBookmarksByBook(file);
        StringBuilder lines = new StringBuilder();
        String fast = a.getString(R.string.fast_bookmark);
        if (TxtUtils.isListNotEmpty(objects)) {
            for (AppBookmark b : objects) {
                if (!fast.equals(b.getText())) {
                    lines.append(b.getText());
                    lines.append("\n");
                }
            }
        }
        bookmarks.setText(TxtUtils.replaceLast(lines.toString(), "\n", ""));
        if (TxtUtils.isListEmpty(objects)) {
            bookmarksSection.setVisibility(View.GONE);
        }

        bookmarks.setOnClickListener(v -> bookmarks.setMaxLines(Integer.MAX_VALUE));

        final TextView infoView = (TextView) dialog.findViewById(R.id.metaInfo);
        final TextView expand = (TextView) dialog.findViewById(R.id.expand);
        String bookOverview = FileMetaCore.getBookOverview(file.getPath());
        infoView.setText(TxtUtils.nullToEmpty(bookOverview));

        expand.setVisibility(TxtUtils.isNotEmpty(bookOverview) && bookOverview.length() > 200 ? View.VISIBLE : View.GONE);

        expand.setOnClickListener(v -> {
            // AlertDialogs.showOkDialog(a, infoView.getText().toString(), null);
            infoView.setMaxLines(Integer.MAX_VALUE);
            infoView.setTextIsSelectable(true);
            expand.setVisibility(View.GONE);
        });

        String sequence = fileMeta.getSequence();
        if (TxtUtils.isNotEmpty(sequence)) {
            sequence = sequence.replace(",", "");
            final TextView metaSeries = (TextView) dialog.findViewById(R.id.metaSeries);

            if (fileMeta.getSIndex() != null && fileMeta.getSIndex() > 0) {
                sequence = sequence + ", " + fileMeta.getSIndex();
            }

            metaSeries.setText(sequence);
            List<FileMeta> result = AppDB.get().searchBy(SEARCH_IN.SERIES.getDotPrefix() + " " + fileMeta.getSequence(), SORT_BY.SERIES_INDEX, true);
            if (TxtUtils.isListNotEmpty(result) && result.size() > 1) {

                RecyclerView recyclerView = dialog.findViewById(R.id.recycleViewSeries);
                recyclerView.setVisibility(View.VISIBLE);
                //recyclerView.setHasFixedSize(true);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(a);
                linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                recyclerView.setLayoutManager(linearLayoutManager);

                FileMetaAdapter adapter = new FileMetaAdapter();
                adapter.tempValue = FileMetaAdapter.TEMP_VALUE_SERIES;
                adapter.setAdapterType(FileMetaAdapter.ADAPTER_GRID);
                adapter.getItemsList().addAll(result);
                recyclerView.setAdapter(adapter);

                DefaultListeners.bindAdapter(a, adapter);
                adapter.setOnItemLongClickListener(result1 -> true);
            }
        } else {
            ((TextView) dialog.findViewById(R.id.metaSeries)).setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.metaSeriesID)).setVisibility(View.GONE);
        }

        String genre = fileMeta.getGenre();
        if (TxtUtils.isNotEmpty(genre)) {
            final TextView metaGenre = (TextView) dialog.findViewById(R.id.metaGenre);
            metaGenre.setText(showKeys(genre));

        } else {
            ((TextView) dialog.findViewById(R.id.metaGenre)).setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.metaGenreID)).setVisibility(View.GONE);
        }

        if (TxtUtils.isNotEmpty(fileMeta.getKeyword())) {
            final TextView metaKeys = (TextView) dialog.findViewById(R.id.keywordList);
            metaKeys.setText(showKeys(fileMeta.getKeyword()));
        } else {
            ((TextView) dialog.findViewById(R.id.keywordID)).setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.keywordList)).setVisibility(View.GONE);
        }

        final Runnable tagsRunnable = () -> {
            String tag = fileMeta.getTag();
            if (TxtUtils.isNotEmpty(tag)) {
                String replace = tag.replace("#", " ");
                replace = TxtUtils.replaceLast(replace, ",", "").trim();
                ((TextView) dialog.findViewById(R.id.tagsList)).setText(replace);
                // ((TextView) dialog.findViewById(R.id.tagsID)).setVisibility(View.VISIBLE);
                ((TextView) dialog.findViewById(R.id.tagsList)).setVisibility(View.VISIBLE);
            } else {
                // ((TextView) dialog.findViewById(R.id.tagsID)).setVisibility(View.GONE);
                ((TextView) dialog.findViewById(R.id.tagsList)).setVisibility(View.GONE);
            }
        };
        tagsRunnable.run();

        TxtUtils.underlineTextView(dialog.findViewById(R.id.addTags)).setOnClickListener(v ->
                Dialogs.showTagsDialog(a, new File(fileMeta.getPath()), false, () -> {
                    tagsRunnable.run();
                    EventBus.getDefault().post(new NotifyAllFragments());
                }));

        TextView metaTags = (TextView) dialog.findViewById(R.id.metaTags);
        TextView metaTagsInfo = (TextView) dialog.findViewById(R.id.metaTagsInfo);
        metaTags.setVisibility(View.GONE);
        metaTagsInfo.setVisibility(View.GONE);
        if (BookType.PDF.is(file.getPath()) || BookType.DJVU.is(file.getPath())) {
            metaTags.setVisibility(View.VISIBLE);
            metaTagsInfo.setVisibility(View.VISIBLE);

            final List<String> list2 = new ArrayList<String>(Arrays.asList(
                    //
                    "info:Title", //
                    "info:Author", //
                    "info:Subject", //
                    "info:Keywords", //
                    "info:Creator", //
                    "info:Producer", //
                    "info:CreationDate", //
                    "info:ModDate", //
                    "info:Edition", //
                    "info:Trapped" //
            ));

            try {
                final CodecDocument doc = ImageExtractor.singleCodecContext(file.getPath(), "", 0, 0);

                if (BookType.DJVU.is(file.getPath())) {
                    list2.addAll(doc.getMetaKeys());
                }

                StringBuilder meta = new StringBuilder();
                for (String id : list2) {
                    String metaValue = doc.getMeta(id);

                    if (TxtUtils.isNotEmpty(metaValue)) {
                        id = id.replace("info:", "");
                        meta.append("<b>" + id).append(": " + "</b>").append(metaValue).append("<br>");
                    }
                }
                doc.recycle();

                String text = TxtUtils.replaceLast(meta.toString(), "<br>", "");
                metaTagsInfo.setText(Html.fromHtml(text));

            } catch (Exception e) {
                LOG.e(e);
            }
        }

        TextView convertFile = (TextView) dialog.findViewById(R.id.convertFile);
        convertFile.setOnClickListener(v -> ShareDialog.showsItemsDialog(a, file.getPath(), AppState.CONVERTERS.get("EPUB")));
        convertFile.setText(TxtUtils.underline(a.getString(R.string.convert_to) + " EPUB"));
        convertFile.setVisibility(ExtUtils.isImageOrEpub(file) ? View.GONE : View.VISIBLE);
        convertFile.setVisibility(View.GONE);

        TxtUtils.underlineTextView(dialog.findViewById(R.id.openWith)).setOnClickListener(v -> {
            if (infoDialog != null) {
                infoDialog.dismiss();
                infoDialog = null;
            }
            ExtUtils.openWith(a, file);
        });

        TxtUtils.underlineTextView(dialog.findViewById(R.id.sendFile)).setOnClickListener(v -> {
            if (infoDialog != null) {
                infoDialog.dismiss();
                infoDialog = null;
            }
            ExtUtils.sendFileTo(a, file);
        });

        TextView delete = TxtUtils.underlineTextView(dialog.findViewById(R.id.delete));
        if (onDeleteAction == null) {
            delete.setVisibility(View.GONE);
        }
        delete.setOnClickListener(v -> {
            if (infoDialog != null) {
                infoDialog.dismiss();
                infoDialog = null;
            }

            dialogDelete(a, file, onDeleteAction);
        });

        View openFile = dialog.findViewById(R.id.openFile);
        // openFile.setVisibility(ExtUtils.isNotSupportedFile(file) ? View.GONE :
        // View.VISIBLE);
        openFile.setOnClickListener(v -> {
            if (infoDialog != null) {
                infoDialog.dismiss();
                infoDialog = null;
            }
            ExtUtils.showDocumentWithoutDialog2(a, file);
        });

        final ImageView coverImage = (ImageView) dialog.findViewById(R.id.image);
        coverImage.getLayoutParams().width = Dips.screenMinWH() / 2;

        IMG.getCoverPage(coverImage, file.getPath(), IMG.getImageSize());

        coverImage.setOnClickListener(v -> showImage(a, file.getPath()));

        // IMG.updateImageSizeBig(coverImage);

        final ImageView starIcon = ((ImageView) dialog.findViewById(R.id.starIcon));

        starIcon.setOnClickListener(v -> {
            DefaultListeners.getOnStarClick(a).onResultRecive(fileMeta, null);

            if (fileMeta.getIsStar() == null || !fileMeta.getIsStar()) {
                starIcon.setImageResource(R.drawable.star_2);
            } else {
                starIcon.setImageResource(R.drawable.star_1);
            }
            TintUtil.setTintImageNoAlpha(starIcon, TintUtil.getColorInDayNighth());
        });

        if (fileMeta.getIsStar() == null || !fileMeta.getIsStar()) {
            starIcon.setImageResource(R.drawable.star_2);
        } else {
            starIcon.setImageResource(R.drawable.star_1);
        }

        TintUtil.setTintImageNoAlpha(starIcon, TintUtil.getColorInDayNighth());

        TintUtil.setBackgroundFillColor(openFile, TintUtil.color);


        // builder.setTitle(R.string.file_info);
        builder.setView(dialog);

        builder.setNegativeButton(R.string.close, (dialog1, id) -> {});

        builder.setPositiveButton(R.string.read_a_book, (dialog12, id) -> ExtUtils.showDocumentWithoutDialog2(a, file));

        infoDialog = builder.create();
        infoDialog.setOnDismissListener(dialog13 -> Keyboards.hideNavigation(a));

        infoDialog.show();
    }

    public static void showImage(Activity a, String path) {
        final Dialog builder = new Dialog(a);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(dialogInterface -> {
        });

        final ScaledImageView imageView = new ScaledImageView(a);
        imageView.setOnClickListener(v -> builder.dismiss());
        final String url = IMG.toUrl(path, -2, Dips.screenWidth());

        Glide.with(LibreraApp.context).asBitmap().load(url).into(imageView);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (Dips.screenWidth() * 0.9), (int) (Dips.screenHeight() * 0.9));
        builder.addContentView(imageView, params);
        builder.show();
    }

    public static void showImageHttpPath(Context a, String path) {
        final Dialog builder = new Dialog(a);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(dialogInterface -> {});

        final ScaledImageView imageView = new ScaledImageView(a);
        imageView.setOnClickListener(v -> builder.dismiss());
        Glide.with(LibreraApp.context).asBitmap().load(path).into(imageView);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (Dips.screenWidth() * 0.9), (int) (Dips.screenHeight() * 0.9));
        builder.addContentView(imageView, params);
        builder.show();
    }

    public static void dialogDelete(final Activity a, final File file, final Runnable onDeleteAction) {
        if (file == null || onDeleteAction == null) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        String name = file.getName();
        if (ExtUtils.isExteralSD(file.getPath())) {
            try {
                name = URLDecoder.decode(file.getName(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOG.e(e);
            }
        }

        builder.setMessage(a.getString(R.string.do_you_want_to_delete_this_file_) + "\n\"" + name + "\"").setPositiveButton(android.R.string.yes, (dialog, id) -> {
            if (Clouds.isLibreraSyncRootFolder(file.getPath())) {
                new AsyncProgressResultToastTask(a) {
                    @Override
                    protected Boolean doInBackground(Object... objects) {
                        try {
                            GFile.deleteRemoteFile(file);
                            a.runOnUiThread(onDeleteAction);
                            //GFile.runSyncService(a);
                        } catch (Exception e) {
                            LOG.e(e);
                            return false;
                        }
                        return true;
                    }
                }.execute();
            } else {
                onDeleteAction.run();
                AppData.get().removeRecent(new FileMeta(file.getPath()));
            }
        }).setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss());
        builder.show();
    }
}
