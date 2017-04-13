package com.foobnix.pdf.info.presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.AppDB;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class RecentAdapter extends BaseAdapter {
    private AppSharedPreferences viewerPreferences;
    private List<Uri> uris = new ArrayList<Uri>();
    private boolean submenu = false;
    private Context c;
    private ResultResponse<File> onMenuPressed;
    private Runnable onStarPressed;

    public RecentAdapter(Context c) {
        this.c = c;
        realoadAds();
    }

    private View adView;
    int adsPosition1 = 0;

    public void realoadAds() {
        if (!ADS.isNativeEnable) {
            return;
        }

        if (!AppsConfig.checkIsProInstalled(c)) {
            adsPosition1 = new Random().nextInt(ADS.ADS_MAX_POS);

            NativeExpressAdView adView1 = new NativeExpressAdView(c);
            adView1.setAdUnitId(AppsConfig.ADMOB_NATIVE_SMALL);
            int[] col = ADS.getNumberOfColumsAndWidth();
            adView1.setAdSize(new AdSize(Dips.pxToDp(col[1]) - 10, 100));

            adView1.loadAd(ADS.adRequest);
            adView1.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int errorCode) {
                    adView = LayoutInflater.from(c).inflate(R.layout.ads, null, false);
                    adView.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Urls.openPdfPro(c);
                        }
                    });
                    notifyDataSetChanged();
                }
            });
            adView = adView1;
        }
    }

    @Override
    public int getCount() {
        return uris.size();
    }

    @Override
    public Uri getItem(int i) {
        return uris.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    private ResultResponse<Uri> onDeleClick;

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View browserItem = convertView;

        final Uri uri = uris.get(i);

        if (ADS.isNativeEnable && uri.equals(Uri.EMPTY) && !submenu) {
            if (browserItem != null && ((Boolean) browserItem.getTag()) == false) {
                return browserItem;
            }
            browserItem = adView;
            browserItem.setTag(false);
            return browserItem;
        }

        if (browserItem != null && ((Boolean) browserItem.getTag()) == false) {
            browserItem = null;
        }

        if (browserItem == null) {
            browserItem = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.browse_item_list, viewGroup, false);
            browserItem.setTag(true);
        }

        final ImageView imageView = (ImageView) browserItem.findViewById(R.id.browserItemIcon);
        IMG.updateImageSizeSmall(imageView);
        IMG.updateImageSizeSmall((View) imageView.getParent());

        File uriFile = new File(uri.getPath());

        final TextView title1 = (TextView) browserItem.findViewById(R.id.title1);
        final TextView title2 = (TextView) browserItem.findViewById(R.id.title2);

        final TextView extFile = (TextView) browserItem.findViewById(R.id.browserExt);
        final TextView textSize = (TextView) browserItem.findViewById(R.id.browserSize);
        final TextView textDate = (TextView) browserItem.findViewById(R.id.browseDate);
        final TextView textPath = (TextView) browserItem.findViewById(R.id.browserPath);
        TextView idPercentText = (TextView) browserItem.findViewById(R.id.idPercentText);
        ImageView itemMenu = (ImageView) browserItem.findViewById(R.id.itemMenu);
        View idProgressColor = browserItem.findViewById(R.id.idProgressColor);
        View idProgressBg = browserItem.findViewById(R.id.idProgressBg);

        idProgressColor.setBackgroundColor(TintUtil.color);

        final ImageView starIcon = (ImageView) browserItem.findViewById(R.id.starIcon);
        if (submenu) {
            starIcon.setVisibility(View.GONE);
        }
        TintUtil.setTintImage(itemMenu);

        if (uriFile.isFile()) {
            title1.setText(uri.getLastPathSegment());
        } else {
            title1.setText(uri.getPath());
        }

        title1.setVisibility(AppState.getInstance().isRecentGrid ? View.GONE : View.VISIBLE);
        title2.setVisibility(AppState.getInstance().isRecentGrid ? View.GONE : View.VISIBLE);
        if (submenu) {
            title1.setVisibility(View.VISIBLE);
        }

        if (AppState.get().coverSmallSize >= IMG.TWO_LINE_COVER_SIZE + 40) {
            title1.setLines(2);
            title1.setSingleLine(false);
        } else {
            title1.setLines(1);
            title1.setSingleLine(true);
        }

        final ImageView deleteView = (ImageView) browserItem.findViewById(R.id.delete);
        if (deleteView != null) {
            deleteView.setVisibility(View.VISIBLE);
            deleteView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (onDeleClick != null) {
                        onDeleClick.onResultRecive(uri);
                    }
                }
            });
        }

        if (submenu) {
            imageView.setVisibility(View.GONE);
            deleteView.setVisibility(View.GONE);
            // title1.setTextColor(Color.BLACK);
            itemMenu.setVisibility(View.GONE);
            // textView.setSingleLine(true);
        }

        final FileMeta info = AppDB.get().getOrCreate(uri.getPath());
        if (info != null) {
            title1.setText("" + info.getTitle());
            title2.setText("" + info.getAuthor());
        }

        BookSettings bookSettings = SettingsManager.getTempBookSettings(uri.getPath());

        int currentPage = 1;
        int pages = 1;

        if (bookSettings != null) {
            currentPage = bookSettings.getCurrentPage().docIndex + 1;
            pages = bookSettings.getPages();
        }
        if (pages == 0) {
            pages = currentPage;
        }
        float percent = (float) currentPage / pages;

        idPercentText.setText((int) (percent * 100) + "%");

        idProgressColor.getLayoutParams().width = Dips.dpToPx((int) (200 * percent));
        idProgressBg.getLayoutParams().width = Dips.dpToPx(200);

        if (info != null) {
            textSize.setText(info.getSizeTxt());
            textDate.setText(info.getDateTxt());
            extFile.setText(info.getExt());
            textPath.setText(info.getPathTxt());

            if (!submenu) {
                StarsWrapper.addStars(starIcon, info, onStarPressed);
            }
        }

        if (AppState.get().isCropBookCovers) {
            imageView.setScaleType(ScaleType.CENTER_CROP);
        } else {
            imageView.setScaleType(ScaleType.FIT_CENTER);
        }

        IMG.getCoverPageWithEffect(imageView, uri.getPath(), IMG.getImageSize(), new ImageLoadingListener() {

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
                FileMeta info = null;//  FileMetaDB.get().getOrCreate(uri.getPath());
                if (info != null) {
                    title1.setText("" + info.getTitle());
                    title2.setText("" + info.getAuthor());

                    extFile.setText(info.getExt());
                    textPath.setText(info.getPathTxt());

                    if (!submenu) {
                        StarsWrapper.addStars(starIcon, info, onStarPressed);
                    }
                }
            }

            @Override
            public void onLoadingCancelled(String arg0, View arg1) {
                // TODO Auto-generated method stub

            }
        });

        itemMenu.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onMenuPressed != null) {
                    if (ExtUtils.doifFileExists(c, uri.getPath())) {
                        File file = new File(uri.getPath());
                        onMenuPressed.onResultRecive(file);
                    }
                }
            }
        });

        if (uriFile.isFile() || !uriFile.exists()) {
            imageView.setVisibility(View.VISIBLE);
        } else {
            if (getCount() == 1) {
                deleteView.setVisibility(View.GONE);
            }
            imageView.setVisibility(View.GONE);
        }

        return browserItem;
    }

    public void setUris(List<Uri> turi) {
        LOG.d(RecentAdapter.class.getName(), "setUris");
        uris.clear();
        uris.addAll(turi);
        if (ADS.isNativeEnable) {
            if (!AppsConfig.checkIsProInstalled(c) && uris.size() > 3) {
                if (adsPosition1 < uris.size()) {
                    uris.add(adsPosition1, Uri.EMPTY);
                }
            }
        }

        // notifyDataSetInvalidated();
        notifyDataSetChanged();

    }

    private final static Comparator<Uri> comparator = new Comparator<Uri>() {

        @Override
        public int compare(Uri lhs, Uri rhs) {
            return lhs.getPath().compareTo(rhs.getPath());
        }
    };

    public AppSharedPreferences getViewerPreferences() {
        return viewerPreferences;
    }

    public void setViewerPreferences(AppSharedPreferences viewerPreferences) {
        this.viewerPreferences = viewerPreferences;
    }

    public boolean isSubmenu() {
        return submenu;
    }

    public void setSubmenu(boolean submenu) {
        this.submenu = submenu;
    }

    public void setOnDeleClick(ResultResponse<Uri> onDeleClick) {
        this.onDeleClick = onDeleClick;
    }

    public ResultResponse<File> getOnMenuPressed() {
        return onMenuPressed;
    }

    public void setOnMenuPressed(ResultResponse<File> onMenuPressed) {
        this.onMenuPressed = onMenuPressed;
    }

    public Runnable getOnStarPressed() {
        return onStarPressed;
    }

    public void setOnStarPressed(Runnable onStarPressed) {
        this.onStarPressed = onStarPressed;
    }
}
