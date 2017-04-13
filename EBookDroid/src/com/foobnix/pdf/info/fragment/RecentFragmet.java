package com.foobnix.pdf.info.fragment;

import java.io.File;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.presentation.RecentAdapter;
import com.foobnix.pdf.info.presentation.RecentBooksView;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.TempHolder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class RecentFragmet extends BaseListFragment {
    private RecentAdapter recentAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (recentAdapter == null) {
            recentAdapter = new RecentAdapter(getActivity());
        }
        recentAdapter.setOnMenuPressed(onMenuPressed);
        recentAdapter.setOnStarPressed(onStarPressed);
        withBigAds = true;

    }

    ResultResponse<File> onMenuPressed = new ResultResponse<File>() {

        @Override
        public boolean onResultRecive(final File file) {

            ShareDialog.show(getActivity(), file, new Runnable() {

                @Override
                public void run() {
                    onDeleteClieck.onResultRecive(Uri.fromFile(file));
                    file.delete();
                }
            }, -1, null);
            return false;

        }

    };

    @Override
    public void onConfigurationChanged(final android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onGridList();
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pathContainer.setVisibility(View.GONE);
        pathView.setVisibility(View.GONE);
        onHome.setVisibility(View.GONE);
        onListGrid.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isRecentGrid = !AppState.get().isRecentGrid;
                onGridList();
            }
        });

        recentAdapter.setOnDeleClick(onDeleteClieck);

        recentContainer.setVisibility(View.VISIBLE);
        TextView recentClear = (TextView) recentContainer.findViewById(R.id.clearAllRecent);
        TxtUtils.underlineTextView(recentClear);
        recentClear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                builder.setMessage(getString(R.string.clear_all_recent) + "?");
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppSharedPreferences.get().cleanRecent();
                        updateInformation();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();

            }
        });
        ((ViewGroup) starList.getParent()).setVisibility(View.VISIBLE);

        TextView starsClear = (TextView) recentContainer.findViewById(R.id.clearAllStarred);
        TxtUtils.underlineTextView(starsClear);
        starsClear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                builder.setMessage(getString(R.string.clear_all_marked) + "?");
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppSharedPreferences.get().cleanStars();
                        updateStars();

                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();

            }
        });

        onGridList();
    }

    ResultResponse<Uri> onDeleteClieck = new ResultResponse<Uri>() {

        @Override
        public boolean onResultRecive(Uri uri) {
            AppSharedPreferences.get().removeRecent(uri);
            updateInformation();
            RecentUpates.updateAll(getActivity());
            return false;
        }
    };

    public void onGridList() {
        LOG.d("Recent Adapter", "onGlidList()");

        if (!isVisible()) {
            return;
        }
        if (AppState.getInstance().isRecentGrid) {
            onListGrid.setImageResource(R.drawable.glyphicons_156_show_big_thumbnails);
            listView.setNumColumns(-1);
            listView.setColumnWidth(-1);
        } else {
            onListGrid.setImageResource(R.drawable.glyphicons_114_justify);

            final int width = Dips.screenWidth();
            final int height = Dips.screenHeight();
            if (width > height) {
                listView.setNumColumns(2);
                listView.setColumnWidth(width / 2);
            } else {
                listView.setNumColumns(1);
                listView.setColumnWidth(width);
            }

        }
        listView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

        recentAdapter.notifyDataSetChanged();

    }

    public void updateStars() {
        if (starList == null) {
            return;
        }
        IMG.updateLayoutHeightSizeSmall((ViewGroup) starList.getParent());
        IMG.updateLayoutHeightSizeSmall(starList);
        RecentBooksView.init(getActivity(), starList, false);
    }

    Runnable onStarPressed = new Runnable() {

        @Override
        public void run() {
            updateStars();
        }
    };

    int listHash = 0;

    @Override
    public void onSelected() {
        super.onSelected();
        LOG.d("SEARCH SELECTED");
        if (listHash != TempHolder.listHash) {
            listHash = TempHolder.listHash;
            updateInformation();
        }
    }

    public void updateInformation() {
        LOG.d("Recent update information");
        if (recentAdapter != null) {
            recentAdapter.setUris(AppSharedPreferences.get().getRecent());
        }
        updateStars();
    }

    @Override
    public void onPause() {
        super.onPause();
        RecentUpates.updateAll(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        LOG.d("Recent onResume");
        updateInformation();
    }

    @Override
    public BaseAdapter getListAdapter() {
        return recentAdapter;
    }

    @Override
    public void onItemClick(int pos) {
        final Uri file = recentAdapter.getItem(pos);
        ExtUtils.openFile(getActivity(), new File(file.getPath()));
    }

    @Override
    public boolean onLongClickItem(int pos) {
        if (listView == null) {
            return false;
        }
        final Uri file = recentAdapter.getItem(pos);
        FileInformationDialog.showFileInfoDialog(getActivity(), new File(file.getPath()), null);
        return true;

    }

}
