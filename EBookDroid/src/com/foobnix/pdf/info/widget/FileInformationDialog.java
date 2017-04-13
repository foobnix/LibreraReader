package com.foobnix.pdf.info.widget;

import java.io.File;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.bitmaps.RawBitmap;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
		if (TxtUtils.isNotEmpty(fileMeta.getAuthor())) {
			title.setText(fileMeta.getAuthor() + " - " + fileMeta.getTitle());
		} else {
			title.setText(fileMeta.getTitle());
		}
		if (!ExtUtils.isTextFomat(file.getPath())) {
			title.setText(ExtUtils.getFileNameWithoutExt(file.getName()));
		}

		((TextView) dialog.findViewById(R.id.path)).setText(file.getPath());
		((TextView) dialog.findViewById(R.id.date)).setText(fileMeta.getDateTxt());
		((TextView) dialog.findViewById(R.id.mimeExt)).setText(fileMeta.getExt());
		((TextView) dialog.findViewById(R.id.size)).setText(fileMeta.getSizeTxt());
		((TextView) dialog.findViewById(R.id.mimeType)).setText("" + ExtUtils.getMimeType(file));

		// ((TextView)
		// dialog.findViewById(R.id.metaTitle)).setText(fileMeta.getTitle());
		// ((TextView)
		// dialog.findViewById(R.id.metaAuthor)).setText(fileMeta.getAuthor());

		final TextView infoView = (TextView) dialog.findViewById(R.id.metaInfo);
		String bookOverview = FileMetaCore.getBookOverview(file.getPath());
		if (TxtUtils.isEmpty(bookOverview)) {
			((View) infoView.getParent()).setVisibility(View.GONE);
		} else {
			infoView.setText(bookOverview);
		}

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

		TextView convertFile = (TextView) dialog.findViewById(R.id.convertFile);
		convertFile.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ShareDialog.showsItemsDialog(a, file.getPath(), AppState.CONVERTERS.get("EPUB"));
			}
		});
		convertFile.setText(TxtUtils.underline(a.getString(R.string.convert_to) + " EPUB"));
		convertFile.setVisibility(ExtUtils.isImageOrEpub(file) ? View.GONE : View.VISIBLE);

		TxtUtils.underlineTextView((TextView) dialog.findViewById(R.id.openWith))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (infoDialog != null) {
							infoDialog.dismiss();
							infoDialog = null;
						}
						ExtUtils.openWith(a, file);

					}
				});

		TxtUtils.underlineTextView((TextView) dialog.findViewById(R.id.sendFile))
				.setOnClickListener(new OnClickListener() {

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

		TextView openFile = TxtUtils.underlineTextView((TextView) dialog.findViewById(R.id.openFile));
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

		ImageView coverImage = (ImageView) dialog.findViewById(R.id.image);

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
				TintUtil.setTintImage(starIcon, TintUtil.color);

			}
		});

		if (fileMeta.getIsStar() == null || fileMeta.getIsStar() == false) {
			starIcon.setImageResource(R.drawable.star_2);
		} else {
			starIcon.setImageResource(R.drawable.star_1);
		}
		TintUtil.setTintImage(starIcon, TintUtil.color);
		TintUtil.setBackgroundFillColor(openFile, TintUtil.color);

		// builder.setTitle(R.string.file_info);
		builder.setView(dialog);

		builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

			}
		});

		infoDialog = builder.create();
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

		final ImageView imageView = new ImageView(a);
		imageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				builder.dismiss();

			}
		});
		ImageLoader.getInstance().loadImage(IMG.toUrl(path, -2, Dips.screenWidth()), new ImageLoadingListener() {

			@Override
			public void onLoadingStarted(String arg0, View arg1) {
			}

			@Override
			public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {

			}

			@Override
			public void onLoadingComplete(String arg0, View arg1, Bitmap bmp) {

				try {
					int r = Math.min(4, Dips.screenWidth() / bmp.getWidth());
					if (r <= 1) {
						imageView.setImageBitmap(bmp);
						return;
					}

					Rect srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
					BitmapRef scale = null;
					if (r == 2) {
						scale = RawBitmap.scaleHq2x(new RawBitmap(bmp, srcRect));
					}
					if (r == 3) {
						scale = RawBitmap.scaleHq3x(new RawBitmap(bmp, srcRect));
					}
					if (r == 4) {
						scale = RawBitmap.scaleHq4x(new RawBitmap(bmp, srcRect));
					}

					imageView.setImageBitmap(scale.getBitmap());
				} catch (Exception e) {
					LOG.e(e);
					imageView.setImageBitmap(bmp);
				}

			}

			@Override
			public void onLoadingCancelled(String arg0, View arg1) {
				// TODO Auto-generated method stub

			}
		});

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		builder.addContentView(imageView, params);
		builder.show();
	}

	public static void dialogDelete(final Activity a, final File file, final Runnable onDeleteAction) {
		if (file == null || onDeleteAction == null) {
			return;
		}
		final AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setMessage(a.getString(R.string.delete_book_) + " " + file.getName())
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
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
