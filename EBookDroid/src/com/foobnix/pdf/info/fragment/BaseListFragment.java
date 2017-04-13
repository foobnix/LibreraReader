package com.foobnix.pdf.info.fragment;

import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.search.view.BookshelfView;
import com.google.android.gms.ads.NativeExpressAdView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public abstract class BaseListFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener {

    protected TextView pathView;
    protected BookshelfView listView;
    protected View filterLayout;
    protected View pathContainer;

    protected View onHome;
    protected ImageView onListGrid;

    protected ImageView sortOrderImage;
    protected TextView sortBy;
    protected AutoCompleteTextView filterLine;
    protected ImageView cleanFilter;
    protected View onRefresh;
    protected ImageView gridList;

    protected LinearLayout recentContainer, exportContainer;
    protected RelativeLayout bookmarksSearchContainer;
    NativeExpressAdView nativeBig;
    protected LinearLayout starList;
    protected TextView countBooks;

    View progressBar, secondTopPanel;
    public boolean withBigAds = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.title_list, container, false);

        nativeBig = (NativeExpressAdView) inflate.findViewById(R.id.nativeBig);
        if (withBigAds) {
            nativeBig.loadAd(ADS.adRequest);
        }

        starList = (LinearLayout) inflate.findViewById(R.id.starList);
        secondTopPanel = inflate.findViewById(R.id.secondTopPanel);

        listView = (BookshelfView) inflate.findViewById(R.id.list);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), false, true));

        countBooks = (TextView) inflate.findViewById(R.id.countBooks);
        pathView = (TextView) inflate.findViewById(R.id.path);
        onListGrid = (ImageView) inflate.findViewById(R.id.onListGrid);
        // onListGrid.setVisibility(View.GONE);

        onHome = inflate.findViewById(R.id.onHome);

        progressBar = inflate.findViewById(R.id.progressBar);

        pathContainer = inflate.findViewById(R.id.pathContainer);
        pathContainer.setVisibility(View.GONE);

        filterLayout = inflate.findViewById(R.id.filterLayout);
        filterLayout.setVisibility(View.GONE);

        exportContainer = (LinearLayout) inflate.findViewById(R.id.exportContainer);
        exportContainer.setVisibility(View.GONE);

        recentContainer = (LinearLayout) inflate.findViewById(R.id.recentContainer);
        recentContainer.setVisibility(View.GONE);

        bookmarksSearchContainer = (RelativeLayout) inflate.findViewById(R.id.bookmarksSearchContainer);
        bookmarksSearchContainer.setVisibility(View.GONE);

        listView.setAdapter(getListAdapter());

        sortBy = (TextView) inflate.findViewById(R.id.sortBy);
        sortOrderImage = (ImageView) inflate.findViewById(R.id.sortOrder);
        filterLine = (AutoCompleteTextView) inflate.findViewById(R.id.filterLine);
        cleanFilter = (ImageView) inflate.findViewById(R.id.cleanFilter);
        cleanFilter.setVisibility(View.GONE);

        onRefresh = inflate.findViewById(R.id.onRefresh);
        gridList = (ImageView) inflate.findViewById(R.id.onGridList);

        // TintUtil.addTingBg(sortBy);
        // TintUtil.addTingBg(sortOrderImage);

        TintUtil.addDrawable(filterLine.getBackground());
        TintUtil.addGradiendDrawableFill((GradientDrawable) secondTopPanel.getBackground());

        // TintUtil.addDrawable();

        // TintUtil.setTintImage(sortOrderImage, Color.WHITE);

        // TintUtil.addTingBg(onRefresh);
        // TintUtil.addTingBg(gridList);
        // TintUtil.addDrawable(cleanFilter.getDrawable());

        return inflate;
    }

    public abstract BaseAdapter getListAdapter();

    public abstract void onItemClick(int pos);

    public boolean onLongClickItem(int pos) {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onItemClick(position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return onLongClickItem(position);
    }

    @Override
    public boolean hideBottomBanner() {
        if (withBigAds) {
            if (getListAdapter().getCount() == 0 && AppSharedPreferences.get().getStars().isEmpty()) {
                listView.setVisibility(View.GONE);
                nativeBig.setVisibility(View.VISIBLE);
            } else {
                listView.setVisibility(View.VISIBLE);
                nativeBig.setVisibility(View.GONE);
            }
        }
        return withBigAds && (getListAdapter().getCount() == 0 && AppSharedPreferences.get().getStars().isEmpty());
    }

    public void adsPause() {
        if (nativeBig != null) {
            nativeBig.pause();
        }
    }

    public void adsResume() {
        if (nativeBig != null) {
            nativeBig.resume();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adsResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        adsPause();
    }

    @Override
    public void onSelected() {

    }

}
