package com.foobnix.pdf.info.fragment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.ExtFilter;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.presentation.BrowserAdapter;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.TempHolder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.GridView;

public class BrowseFragmet extends BaseListFragment {
    BrowserAdapter adapter;
    private Map<File, Integer> stack = new HashMap<File, Integer>();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        adapter = new BrowserAdapter(getActivity(), new ExtFilter(ExtUtils.browseExts));
        adapter.setOnMenuPressed(onMenuPressed);
        withBigAds = true;
        AppState.get().isBrowseGrid = false;
    }

    ResultResponse<File> onMenuPressed = new ResultResponse<File>() {

        @Override
        public boolean onResultRecive(final File file) {
            Runnable deleteRunnable = new Runnable() {

                @Override
                public void run() {
                    File parentFile = file.getParentFile();
                    file.delete();
                    setCurrentDir(parentFile);
                }
            };

            if (ExtUtils.isNotSupportedFile(file)) {
                ShareDialog.showArchive(getActivity(), file, deleteRunnable);
            } else {
                ShareDialog.show(getActivity(), file, deleteRunnable, -1, null);
            }
            return false;

        }

    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pathContainer.setVisibility(View.VISIBLE);
        pathView.setEllipsize(TruncateAt.START);
        setCurrentDir(new File(ExportSettingsManager.getInstance(getActivity()).getLastPath()));
        onListGrid.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isBrowseGrid = !AppState.get().isBrowseGrid;
                onGlidList();
            }
        });
        onHome.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setCurrentDir(Environment.getExternalStorageDirectory());
            }
        });
        onGlidList();

    }

    @Override
    public void onConfigurationChanged(final android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onGlidList();
    };

    public void onGlidList() {
        if (!isVisible()) {
            return;
        }
        if (AppState.getInstance().isBrowseGrid) {
            onListGrid.setImageResource(R.drawable.glyphicons_156_show_big_thumbnails);
            listView.setNumColumns(-1);
            listView.setColumnWidth(Dips.dpToPx(AppState.get().coverBigSize));
        } else {
            onListGrid.setImageResource(R.drawable.glyphicons_114_justify);

            int[] col = ADS.getNumberOfColumsAndWidth();
            listView.setNumColumns(col[0]);
            listView.setColumnWidth(col[1]);

        }
        listView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onResume() {
        super.onResume();
        pathContainer.setVisibility(View.VISIBLE);
        setCurrentDir(new File(ExportSettingsManager.getInstance(getActivity()).getLastPath()));
    }

    int listHash = 0;

    @Override
    public void onSelected() {
        super.onSelected();
        LOG.d("SEARCH SELECTED");
        if (listHash != TempHolder.listHash) {
            listHash = TempHolder.listHash;
            onGlidList();
        }
    }

    @Deprecated
    public boolean onBack() {
        File file = new File(ExportSettingsManager.getInstance(getActivity()).getLastPath());
        if (file.getPath() != null) {
            setCurrentDir(file);
            return true;
        }
        return false;
    }

    @Override
    public BaseAdapter getListAdapter() {
        return adapter;
    }

    public boolean isBackProccesed() {
        try {
            // if ("/".equals(adapter.getItem(0).getFile().getParent())) {
            if (true) {
                return false;
            }
        } catch (Exception e) {
            LOG.e(e);
            return false;
        }
        onItemClick(0);
        return true;
    }

    @Override
    public boolean onLongClickItem(int pos) {
        if (listView == null) {
            return false;
        }
        final File file = null;// adapter.getItem(pos).getFile();
        if (!file.isDirectory()) {
            FileInformationDialog.showFileInfoDialog(getActivity(), file, null);
            return true;
        }
        return false;

    }

    @Override
    public void onItemClick(int pos) {
        final File file = null;// adapter.getItem(pos).getFile();
        if (file.isDirectory()) {
            File parentFile = file.getParentFile();
            // if (!stack.containsKey(file)) {
            if (listView.getFirstVisiblePosition() > 0) {
                stack.put(parentFile, listView.getFirstVisiblePosition());
                LOG.d("TEST", "put " + listView.getFirstVisiblePosition() + parentFile);
            }
            // }

            setCurrentDir(file);
            ExportSettingsManager.getInstance(getActivity()).saveLastPath(file);

            if (stack.containsKey(file)) {
                listView.setSelection(stack.get(file));
                // listView.scrollTo(0, stack.get(file));
                LOG.d("TEST", "setSelection " + stack.get(file) + file);
            } else {
                listView.setSelection(0);
                LOG.d("TEST", "setSelection " + 0 + file);
            }

        } else {
            ExtUtils.openFile(getActivity(), file);
        }

    }

    private void setCurrentDir(File newDir) {
        adapter.setCurrentDirectory(newDir);
        String[] split = newDir.getPath().split("/");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i == split.length - 1) {
                res.append("/<b>" + split[i] + "</b>");
            } else {
                String string = split[i];
                if (string.length() != 0) {
                    res.append("/" + string);
                }
            }
        }

        pathView.setText(Html.fromHtml(res.toString()));
    }

}
