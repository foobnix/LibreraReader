package com.foobnix.pdf.info.presentation;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class BrowserAdapter extends BaseAdapter {
    private final Context c;
    private File currentDirectory;
    private List<FileMeta> files = Collections.emptyList();
    private final FileFilter filter;
    private ResultResponse<File> onMenuPressed;

    public BrowserAdapter(final Context c, FileFilter filter) {
        this.c = c;
        this.filter = filter;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public FileMeta getItem(int i) {
        return files.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {

        View browserItem = convertView = null;

        if (convertView != null && ((Boolean) convertView.getTag()) != AppState.get().isBrowseGrid) {
            convertView = null;
        }

        if (convertView == null) {
            if (AppState.get().isBrowseGrid) {
                browserItem = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.browse_item_grid, viewGroup, false);
            } else {
                browserItem = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.browse_item_list, viewGroup, false);
            }
            browserItem.setTag(AppState.get().isBrowseGrid);
        }

        final ImageView imageView = (ImageView) browserItem.findViewById(R.id.browserItemIcon);
        final ImageView starIcon = (ImageView) browserItem.findViewById(R.id.starIcon);

        final FileMeta file = files.get(i);
        final TextView title1 = (TextView) browserItem.findViewById(R.id.title1);
        final TextView title2 = (TextView) browserItem.findViewById(R.id.title2);
        title1.setText(file.getPathTxt());

        View progresLayout = browserItem.findViewById(R.id.progresLayout);
        if (progresLayout != null) {
            progresLayout.setVisibility(View.GONE);
        }

        View delete = browserItem.findViewById(R.id.delete);
        if (delete != null) {
            delete.setVisibility(View.GONE);
        }

        final TextView extFile = (TextView) browserItem.findViewById(R.id.browserExt);

        final ImageView itemMenu = (ImageView) browserItem.findViewById(R.id.itemMenu);
        TintUtil.setTintImageWithAlpha(itemMenu);

        final View infoLayout = browserItem.findViewById(R.id.infoLayout);
        final TextView textPath = (TextView) browserItem.findViewById(R.id.browserPath);

        imageView.setColorFilter(null);

        if (file.getPath().equals(currentDirectory.getParent())) {
            imageView.setImageResource(R.drawable.glyphicons_217_circle_arrow_left);
            TintUtil.setTintImageWithAlpha(imageView);
            imageView.setBackgroundColor(Color.TRANSPARENT);

            title1.setText(file.getPath());
            title1.setSingleLine();
            infoLayout.setVisibility(View.GONE);
            itemMenu.setVisibility(View.GONE);
            extFile.setVisibility(View.GONE);
            title2.setVisibility(View.GONE);
            textPath.setVisibility(View.GONE);
            imageView.setScaleType(ScaleType.CENTER_INSIDE);

            starIcon.setVisibility(View.GONE);

            imageView.getLayoutParams().width = AppState.get().isBrowseGrid ? Dips.dpToPx(AppState.get().coverBigSize) : Dips.dpToPx(35);
            imageView.getLayoutParams().height = AppState.get().isBrowseGrid ? Dips.dpToPx((int) (AppState.get().coverBigSize * 1.5)) : Dips.dpToPx(35);
            imageView.setLayoutParams(imageView.getLayoutParams());
        } else if (new File(file.getPath()).isDirectory()) {
            imageView.getLayoutParams().width = AppState.get().isBrowseGrid ? Dips.dpToPx(AppState.get().coverBigSize) : Dips.dpToPx(35);
            imageView.getLayoutParams().height = AppState.get().isBrowseGrid ? Dips.dpToPx((int) (AppState.get().coverBigSize * 1.5)) : Dips.dpToPx(35);
            imageView.setLayoutParams(imageView.getLayoutParams());

            title1.setSingleLine();
            imageView.setImageResource(R.drawable.glyphicons_441_folder_closed);
            TintUtil.setTintImageWithAlpha(imageView);
            imageView.setBackgroundColor(Color.TRANSPARENT);

            infoLayout.setVisibility(View.GONE);
            itemMenu.setVisibility(View.GONE);
            extFile.setVisibility(View.GONE);
            title2.setVisibility(View.GONE);
            textPath.setVisibility(View.GONE);
            imageView.setScaleType(ScaleType.CENTER_INSIDE);

            starIcon.setVisibility(View.GONE);

        } else {
            imageView.setBackgroundColor(Color.TRANSPARENT);
            if (AppState.get().isBrowseGrid) {
                IMG.updateImageSizeBig(imageView);
                IMG.updateImageSizeBig((View) imageView.getParent());
            } else {
                IMG.updateImageSizeSmall(imageView);
                IMG.updateImageSizeSmall((View) imageView.getParent());
            }

            if (AppState.get().isCropBookCovers) {
                imageView.setScaleType(ScaleType.CENTER_CROP);
            } else {
                imageView.setScaleType(ScaleType.FIT_CENTER);
            }

            starIcon.setVisibility(View.VISIBLE);

            if (AppState.get().isBrowseGrid) {
                title2.setVisibility(View.GONE);
                textPath.setVisibility(View.GONE);
            } else {
                title2.setVisibility(View.VISIBLE);
                textPath.setVisibility(View.VISIBLE);
            }

            FileMeta info = null;
            if (info != null) {
                title1.setText("" + info.getTitle());
                title2.setText("" + info.getAuthor());
            }

            if (!AppState.get().isBrowseGrid && AppState.get().coverSmallSize >= IMG.TWO_LINE_COVER_SIZE) {
                title1.setSingleLine(false);
                title1.setLines(2);
            } else {
                title1.setSingleLine(true);
                title1.setLines(1);
            }


            IMG.getCoverPageWithEffect(imageView, file.getPath(), IMG.getImageSize(), new ImageLoadingListener() {

                @Override
                public void onLoadingStarted(String arg0, View arg1) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
                    FileMeta info = null;// MetaCache.get().getByPath(file.getPath());
                    if (info != null) {
                        title1.setText("" + info.getTitle());
                        title2.setText("" + info.getAuthor());
                        textPath.setText(info.getPathTxt());
                        extFile.setText(info.getExt());
                        StarsWrapper.addStars(starIcon, info);
                    }
                }

                @Override
                public void onLoadingCancelled(String arg0, View arg1) {
                    // TODO Auto-generated method stub

                }
            });
            textPath.setText(file.getPathTxt());
            infoLayout.setVisibility(View.VISIBLE);
            itemMenu.setVisibility(View.VISIBLE);
            extFile.setVisibility(View.VISIBLE);

            final TextView textSize = (TextView) browserItem.findViewById(R.id.browserSize);
            final TextView textDate = (TextView) browserItem.findViewById(R.id.browseDate);

            View menuIcon = browserItem.findViewById(R.id.itemMenu);
            menuIcon.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (onMenuPressed != null) {
                        // onMenuPressed.onResult(file.getFile());
                    }
                }
            });
            extFile.setText(file.getExt());
            textSize.setText(file.getSizeTxt());
            textDate.setText(file.getDateTxt());

        }

        return browserItem;
    }

    @Override
    public void notifyDataSetChanged() {
        // MetaCache.get().updateStartsCache();
        super.notifyDataSetChanged();
    }

    public void setOnMenuPressed(ResultResponse<File> onMenuPressed) {
        this.onMenuPressed = onMenuPressed;
    }

    public void setCurrentDirectory(File currentDirectory) {
        final File[] fileArray = currentDirectory.listFiles(filter);
        ArrayList<File> files = new ArrayList<File>(fileArray != null ? Arrays.asList(fileArray) : Collections.<File> emptyList());
        this.currentDirectory = currentDirectory;
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
        if (currentDirectory.getParentFile() != null) {
            files.add(0, currentDirectory.getParentFile());
        }
        setFiles(files);
    }

    public void setFiles(List<File> files) {
        // this.files = MetaCache.get().createGetSimpleMetafromFiles(files);
        notifyDataSetInvalidated();
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }
}
