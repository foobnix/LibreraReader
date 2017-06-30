package com.foobnix.ui2.adapter;

import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppRecycleAdapter;
import com.foobnix.ui2.fast.FastScroller;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class FileMetaAdapter extends AppRecycleAdapter<FileMeta, RecyclerView.ViewHolder> implements FastScroller.SectionIndexer {

    public static final int DISPLAY_TYPE_FILE = 2;
    public static final int DISPLAY_TYPE_DIRECTORY = 3;
    public static final int DISPALY_TYPE_LAYOUT_STARS = 4;

    public static final int ADAPTER_LIST = 0;
    public static final int ADAPTER_GRID = 1;
    public static final int ADAPTER_COVERS = 3;

    private int adapterType = ADAPTER_LIST;

    public static final int TEMP_VALUE_NONE = 0;
    public static final int TEMP_VALUE_FOLDER_PATH = 1;
    public static final int TEMP_VALUE_STAR_GRID_ITEM = 2;
    public int tempValue = TEMP_VALUE_NONE;

    public class FileMetaViewHolder extends RecyclerView.ViewHolder {
        public TextView title, author, path, ext, size, date, series, idPercentText;
        public ImageView image, star, menu;
        public View progresLayout, parent, remove, layoutBootom, infoLayout, idProgressColor;

        public FileMetaViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title1);
            author = (TextView) view.findViewById(R.id.title2);
            path = (TextView) view.findViewById(R.id.browserPath);
            size = (TextView) view.findViewById(R.id.browserSize);
            ext = (TextView) view.findViewById(R.id.browserExt);
            date = (TextView) view.findViewById(R.id.browseDate);
            series = (TextView) view.findViewById(R.id.series);
            idPercentText = (TextView) view.findViewById(R.id.idPercentText);

            image = (ImageView) view.findViewById(R.id.browserItemIcon);
            star = (ImageView) view.findViewById(R.id.starIcon);
            idProgressColor = view.findViewById(R.id.idProgressColor);
            infoLayout = view.findViewById(R.id.infoLayout);

            progresLayout = view.findViewById(R.id.progresLayout);
            layoutBootom = view.findViewById(R.id.layoutBootom);

            menu = (ImageView) view.findViewById(R.id.itemMenu);
            remove = view.findViewById(R.id.delete);

            parent = view;
        }
    }

    public class DirectoryViewHolder extends RecyclerView.ViewHolder {
        public TextView title, path;
        public ImageView image, starIcon;
        public View parent;

        public DirectoryViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.text1);
            path = (TextView) view.findViewById(R.id.text2);
            image = (ImageView) view.findViewById(R.id.image1);
            starIcon = (ImageView) view.findViewById(R.id.starIcon);
            parent = view;
        }
    }

    public class StarsLayoutViewHolder extends RecyclerView.ViewHolder {
        public RecyclerView recyclerView;
        public TextView clearAllRecent, clearAllStars, starredName, recentName;
        public View panelStars, panelRecent;

        public StarsLayoutViewHolder(View view) {
            super(view);
            recyclerView = (RecyclerView) view.findViewById(R.id.recyclerViewStars);
            clearAllRecent = (TextView) view.findViewById(R.id.clearAllRecent);
            clearAllStars = (TextView) view.findViewById(R.id.clearAllStars);
            starredName = (TextView) view.findViewById(R.id.starredName);
            recentName = (TextView) view.findViewById(R.id.recentName);
            panelStars = view.findViewById(R.id.panelStars);
            panelRecent = view.findViewById(R.id.panelRecent);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (viewType == DISPALY_TYPE_LAYOUT_STARS) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_stars, parent, false);
            return new StarsLayoutViewHolder(itemView);
        }

        if (viewType == DISPLAY_TYPE_DIRECTORY) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browse_dir, parent, false);
            return new DirectoryViewHolder(itemView);
        }

        if (viewType == DISPLAY_TYPE_FILE) {
            if (adapterType == ADAPTER_LIST) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browse_item_list, parent, false);
            } else {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browse_item_grid, parent, false);
            }
            return new FileMetaViewHolder(itemView);
        }
        return null;
    }

    @Override
    public void onViewRecycled(ViewHolder holderAll) {
        super.onViewRecycled(holderAll);
        if (holderAll instanceof FileMetaViewHolder) {
            final FileMetaViewHolder holder = (FileMetaViewHolder) holderAll;
            ImageLoader.getInstance().cancelDisplayTask(holder.image);
            LOG.d("onViewRecycled");
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holderAll, final int position) {
        final FileMeta fileMeta = getItem(position);

        if (holderAll instanceof FileMetaViewHolder) {
            final FileMetaViewHolder holder = (FileMetaViewHolder) holderAll;
            bindFileMetaView(holder, position);

            IMG.getCoverPageWithEffectPos(holder.image, fileMeta.getPath(), IMG.getImageSize(), position, new SimpleImageLoadingListener() {

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    super.onLoadingCancelled(imageUri, view);
                }

                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    super.onLoadingStarted(imageUri, view);
                }

                @Override
                public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
                    if (position <= items.size() - 1) {
                        items.set(position, AppDB.get().getOrCreate(fileMeta.getPath()));
                        bindFileMetaView(holder, position);
                    }
                }

            });
        } else if (holderAll instanceof DirectoryViewHolder) {
            final DirectoryViewHolder holder = (DirectoryViewHolder) holderAll;
            holder.title.setText(fileMeta.getPathTxt());
            holder.path.setText(fileMeta.getPath());

            if (tempValue == TEMP_VALUE_FOLDER_PATH) {
                holder.path.setVisibility(View.VISIBLE);
            } else {
                holder.path.setVisibility(View.GONE);
            }

            TintUtil.setTintImage(holder.image);
            bindItemClickAndLongClickListeners(holder.parent, fileMeta);
            if (!AppState.get().isBorderAndShadow) {
                holder.parent.setBackgroundColor(Color.TRANSPARENT);
            }

            if (AppDB.get().isStarFolder(fileMeta.getPath())) {
                holder.starIcon.setImageResource(R.drawable.star_1);
            } else {
                holder.starIcon.setImageResource(R.drawable.star_2);
            }
            TintUtil.setTintImage(holder.starIcon, TintUtil.color);

            if (onStarClickListener != null) {
                holder.starIcon.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onStarClickListener.onResultRecive(fileMeta, FileMetaAdapter.this);
                    }
                });
            }

        } else if (holderAll instanceof StarsLayoutViewHolder) {
            final StarsLayoutViewHolder holder = (StarsLayoutViewHolder) holderAll;
            FileMetaAdapter adapter = new FileMetaAdapter();
            adapter.setOnItemClickListener(onItemClickListener);
            adapter.setOnItemLongClickListener(onItemLongClickListener);

            adapter.setOnMenuClickListener(onMenuClickListener);
            adapter.setOnStarClickListener(onStarClickListener);

            adapter.setOnAuthorClickListener(onAuthorClickListener);
            adapter.setOnSeriesClickListener(onSeriesClickListener);

            adapter.setAdapterType(FileMetaAdapter.ADAPTER_GRID);
            adapter.tempValue = TEMP_VALUE_STAR_GRID_ITEM;
            holder.recyclerView.setAdapter(adapter);

            adapter.getItemsList().clear();
            List<FileMeta> allStars = AppDB.get().getStarsFiles();
            adapter.getItemsList().addAll(allStars);
            adapter.notifyDataSetChanged();

            TintUtil.setBackgroundFillColor(holder.panelRecent, TintUtil.color);
            TintUtil.setBackgroundFillColor(holder.panelStars, TintUtil.color);

            if (clearAllRecent == null) {
                holder.clearAllRecent.setVisibility(View.GONE);
            } else {
                holder.clearAllRecent.setVisibility(View.VISIBLE);
                TxtUtils.underlineTextView(holder.clearAllRecent).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        clearAllRecent.run();
                    }
                });
            }
            if (clearAllStars == null) {
                holder.clearAllStars.setVisibility(View.GONE);
            } else {
                holder.clearAllStars.setVisibility(View.VISIBLE);
                TxtUtils.underlineTextView(holder.clearAllStars).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        clearAllStars.run();
                    }
                });
            }
            holder.starredName.setText(holder.starredName.getContext().getString(R.string.starred) + " (" + allStars.size() + ")");
            holder.recentName.setText(holder.starredName.getContext().getString(R.string.recent) + " (" + (getItemCount() - 1) + ")");
        }

    }

    private FileMeta bindFileMetaView(final FileMetaViewHolder holder, final int position) {
        if (position >= items.size()) {
            return new FileMeta();
        }
        final FileMeta fileMeta = getItem(position);

        holder.title.setText(fileMeta.getTitle());

        holder.author.setText(fileMeta.getAuthor());

        if (TxtUtils.isEmpty(fileMeta.getAuthor())) {
            holder.title.setSingleLine(false);
            holder.title.setLines(2);
            holder.author.setVisibility(View.GONE);
        } else {
            holder.title.setSingleLine(true);
            holder.title.setLines(1);
            holder.author.setVisibility(View.VISIBLE);
        }

        if (holder.series != null && onSeriesClickListener != null) {
            holder.series.setText(fileMeta.getSequence());
            holder.series.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (onSeriesClickListener != null) {
                        onSeriesClickListener.onResultRecive(fileMeta.getSequence());
                    }
                }
            });
        }
        holder.author.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onAuthorClickListener != null) {
                    onAuthorClickListener.onResultRecive(fileMeta.getAuthor());
                }

            }
        });

        holder.path.setText(fileMeta.getPathTxt());
        holder.ext.setText(fileMeta.getChild() != null ? fileMeta.getChild() : fileMeta.getExt());
        holder.size.setText(fileMeta.getSizeTxt());
        holder.date.setText(fileMeta.getDateTxt());

        if (holder.idProgressColor != null && fileMeta.getIsRecentProgress() != null) {
            if (fileMeta.getIsRecentProgress() > 1) {
                fileMeta.setIsRecentProgress(1f);
            }
            holder.progresLayout.setVisibility(View.VISIBLE);
            holder.idProgressColor.setBackgroundColor(TintUtil.color);
            holder.idProgressColor.getLayoutParams().width = Dips.dpToPx((int) (200 * fileMeta.getIsRecentProgress()));
            holder.idPercentText.setText("" + (int) (100 * fileMeta.getIsRecentProgress()) + "%");
        } else if (holder.progresLayout != null) {
            holder.progresLayout.setVisibility(View.GONE);
        }

        if (fileMeta.getIsStar() == null || fileMeta.getIsStar() == false) {
            holder.star.setImageResource(R.drawable.star_2);
        } else {
            holder.star.setImageResource(R.drawable.star_1);
        }
        TintUtil.setTintImage(holder.star, TintUtil.color);

        if (onStarClickListener != null) {
            holder.star.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    onStarClickListener.onResultRecive(fileMeta, FileMetaAdapter.this);
                }
            });
        } else {
        }

        bindItemClickAndLongClickListeners(holder.parent, fileMeta);

        if (adapterType == ADAPTER_GRID || adapterType == ADAPTER_COVERS) {
            holder.path.setVisibility(View.GONE);
            holder.size.setVisibility(View.GONE);

            int sizeDP = AppState.get().coverBigSize;
            if (tempValue == TEMP_VALUE_STAR_GRID_ITEM) {
                sizeDP = (int) (AppState.get().coverSmallSize * 1.2f);
            }

            IMG.updateImageSizeBig((View) holder.image.getParent().getParent(), sizeDP);

            LayoutParams lp = holder.image.getLayoutParams();
            lp.width = Dips.dpToPx(sizeDP);

            if (AppState.get().isCropBookCovers) {
                lp.height = (int) (lp.width * 1.5);
            } else {
                lp.width = LayoutParams.WRAP_CONTENT;
                lp.height = LayoutParams.WRAP_CONTENT;
            }

        } else {
            holder.path.setVisibility(View.VISIBLE);
            holder.size.setVisibility(View.VISIBLE);

            IMG.updateImageSizeSmall((View) holder.image.getParent().getParent());

            LayoutParams lp = holder.image.getLayoutParams();
            lp.width = Dips.dpToPx(AppState.get().coverSmallSize);
            if (AppState.get().isCropBookCovers) {
                lp.height = (int) (lp.width * 1.5);
            } else {
                lp.height = LayoutParams.WRAP_CONTENT;
            }
        }

        if (AppState.get().isBorderAndShadow) {
            View parent = (View) holder.image.getParent();
            parent.setBackgroundColor(Color.TRANSPARENT);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) parent.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, 0);
        }

        if (AppState.get().isCropBookCovers) {
            holder.image.setScaleType(ScaleType.CENTER_CROP);
        } else {
            holder.image.setScaleType(ScaleType.FIT_CENTER);
        }

        if (holder.layoutBootom != null) {
            if (adapterType == ADAPTER_COVERS || adapterType == ADAPTER_LIST) {
                holder.layoutBootom.setVisibility(View.GONE);
                holder.infoLayout.setVisibility(View.GONE);
            } else if (adapterType == ADAPTER_GRID) {
                holder.layoutBootom.setVisibility(View.VISIBLE);
                holder.infoLayout.setVisibility(View.VISIBLE);
            }
        }

        if (adapterType == ADAPTER_LIST && AppState.get().coverSmallSize >= IMG.TWO_LINE_COVER_SIZE) {
            holder.title.setSingleLine(false);
            holder.title.setLines(2);
            holder.path.setVisibility(View.VISIBLE);
        } else if (adapterType == ADAPTER_LIST) {
            holder.title.setSingleLine(true);
            holder.title.setLines(1);
            holder.path.setVisibility(View.GONE);
            if (AppState.get().coverSmallSize <= IMG.TWO_LINE_COVER_SIZE) {
                holder.infoLayout.setVisibility(View.GONE);
            }
        }

        if (adapterType == ADAPTER_GRID && AppState.get().coverBigSize <= IMG.TWO_LINE_COVER_SIZE * 2) {
            holder.date.setVisibility(View.GONE);
        } else {
            holder.date.setVisibility(View.VISIBLE);
        }

        TintUtil.setTintImage(holder.menu);

        if (holder.remove != null) {
            holder.remove.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    onDeleteClickListener.onResultRecive(fileMeta);
                }
            });

            if (onDeleteClickListener == null) {
                holder.remove.setVisibility(View.GONE);
            }
        }

        holder.menu.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onMenuClickListener != null) {
                    onMenuClickListener.onResultRecive(fileMeta);
                }
            }

        });
        holder.parent.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onItemClickListener.onResultRecive(fileMeta);
            }
        });
        holder.parent.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                onItemLongClickListener.onResultRecive(fileMeta);
                return false;
            }
        });
        if (!AppState.get().isBorderAndShadow) {
            holder.parent.setBackgroundColor(Color.TRANSPARENT);
        }

        return fileMeta;
    }

    @Override
    public String getSectionText(int position) {
        return " " + (position + 1) + " ";
    }


    @Override
    public int getItemViewType(int position) {
        Integer cusType = getItem(position).getCusType();
        if (cusType == null) {
            return DISPLAY_TYPE_FILE;
        }
        return cusType;
    }

    public void setOnMenuClickListener(ResultResponse<FileMeta> onMenuClickListener) {
        this.onMenuClickListener = onMenuClickListener;
    }

    public void setOnDeleteClickListener(ResultResponse<FileMeta> onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public void setOnAuthorClickListener(ResultResponse<String> onAuthorClickListener) {
        this.onAuthorClickListener = onAuthorClickListener;
    }

    public void setOnSeriesClickListener(ResultResponse<String> onSeriesClickListener) {
        this.onSeriesClickListener = onSeriesClickListener;
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }

    public void setOnStarClickListener(ResultResponse2<FileMeta, FileMetaAdapter> onStarClickListener) {
        this.onStarClickListener = onStarClickListener;
    }

    public void setClearAllRecent(Runnable clearAllRecent) {
        this.clearAllRecent = clearAllRecent;
    }

    public void setClearAllStars(Runnable clearAllStars) {
        this.clearAllStars = clearAllStars;
    }

    private ResultResponse<FileMeta> onMenuClickListener;
    private ResultResponse<FileMeta> onDeleteClickListener;
    private ResultResponse<String> onAuthorClickListener;
    private ResultResponse<String> onSeriesClickListener;
    private ResultResponse2<FileMeta, FileMetaAdapter> onStarClickListener;
    private Runnable clearAllRecent;
    private Runnable clearAllStars;

}