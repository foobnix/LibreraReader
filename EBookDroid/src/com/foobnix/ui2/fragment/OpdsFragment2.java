package com.foobnix.ui2.fragment;

import java.util.List;
import java.util.Stack;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.opds.Entry;
import com.foobnix.opds.Feed;
import com.foobnix.opds.Link;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.ui2.adapter.EntryAdapter;
import com.foobnix.ui2.fast.FastScrollRecyclerView;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class OpdsFragment2 extends UIFragment<Entry> {

    private static final String FLIBUSTA = "http://flibusta.is";
    EntryAdapter searchAdapter;
    private FastScrollRecyclerView recyclerView;
    TextView titleView;

    String url = FLIBUSTA + "/opds";
    String title;
    Stack<String> stack = new Stack<String>();

    public OpdsFragment2() {
        super();
    }

    public static OpdsFragment2 newInstance(Bundle bundle) {
        OpdsFragment2 br = new OpdsFragment2();
        br.setArguments(bundle);
        return br;
    }

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return new Pair<Integer, Integer>(R.string.catalogs, R.drawable.glyphicons_145_folder_open);
    }

    @Override
    public void onTintChanged() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_opds2, container, false);

        recyclerView = (FastScrollRecyclerView) view.findViewById(R.id.recyclerView);

        titleView = (TextView) view.findViewById(R.id.titleView);

        searchAdapter = new EntryAdapter();

        onGridList();

        searchAdapter.setOnItemClickListener(new ResultResponse<Entry>() {

            @Override
            public boolean onResultRecive(Entry result) {
                onClickLink(result.links.get(0));
                return false;
            }
        });
        searchAdapter.setOnLinkClickListener(new ResultResponse<Link>() {

            @Override
            public boolean onResultRecive(Link link) {
                onClickLink(link);
                return false;
            }
        });

        searchAdapter.setOnItemLongClickListener(new ResultResponse<Entry>() {
            @Override
            public boolean onResultRecive(Entry result) {
                return false;
            }
        });

        view.findViewById(R.id.onBack).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackAction();
            }
        });

        view.findViewById(R.id.onHome).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                stack.clear();
                url = getHome();
                populate();
            }
        });

        populate();
        onTintChanged();

        return view;
    }

    public boolean onBackAction() {
        String last = popStack();

        if (last.equals(url)) {
            last = popStack();// two times
        }
        url = last;

        populate();
        return !getHome().equals(url);
    }

    public String popStack() {
        if (stack.isEmpty()) {
            return getHome();
        }
        return stack.pop();
    }

    public String getHome() {
        return FLIBUSTA + "/opds";
    }

    public void onClickLink(Link link) {
        if (link.isDisabled()) {
            Toast.makeText(getActivity(), R.string.can_t_download, Toast.LENGTH_SHORT).show();
        } else if (link.isWebLink()) {
            Urls.open(getActivity(), link.href);
        } else if (link.isOpdsLink()) {
            url = link.href;
            stack.push(url);
            populate();
        } else if (link.isImageLink()) {
        } else {
            final DownloadManager dm = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
            Request request = new Request(Uri.parse(link.href));
            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            String subPath = ExtUtils.LIRBI + "/" + link.getDownloadName();
            LOG.d("Download to:", subPath);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, subPath);
            dm.enqueue(request);
        }
    }

    @Override
    public List<Entry> prepareDataInBackground() {
        LOG.d("Load: >>>", url);
        Feed feed = OPDS.getFeed(url);
        updateLinks(feed.title, feed.links);
        for (Entry e : feed.entries) {
            updateLinks(e.getTitle(), e.links);
        }
        title = feed.title;
        return feed.entries;
    }

    public void updateLinks(String parentTitle, List<Link> links) {
        for (Link l : links) {
            if (!l.href.startsWith("http")) {
                l.href = FLIBUSTA + l.href;
                l.parentTitle = parentTitle;
            }
        }
    }

    @Override
    public void populateDataInUI(List<Entry> items) {
        searchAdapter.clearItems();
        searchAdapter.getItemsList().addAll(items);
        recyclerView.setAdapter(searchAdapter);

        titleView.setText("" + title);
    }



    public void onGridList() {
        if (searchAdapter == null) {
            return;
        }

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(searchAdapter);

    }

    @Override
    public boolean isBackPressed() {
        return onBackAction();
    }

    @Override
    public void notifyFragment() {
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
            populate();
        }
    }

    @Override
    public void resetFragment() {
        onGridList();
    }

}
