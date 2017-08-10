package com.foobnix.ui2.fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.opds.Entry;
import com.foobnix.opds.Feed;
import com.foobnix.opds.Hrefs;
import com.foobnix.opds.Link;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FontExtractor;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TTSModule;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.view.AlertDialogs;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class OpdsFragment2 extends UIFragment<Entry> {

    EntryAdapter searchAdapter;
    private FastScrollRecyclerView recyclerView;
    TextView titleView;

    String url = "/";
    String urlRoot = "";

    String title;
    Stack<String> stack = new Stack<String>();

    ImageView onPlus, onSearch;

    public OpdsFragment2() {
        super();
    }

    public static OpdsFragment2 newInstance(Bundle bundle) {
        OpdsFragment2 br = new OpdsFragment2();
        br.setArguments(bundle);
        return br;
    }

    public List<Entry> getAllCatalogs() {

        if (false) {
            String test = "http://flibusta.is/opds/authorsequence/88428/8457";
            return Arrays.asList(new Entry(test, test));
        }

        if (true) {
            return Arrays.asList(
                    //
                    new Entry("http://flibusta.is/opds", "Flibusta", "Книжное братство", "http://flibusta.is/favicon.ico"), //
                    new Entry("https://www.gitbook.com/api/opds/catalog.atom", "GitBook", "Public books are always free.", "https://www.gitbook.com/assets/images/logo/128.png"), //
                    new Entry("http://m.gutenberg.org/ebooks.opds/", "Project Gutenberg", "Free ebooks since 1971", "http://m.gutenberg.org/pics/favicon.png"), //
                    new Entry("http://manybooks.net/opds/index.php", "Manybooks", "Online Catalog for Manybooks.net", "http://manybooks.net/sites/all/themes/manybooks/images/library-books-icon.png")//
            );
        }

        try {
            InputStream open = new FileInputStream(new File(FontExtractor.getFontsDir(getActivity(), TTSModule.DICT_FOLDER), "catalogs.txt"));
            List<Entry> list = new ArrayList<Entry>();

            BufferedReader input = new BufferedReader(new InputStreamReader(open));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.contains(": ")) {
                    String[] v = line.split(": ");
                    LOG.d(v[0], v[1]);
                    list.add(new Entry(v[1].trim(), v[0].trim()));
                }
            }
            input.close();
            return list;
        } catch (Exception e) {
            LOG.e(e);

        }
        return Collections.EMPTY_LIST;

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
        onPlus = (ImageView) view.findViewById(R.id.onPlus);
        onSearch = (ImageView) view.findViewById(R.id.onSearch);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        onPlus.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Not implemented yet", Toast.LENGTH_SHORT).show();
            }
        });

        onSearch.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });
        onSearch.setVisibility(View.GONE);

        searchAdapter = new EntryAdapter();

        onGridList();

        searchAdapter.setOnItemClickListener(new ResultResponse<Entry>() {

            @Override
            public boolean onResultRecive(Entry result) {
                for (Link link : result.links) {
                    if (link.isOpdsLink()) {
                        onClickLink(link);
                    }
                }
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
                urlRoot = "";
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
        return "/";
    }

    public void onClickLink(final Link link) {
        if (link.isDisabled()) {
            Toast.makeText(getActivity(), R.string.can_t_download, Toast.LENGTH_SHORT).show();
        } else if (link.isWebLink()) {
            Urls.open(getActivity(), link.href);
        } else if (link.isOpdsLink()) {
            if (url.equals("/")) {
                urlRoot = link.href;
            }
            url = link.href;
            stack.push(url);
            populate();

        } else if (link.isImageLink()) {
        } else {
            LOG.d("Download >>", link.href);
            AlertDialogs.showDialog(getActivity(), link.getDownloadName(), getActivity().getString(R.string.download), new Runnable() {

                @Override
                public void run() {

                    final DownloadManager dm = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                    Request request = new Request(Uri.parse(link.href));
                    request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    LOG.d("Download to:", ExtUtils.LIRBI + "/" + link.getDownloadName());

                    File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ExtUtils.LIRBI);
                    root.mkdirs();

                    File file = new File(root, link.getDownloadName());
                    file.delete();

                    request.setDestinationUri(Uri.fromFile(file));
                    dm.enqueue(request);
                }
            });

        }
    }

    @Override
    public List<Entry> prepareDataInBackground() {
        if ("/".equals(url)) {
            title = getString(R.string.catalogs);
            return getAllCatalogs();
        }

        Feed feed = OPDS.getFeed(url);

        LOG.d("Load: >>>", feed.title, url);

        feed.updateLinksForUI();

        updateLinks(feed.title, urlRoot, feed.links);
        for (Entry e : feed.entries) {
            updateLinks(e.getTitle(), urlRoot, e.links);
        }
        title = feed.title;
        return feed.entries;
    }

    public void updateLinks(String parentTitle, String homeUrl, List<Link> links) {
        for (Link l : links) {
            Hrefs.fixHref(l, homeUrl);
            l.parentTitle = parentTitle;
        }
    }

    @Override
    public void populateDataInUI(List<Entry> entries) {
        searchAdapter.clearItems();
        searchAdapter.getItemsList().addAll(entries);
        recyclerView.setAdapter(searchAdapter);

        if (title != null) {
            titleView.setText("" + title);
        }
        onPlus.setVisibility(url == "/" ? View.VISIBLE : View.GONE);
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
