package com.foobnix.pdf.info.widget;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecDocument;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.ScaledImageView;
import com.foobnix.pdf.info.wrapper.AppBookmark;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FileInformationDialog {

    static AlertDialog infoDialog;

    public static void showFileInfoDialog(final Activity a, final File file, final Runnable onDeleteAction) {

        AlertDialog.Builder builder = new AlertDialog.Builder(a);

        final FileMeta fileMeta = AppDB.get().getOrCreate(file.getPath());

        View dialog = LayoutInflater.from(a).inflate(R.layout.dialog_file_info, null, false);

        TextView title = (TextView) dialog.findViewById(R.id.title);
        final TextView bookmarks = (TextView) dialog.findViewById(R.id.bookmarks);

        title.setText(TxtUtils.getFileMetaBookName(fileMeta));

        ((TextView) dialog.findViewById(R.id.path)).setText(file.getPath());
        ((TextView) dialog.findViewById(R.id.date)).setText(fileMeta.getDateTxt());
        ((TextView) dialog.findViewById(R.id.mimeExt)).setText(fileMeta.getExt());
        ((TextView) dialog.findViewById(R.id.size)).setText(fileMeta.getSizeTxt());
        ((TextView) dialog.findViewById(R.id.mimeType)).setText("" + ExtUtils.getMimeType(file));

        final TextView hypenLang = (TextView) dialog.findViewById(R.id.hypenLang);
        hypenLang.setText(DialogTranslateFromTo.getLanuageByCode(fileMeta.getLang()));
        if (fileMeta.getLang() == null) {
            ((View) hypenLang.getParent()).setVisibility(View.GONE);
        }

        List<AppBookmark> objects = AppSharedPreferences.get().getBookmarksByBook(file);
        StringBuilder lines = new StringBuilder();
        String fast = a.getString(R.string.fast_bookmark);
        if (TxtUtils.isListNotEmpty(objects)) {
            for (AppBookmark b : objects) {
                if (!fast.equals(b.getText())) {
                    lines.append(b.getPage() + ": " + b.getText());
                    lines.append("\n");
                }
            }
        }
        bookmarks.setText(TxtUtils.replaceLast(lines.toString(), "\n", ""));

        bookmarks.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AlertDialogs.showOkDialog(a, bookmarks.getText().toString(), null);
            }
        });

        final TextView infoView = (TextView) dialog.findViewById(R.id.metaInfo);
        String bookOverview = FileMetaCore.getBookOverview(file.getPath());
        infoView.setText(TxtUtils.nullToEmpty(bookOverview));

        infoView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AlertDialogs.showOkDialog(a, infoView.getText().toString(), null);
            }
        });

        String sequence = fileMeta.getSequence();
        if (TxtUtils.isNotEmpty(sequence)) {
            String replace = sequence.replaceAll(",$", "").replace(",", " / ");
            ((TextView) dialog.findViewById(R.id.metaSeries)).setText(replace);
        } else {
            ((TextView) dialog.findViewById(R.id.metaSeries)).setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.metaSeriesID)).setVisibility(View.GONE);
        }

        String genre = fileMeta.getGenre();
        if (TxtUtils.isNotEmpty(genre)) {
            genre = TxtUtils.firstUppercase(genre.replaceAll(",$", "").replace(",", " / "));
            ((TextView) dialog.findViewById(R.id.metaGenre)).setText(genre);
        } else {
            ((TextView) dialog.findViewById(R.id.metaGenre)).setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.metaGenreID)).setVisibility(View.GONE);
        }

        String tag = fileMeta.getTag();
        if (TxtUtils.isNotEmpty(tag)) {
            String replace = tag.replace("#", " ");
            replace = TxtUtils.replaceLast(replace, ",", "").trim();
            ((TextView) dialog.findViewById(R.id.tagsList)).setText(replace);
        } else {
            ((TextView) dialog.findViewById(R.id.tagsID)).setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.tagsList)).setVisibility(View.GONE);
        }

        TextView metaPDF = (TextView) dialog.findViewById(R.id.metaPDF);
        metaPDF.setVisibility(View.GONE);
        if (BookType.PDF.is(file.getPath())) {
            CodecDocument doc = ImageExtractor.singleCodecContext(file.getPath(), "", 0, 0);
            if (doc != null) {
                metaPDF.setVisibility(View.VISIBLE);
                StringBuilder meta = new StringBuilder();
                List<String> list = Arrays.asList(
                        //
                        "info:Title", //
                        "info:Author", //
                        "info:Subject", //
                        "info:Keywords" //
                // "info:Creator", //
                // "info:Producer" //
                // "info:CreationDate", //
                // "info:ModDate"//
                );
                for (String id : list) {
                    String metaValue = doc.getMeta(id);
                    if (TxtUtils.isNotEmpty(metaValue)) {
                        id = id.replace("info:Title", a.getString(R.string.title));
                        id = id.replace("info:Author", a.getString(R.string.author));
                        id = id.replace("info:Subject", a.getString(R.string.subject));
                        id = id.replace("info:Keywords", a.getString(R.string.keywords));
                        meta.append("<b>" + id).append(": " + "</b>").append(metaValue).append("<br>");
                    }
                }
                doc.recycle();
                String text = TxtUtils.replaceLast(meta.toString(), "<br>", "");
                metaPDF.setText(Html.fromHtml(text));
            }
        }

        TextView convertFile = (TextView) dialog.findViewById(R.id.convertFile);
        convertFile.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ShareDialog.showsItemsDialog(a, file.getPath(), AppState.CONVERTERS.get("EPUB"));
            }
        });
        convertFile.setText(TxtUtils.underline(a.getString(R.string.convert_to) + " EPUB"));
        convertFile.setVisibility(ExtUtils.isImageOrEpub(file) ? View.GONE : View.VISIBLE);
        convertFile.setVisibility(View.GONE);

        TxtUtils.underlineTextView((TextView) dialog.findViewById(R.id.openWith)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (infoDialog != null) {
                    infoDialog.dismiss();
                    infoDialog = null;
                }
                ExtUtils.openWith(a, file);

            }
        });

        TxtUtils.underlineTextView((TextView) dialog.findViewById(R.id.sendFile)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (infoDialog != null) {
                    infoDialog.dismiss();
                    infoDialog = null;
                }
                ExtUtils.sendFileTo(a, file);

            }
        });

        TextView delete = TxtUtils.underlineTextView((TextView) dialog.findViewById(R.id.delete));
        if (onDeleteAction == null) {
            delete.setVisibility(View.GONE);
        }
        delete.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (infoDialog != null) {
                    infoDialog.dismiss();
                    infoDialog = null;
                }

                dialogDelete(a, file, onDeleteAction);

            }
        });

        View openFile = dialog.findViewById(R.id.openFile);
        openFile.setVisibility(ExtUtils.isNotSupportedFile(file) ? View.GONE : View.VISIBLE);
        openFile.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (infoDialog != null) {
                    infoDialog.dismiss();
                    infoDialog = null;
                }
                ExtUtils.showDocument(a, file);

            }
        });

        final ImageView coverImage = (ImageView) dialog.findViewById(R.id.image);
        coverImage.setMaxWidth(Dips.screenWidth() / 3);
        // coverImage.getLayoutParams().width = Dips.screenWidth() / 3;

        IMG.getCoverPage(coverImage, file.getPath(), IMG.getImageSize());

        coverImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showImage(a, file.getPath());
            }
        });

        // IMG.updateImageSizeBig(coverImage);

        final ImageView starIcon = ((ImageView) dialog.findViewById(R.id.starIcon));

        starIcon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DefaultListeners.getOnStarClick(a).onResultRecive(fileMeta, null);

                if (fileMeta.getIsStar() == null || fileMeta.getIsStar() == false) {
                    starIcon.setImageResource(R.drawable.star_2);
                } else {
                    starIcon.setImageResource(R.drawable.star_1);
                }
                TintUtil.setTintImageWithAlpha(starIcon, Color.WHITE);

            }
        });

        if (fileMeta.getIsStar() == null || fileMeta.getIsStar() == false) {
            starIcon.setImageResource(R.drawable.star_2);
        } else {
            starIcon.setImageResource(R.drawable.star_1);
        }
        TintUtil.setTintImageWithAlpha(starIcon, Color.WHITE);
        TintUtil.setBackgroundFillColor(openFile, TintUtil.color);

        // builder.setTitle(R.string.file_info);
        builder.setView(dialog);

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        infoDialog = builder.create();
        infoDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(a);
            }
        });

        infoDialog.show();
    }

    public static void showImage(Activity a, String path) {
        final Dialog builder = new Dialog(a);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
            }
        });

        final ScaledImageView imageView = new ScaledImageView(a);
        imageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                builder.dismiss();

            }
        });
        ImageLoader.getInstance().displayImage(IMG.toUrl(path, -2, Dips.screenWidth()), imageView, IMG.ExportOptions);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (Dips.screenWidth() * 0.9), (int) (Dips.screenHeight() * 0.9));
        builder.addContentView(imageView, params);
        builder.show();
    }

    public static void showImageHttpPath(Context a, String path) {
        final Dialog builder = new Dialog(a);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
            }
        });

        final ScaledImageView imageView = new ScaledImageView(a);
        imageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                builder.dismiss();

            }
        });
        ImageLoader.getInstance().displayImage(path, imageView, IMG.displayCacheMemoryDisc);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (Dips.screenWidth() * 0.9), (int) (Dips.screenHeight() * 0.9));
        builder.addContentView(imageView, params);
        builder.show();
    }

    public static void dialogDelete(final Activity a, final File file, final Runnable onDeleteAction) {
        if (file == null || onDeleteAction == null) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setMessage(a.getString(R.string.delete_book_) + "\n" + file.getName()).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                onDeleteAction.run();
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

}
