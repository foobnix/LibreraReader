package com.foobnix.ui2.fragment;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.opds.Entry;
import com.foobnix.opds.Feed;
import com.foobnix.opds.Hrefs;
import com.foobnix.opds.Link;
import com.foobnix.opds.OPDS;
import com.foobnix.opds.SamlibOPDS;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.MyProgressBar;
import com.foobnix.pdf.info.widget.AddCatalogDialog;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.EntryAdapter;
import com.foobnix.ui2.fast.FastScrollRecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import okhttp3.CacheControl;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class OpdsFragment2 extends UIFragment<Entry> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.network, R.drawable.glyphicons_2_global);
    EntryAdapter searchAdapter;
    TextView titleView;

    String url = "/";
    String urlRoot = "";

    String title;
    Stack<String> stack = new Stack<String>();

    ImageView onPlus, onProxy;
    View pathContainer, view1, view2;
    long enqueue;
    TextView defaults, faq;
    ImageView starIcon;

    public OpdsFragment2() {
        super();
    }

    public static OpdsFragment2 newInstance(Bundle bundle) {
        OpdsFragment2 br = new OpdsFragment2();
        br.setArguments(bundle);
        return br;
    }

    public List<Entry> getAllCatalogs() {
        String[] list = AppState.get().myOPDSLinks.split(";");
        List<Entry> res = new ArrayList<Entry>();
        boolean hasStars = false;
        for (String line : list) {
            if (TxtUtils.isEmpty(line)) {
                continue;
            }
            if (line.contains("star_1.png")) {
                hasStars = true;
                continue;
            }
            String[] it = line.split(",");
            final Entry e = new Entry(it[0], it[1], it[2], it[3], true);
            e.appState = line + ";";
            res.add(e);
        }
        if (hasStars) {
            res.add(0, new Entry(SamlibOPDS.ROOT_FAVORITES, getString(R.string.favorites), getString(R.string.my_favorites_links), "assets://opds/star_1.png", true));
        }
        return res;

    }

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(pathContainer, TintUtil.color);
        ((FastScrollRecyclerView) recyclerView).myConfiguration();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_opds2, container, false);

        recyclerView = (FastScrollRecyclerView) view.findViewById(R.id.recyclerView);

        titleView = (TextView) view.findViewById(R.id.titleView);
        onPlus = (ImageView) view.findViewById(R.id.onPlus);
        onProxy = (ImageView) view.findViewById(R.id.onProxy);
        starIcon = (ImageView) view.findViewById(R.id.starIcon);
        pathContainer = view.findViewById(R.id.pathContainer);
        view1 = view.findViewById(R.id.view1);
        view2 = view.findViewById(R.id.view2);
        MyProgressBar = (MyProgressBar) view.findViewById(R.id.MyProgressBarOPDS);
        MyProgressBar.setVisibility(View.GONE);
        TintUtil.setDrawableTint(MyProgressBar.getIndeterminateDrawable().getCurrent(), Color.WHITE);

        onPlus.setOnClickListener(v -> AddCatalogDialog.showDialog(getActivity(), this::populate, null, true));

        searchAdapter = new EntryAdapter();

        defaults = (TextView) view.findViewById(R.id.defaults);
        faq = (TextView) view.findViewById(R.id.faq);
        defaults.setOnClickListener(v -> AlertDialogs.showOkDialog(getActivity(),
                getActivity().getString(R.string.restore_defaults_full), () -> {
                    AppState.get().myOPDSLinks = AppState.OPDS_DEFAULT;
                    url = "/";
                    populate();
                }));
        faq.setOnClickListener(v -> Urls.open(getActivity(), "https://wiki.mobileread.com/wiki/OPDS"));

        onGridList();

        searchAdapter.setOnItemClickListener(result -> {
            for (Link link : result.links) {
                if (link.isOpdsLink()) {
                    onClickLink(link);
                    break;
                }
            }

            return false;
        });

        searchAdapter.setOnRemoveLinkClickListener(result -> {
            AlertDialogs.showDialog(getActivity(), getActivity()
                            .getString(R.string.do_you_want_to_delete_) + " " + result.title,
                    getString(R.string.delete), () -> {
                        AppState.get().myOPDSLinks = AppState.get().myOPDSLinks.replace(result.appState, "");
                        url = "/";
                        populate();
                    });

            return false;
        });

        searchAdapter.setOnLinkClickListener(link -> {
            onClickLink(link);
            return false;
        });

        searchAdapter.setOnItemLongClickListener(result -> {
            if (url.equals("/")) {
                AddCatalogDialog.showDialog(getActivity(), this::populate, result, !SamlibOPDS.isSamlibUrl(result.homeUrl));
            }
            return false;
        });

        starIcon.setOnClickListener(v -> {
            final Entry entry = new Entry();
            String url2 = url;
            if (url2.contains("?")) {
                url2 = url2.substring(0, url2.indexOf("?"));
            }
            entry.setAppState(url, title, url2, "assets://opds/star_1.png");

            if (!AppState.get().myOPDSLinks.contains(url)) {
                AddCatalogDialog.showDialog(getActivity(), () -> {
                    starIcon.setImageResource(R.drawable.star_1);
                    TintUtil.setTintImageWithAlpha(starIcon, Color.WHITE);
                }, entry, false);
            } else {
                AppState.get().myOPDSLinks = AppState.get().myOPDSLinks.replace(entry.appState, "");
                starIcon.setImageResource(R.drawable.star_2);
                TintUtil.setTintImageWithAlpha(starIcon, Color.WHITE);
            }
        });

        view.findViewById(R.id.onBack).setOnClickListener(v -> onBackAction());

        view.findViewById(R.id.onHome).setOnClickListener(v -> {
            stack.clear();
            url = getHome();
            LOG.d("URLAction", "ADD", url);
            urlRoot = "";
            populate();
        });

        view.findViewById(R.id.onHome).setOnLongClickListener(v -> {
            AlertDialogs.showOkDialog(getActivity(), getActivity().getString(R.string.restore_defaults_full), () -> {
                AppState.get().myOPDSLinks = AppState.OPDS_DEFAULT;
                populate();
            });
            return true;
        });

        onProxy.setOnClickListener(v -> {
            ADS.hideAdsTemp(getActivity());

            final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            View view3 = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_proxy_server, null, false);

            final CheckBox proxyEnable = (CheckBox) view3.findViewById(R.id.proxyEnable);
            final CheckBox opdsLargeCovers = (CheckBox) view3.findViewById(R.id.opdsLargeCovers);
            final EditText proxyServer = (EditText) view3.findViewById(R.id.proxyServer);
            final EditText proxyPort = (EditText) view3.findViewById(R.id.proxyPort);
            final EditText proxyUser = (EditText) view3.findViewById(R.id.proxyUser);
            final EditText proxyPassword = (EditText) view3.findViewById(R.id.proxyPassword);

            final TextView proxyType = (TextView) view3.findViewById(R.id.proxyType);

            TintUtil.setBackgroundFillColor(view3.findViewById(R.id.section1), TintUtil.color);
            TintUtil.setBackgroundFillColor(view3.findViewById(R.id.section2), TintUtil.color);

            proxyEnable.setChecked(AppState.get().proxyEnable);
            proxyServer.setText(AppState.get().proxyServer);
            proxyPort.setText(AppState.get().proxyPort == 0 ? "" : "" + AppState.get().proxyPort);
            proxyUser.setText(AppState.get().proxyUser);
            proxyPassword.setText(AppState.get().proxyPassword);

            proxyEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (TxtUtils.isEmpty(proxyServer.getText().toString())) {
                        proxyServer.requestFocus();
                        proxyEnable.setChecked(false);
                        Toast.makeText(getContext(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    } else if ("0".equals(proxyPort.getText().toString()) || TxtUtils.isEmpty(proxyPort.getText().toString())) {
                        proxyPort.requestFocus();
                        proxyEnable.setChecked(false);
                        Toast.makeText(getContext(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    }
                }

            });

            TxtUtils.underline(proxyType, AppState.get().proxyType);

            proxyType.setOnClickListener(v1 -> {
                PopupMenu menu = new PopupMenu(v1.getContext(), v1);
                menu.getMenu().add(AppState.PROXY_HTTP).setOnMenuItemClickListener(item -> {
                    AppState.get().proxyType = AppState.PROXY_HTTP;
                    TxtUtils.underline(proxyType, AppState.get().proxyType);
                    return false;
                });
                menu.getMenu().add(AppState.PROXY_SOCKS).setOnMenuItemClickListener(item -> {
                    AppState.get().proxyType = AppState.PROXY_SOCKS;
                    TxtUtils.underline(proxyType, AppState.get().proxyType);
                    return false;
                });
                menu.show();
            });

            builder.setPositiveButton(R.string.apply, (dialog, which) -> {
                AppState.get().proxyEnable = proxyEnable.isChecked();
                AppState.get().proxyServer = proxyServer.getText().toString();

                try {
                    AppState.get().proxyPort = Integer.parseInt(proxyPort.getText().toString());
                } catch (Exception e) {
                    AppState.get().proxyPort = 0;
                }

                AppState.get().proxyUser = proxyUser.getText().toString().trim();
                AppState.get().proxyPassword = proxyPassword.getText().toString().trim();

                OPDS.buildProxy();

                AppProfile.save(getActivity());
                Keyboards.close(proxyServer);
            });

            builder.setNeutralButton(R.string.cancel, (dialog, which) -> {});

            opdsLargeCovers.setChecked(AppState.get().opdsLargeCovers);
            opdsLargeCovers.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().opdsLargeCovers = isChecked);

            final TextView downlodsPath = (TextView) view3.findViewById(R.id.downlodsPath);
            TxtUtils.underline(downlodsPath, TxtUtils.lastTwoPath(BookCSS.get().downlodsPath));
            downlodsPath.setOnClickListener(v12 -> ChooserDialogFragment.chooseFolder(getActivity(), BookCSS.get().downlodsPath)
                    .setOnSelectListener((nPath, dialog) -> {
                        BookCSS.get().downlodsPath = nPath;
                        TxtUtils.underline(downlodsPath, TxtUtils.lastTwoPath(BookCSS.get().downlodsPath));
                        dialog.dismiss();
                        return false;
                    }));

            builder.setView(view3);
            builder.show();
        });
        OPDS.buildProxy();

        populate();
        onTintChanged();

        return view;
    }

    public boolean onBackAction() {
        String last = popStack();

        boolean res = !getHome().equals(last);

        LOG.d("URLAction", last, url);

        if (last.equals(url)) {
            last = popStack();// two times
        }
        url = last;
        stack.push(url);
        LOG.d("URLAction", "ADD", url);

        populate();
        return res;
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
        LOG.d("onClickLink", link.type, link.href);
        if (link.filePath != null) {
            FileMeta meta = new FileMeta(link.filePath);
            meta.setTitle(link.getDownloadName());
            ExtUtils.openFile(getActivity(), meta);
        } else if (link.isDisabled()) {
            Toast.makeText(getActivity(), R.string.can_t_download, Toast.LENGTH_SHORT).show();
        } else if (link.isWebLink()) {
            Urls.open(getActivity(), link.href);
        } else if (link.isOpdsLink()) {
            if (url.equals("/")) {
                urlRoot = link.href;
            }
            url = link.href;
            stack.push(url);
            LOG.d("URLAction", "ADD", url);
            populate();

        } else if (link.isImageLink()) {
        } else {
            LOG.d("Download >>", link.href);
            if (isInProgress()) {
                Toast.makeText(getContext(), R.string.please_wait, Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialogs.showDialog(getActivity(), link.getDownloadName(), getActivity().getString(R.string.download), new Runnable() {
                String bookPath;

                @Override
                public void run() {

                    new AsyncTask() {

                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        protected Object doInBackground(Object... params) {

                            try {
                                OutputStream outStream = null;
                                String displayName = link.getDownloadName();
                                if (ExtUtils.isExteralSD(BookCSS.get().downlodsPath)) {
                                    String mimeType = ExtUtils.getMimeType(displayName);

                                    Uri uri = Uri.parse(BookCSS.get().downlodsPath);
                                    Uri childrenUri = ExtUtils.getChildUri(getContext(), uri);
                                    Uri createDocument = DocumentsContract.createDocument(getActivity().getContentResolver(), childrenUri, mimeType, displayName);

                                    bookPath = createDocument.toString();
                                    outStream = getActivity().getContentResolver().openOutputStream(createDocument);
                                } else {
                                    File LIRBI_DOWNLOAD_DIR;
                                    if (TxtUtils.isNotEmpty(link.author)) {
                                        LIRBI_DOWNLOAD_DIR = new File(BookCSS.get().downlodsPath, TxtUtils.fixFileName(link.author));
                                    } else {
                                        LIRBI_DOWNLOAD_DIR = new File(BookCSS.get().downlodsPath);
                                    }
                                    if (!LIRBI_DOWNLOAD_DIR.exists()) {
                                        LIRBI_DOWNLOAD_DIR.mkdirs();
                                    }

                                    File file = null;
                                    try {
                                        file = new File(LIRBI_DOWNLOAD_DIR, displayName);
                                        file.delete();
                                        outStream = new FileOutputStream(file);
                                    } catch (FileNotFoundException e1) {
                                        try {
                                            file = new File(LIRBI_DOWNLOAD_DIR, TxtUtils.substringSmart(displayName, 50) + "." + ExtUtils.getFileExtension(displayName));
                                            file.delete();
                                            outStream = new FileOutputStream(file);
                                        } catch (FileNotFoundException e2) {
                                            file = new File(LIRBI_DOWNLOAD_DIR, displayName.hashCode() + "." + ExtUtils.getFileExtension(displayName));
                                            file.delete();
                                            outStream = new FileOutputStream(file);
                                        }

                                    }


                                    bookPath = file.getPath();
                                }

                                String href = link.href;

                                // fix manybooks
                                okhttp3.Request request = new okhttp3.Request.Builder()//
                                        .header("User-Agent", OPDS.USER_AGENT).cacheControl(new CacheControl.Builder().noCache().build()).url(href)//
                                        .build();//

                                Response response = OPDS.client//
                                        .newCall(request)//
                                        .execute();

                                BufferedSink sink = Okio.buffer(Okio.sink(outStream));
                                sink.writeAll(response.body().source());
                                sink.close();

                                outStream.close();

                                LOG.d("Download finish");

                            } catch (Exception e) {
                                LOG.e(e);
                                return false;
                            }
                            return true;

                        }

                        @Override
                        protected void onPreExecute() {
                            MyProgressBar.setVisibility(View.VISIBLE);
                        }

                        ;

                        @Override
                        protected void onPostExecute(Object result) {
                            MyProgressBar.setVisibility(View.GONE);
                            if ((Boolean) result == false) {
                                Toast.makeText(getContext(), R.string.loading_error, Toast.LENGTH_LONG).show();
                                // Urls.openWevView(getActivity(), link.href, null);
                            } else {
                                link.filePath = bookPath;

                                if (!ExtUtils.isExteralSD(bookPath)) {
                                    FileMeta meta = AppDB.get().getOrCreate(bookPath);
                                    meta.setIsSearchBook(true);
                                    AppDB.get().updateOrSave(meta);
                                    //IMG.loadCoverPageWithEffect(meta.getPath(), IMG.getImageSize());
                                }
                                TempHolder.listHash++;

                            }
                            clearEmpty();
                        }
                    }.execute();
                }
            });
        }
    }

    public void clearEmpty() {
        if (ExtUtils.isExteralSD(BookCSS.get().downlodsPath)) {
            searchAdapter.notifyDataSetChanged();
            return;
        }

        try {
            File LIRBI_DOWNLOAD_DIR = new File(BookCSS.get().downlodsPath);

            if (!LIRBI_DOWNLOAD_DIR.exists()) {
                LIRBI_DOWNLOAD_DIR.mkdirs();
            }

            for (String file : LIRBI_DOWNLOAD_DIR.list()) {
                File f = new File(LIRBI_DOWNLOAD_DIR, file);
                if (f.length() == 0) {
                    LOG.d("Delete file", f.getPath());
                    f.delete();
                }
            }

            searchAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    boolean isNeedLoginPassword = false;

    public List<Entry> allCatalogs = new ArrayList<Entry>();

    @Override
    public List<Entry> prepareDataInBackground() {
        try {
            LOG.d("OPDS URL", url);
            if ("/".equals(url)) {
                title = getString(R.string.catalogs);
                return allCatalogs = getAllCatalogs();
            }

            if (SamlibOPDS.isSamlibUrl(url)) {
                Pair<List<Entry>, String> pair = SamlibOPDS.getSamlibResult(url);
                List<Entry> samlibResult = pair.first;
                title = pair.second.replace(SamlibOPDS.ROOT_FAVORITES, getString(R.string.favorites)).replace(SamlibOPDS.ROOT_AWARDS, getString(R.string.awards));
                return samlibResult;
            }

            Feed feed = OPDS.getFeed(url, getContext());
            if (feed == null) {
                return Collections.emptyList();
            }
            isNeedLoginPassword = feed.isNeedLoginPassword;

            LOG.d("Load: >>>", feed.title, url);

            feed.updateLinksForUI();

            if (urlRoot.contains("My:")) {
                urlRoot = url;
            }

            updateLinks(feed.title, urlRoot, feed.links);

            for (Link link : feed.links) {
                if ("next".equals(link.rel)) {
                    feed.entries.add(new Entry("Next", link));
                    break;
                }
            }

            for (Entry e : feed.entries) {
                updateLinks(e.getTitle(), urlRoot, e.links);
                if (e.authorUrl != null) {
                    e.authorUrl = Hrefs.fixHref(e.authorUrl, urlRoot);
                }
            }
            title = TxtUtils.nullToEmpty(feed.title).replace("\n", "").replace("\r", "").trim();
            return feed.entries;
        } catch (Exception e) {
            LOG.e(e);
            return Collections.emptyList();

        }
    }

    public void updateLinks(String parentTitle, String homeUrl, List<Link> links) {
        Link alternative = null;
        for (Link l : links) {
            Hrefs.fixHref(l, homeUrl);
            l.parentTitle = parentTitle;
            File book = new File(BookCSS.get().downlodsPath, l.getDownloadName());
            if (book.isFile()) {
                l.filePath = book.getPath();
            }
            if (l.href != null) {

                if (l.href.startsWith("http://manybooks.net/opds/")) {
                    l.type = Link.APPLICATION_ATOM_XML;
                }
                String manyUrl = "http://manybooks.net/send/1:epub:.epub:epub/";
                if (l.href.startsWith(manyUrl)) {
                    String url = l.href.replace(manyUrl, "http://idownload.manybooks.net/");
                    alternative = new Link(url, l.type);
                    alternative.rel = l.rel;
                    alternative.parentTitle = "2." + parentTitle;
                    File book1 = new File(BookCSS.get().downlodsPath, alternative.getDownloadName());
                    if (book1.isFile()) {
                        alternative.filePath = book1.getPath();
                    }

                }
            }

        }
        if (alternative != null) {
            links.add(alternative);
        }
    }

    @Override
    public void populateDataInUI(List<Entry> entries) {
        if (isNeedLoginPassword) {
            AddCatalogDialog.showDialogLogin(getActivity(), url, () -> populate());
            return;
        }

        if (entries == null || entries.isEmpty()) {
            if ("/".equals(url)) {
                return;
            }
            Urls.openWevView(getActivity(), url, new Runnable() {

                @Override
                public void run() {
                    url = popStack();
                }
            });
            url = popStack();
            return;
        }

        searchAdapter.clearItems();
        searchAdapter.getItemsList().addAll(entries);
        recyclerView.setAdapter(searchAdapter);

        if (title != null) {
            titleView.setText("" + title.replaceAll("[\n\r\t ]+", " ").trim());
        }
        int isHomeVisible = url == "/" ? View.VISIBLE : View.GONE;
        onPlus.setVisibility(isHomeVisible);
        defaults.setVisibility(isHomeVisible);
        faq.setVisibility(isHomeVisible);
        onProxy.setVisibility(isHomeVisible);
        view1.setVisibility(isHomeVisible);

        starIcon.setVisibility(url == "/" ? View.GONE : View.VISIBLE);
        for (Entry cat : allCatalogs) {
            if (url.equals(cat.homeUrl)) {
                starIcon.setVisibility(View.GONE);
                break;
            }
        }

        if (AppState.get().myOPDSLinks.contains(url)) {
            starIcon.setImageResource(R.drawable.star_1);
        } else {
            starIcon.setImageResource(R.drawable.star_2);
        }
        TintUtil.setTintImageWithAlpha(starIcon, Color.WHITE);
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
        if (isInProgress()) {
            Toast.makeText(getContext(), R.string.please_wait, Toast.LENGTH_SHORT).show();
            return true;
        }
        return onBackAction();
    }

    @Override
    public void notifyFragment() {
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
            // populate();
        }
    }

    @Override
    public void resetFragment() {
        onGridList();
    }

}
