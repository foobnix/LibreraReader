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
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.foobnix.pdf.info.databinding.DialogProxyServerBinding;
import com.foobnix.pdf.info.databinding.FragmentOpds2Binding;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.AlertDialogs;
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
    public static final Pair<Integer, Integer> PAIR = new Pair<>(R.string.network, R.drawable.glyphicons_2_global);
    EntryAdapter searchAdapter;

    String url = "/";
    String urlRoot = "";

    String title;
    Stack<String> stack = new Stack<>();

    private FragmentOpds2Binding opds2Binding;

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
            String test = "https://books.fbreader.org/opds";
            return Collections.singletonList(new Entry(test, test));
        }

        String[] list = AppState.get().myOPDSLinks.split(";");
        List<Entry> res = new ArrayList<>();
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
        TintUtil.setBackgroundFillColor(opds2Binding.pathContainer, TintUtil.color);
        ((FastScrollRecyclerView) recyclerView).myConfiguration();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        opds2Binding = FragmentOpds2Binding.inflate(inflater, container, false);

        recyclerView = opds2Binding.recyclerView;

        myProgressBar = opds2Binding.myProgressBarOPDS;
        myProgressBar.setVisibility(View.GONE);
        TintUtil.setDrawableTint(myProgressBar.getIndeterminateDrawable().getCurrent(), Color.WHITE);

        opds2Binding.onPlus.setOnClickListener(v -> AddCatalogDialog.showDialog(getActivity(), this::populate, null, true));

        searchAdapter = new EntryAdapter();

        opds2Binding.defaults.setOnClickListener(v -> AlertDialogs.showOkDialog(getActivity(),
                getString(R.string.restore_defaults_full), () -> {
                    AppState.get().myOPDSLinks = AppState.OPDS_DEFAULT;
                    url = "/";
                    populate();
                }));
        opds2Binding.faq.setOnClickListener(v -> Urls.open(getActivity(), "https://wiki.mobileread.com/wiki/OPDS"));

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
            AlertDialogs.showDialog(getActivity(), getString(R.string.do_you_want_to_delete_)
                    + " " + result.title, getString(R.string.delete), () -> {
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
                AddCatalogDialog.showDialog(getActivity(), this::populate, result,
                        !SamlibOPDS.isSamlibUrl(result.homeUrl));
            }
            return false;
        });

        opds2Binding.starIcon.setOnClickListener(v -> {
            final Entry entry = new Entry();
            String url2 = url;
            if (url2.contains("?")) {
                url2 = url2.substring(0, url2.indexOf("?"));
            }
            entry.setAppState(url, title, url2, "assets://opds/star_1.png");

            if (!AppState.get().myOPDSLinks.contains(url)) {
                AddCatalogDialog.showDialog(getActivity(), () -> {
                    opds2Binding.starIcon.setImageResource(R.drawable.star_1);
                    TintUtil.setTintImageWithAlpha(opds2Binding.starIcon, Color.WHITE);
                }, entry, false);
            } else {
                AppState.get().myOPDSLinks = AppState.get().myOPDSLinks.replace(entry.appState, "");
                opds2Binding.starIcon.setImageResource(R.drawable.star_2);
                TintUtil.setTintImageWithAlpha(opds2Binding.starIcon, Color.WHITE);
                // AlertDialogs.showOkDialog(getActivity(),
                // getActivity().getString(R.string.do_you_want_to_delete_), new Runnable() {
                //
                // @Override
                // public void run() {
                //
                // // url = "/";
                // }
                // });
            }
        });

        opds2Binding.onBack.setOnClickListener(v -> onBackAction());

        opds2Binding.onHome.setOnClickListener(v -> {
            stack.clear();
            url = getHome();
            LOG.d("URLAction", "ADD", url);
            urlRoot = "";
            populate();
        });

        opds2Binding.onHome.setOnLongClickListener(v -> {
            AlertDialogs.showOkDialog(getActivity(), getString(R.string.restore_defaults_full), () -> {
                AppState.get().myOPDSLinks = AppState.OPDS_DEFAULT;
                populate();
            });
            return true;
        });

        opds2Binding.onProxy.setOnClickListener(v -> {
            ADS.hideAdsTemp(getActivity());

            final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            final DialogProxyServerBinding proxyServerBinding = DialogProxyServerBinding.inflate(LayoutInflater.from(v.getContext()));

            TintUtil.setBackgroundFillColor(proxyServerBinding.section1, TintUtil.color);
            TintUtil.setBackgroundFillColor(proxyServerBinding.section2, TintUtil.color);

            proxyServerBinding.proxyEnable.setChecked(AppState.get().proxyEnable);
            proxyServerBinding.proxyServer.setText(AppState.get().proxyServer);
            proxyServerBinding.proxyPort.setText(AppState.get().proxyPort == 0 ? "" : "" + AppState.get().proxyPort);
            proxyServerBinding.proxyUser.setText(AppState.get().proxyUser);
            proxyServerBinding.proxyPassword.setText(AppState.get().proxyPassword);

            proxyServerBinding.proxyEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (TxtUtils.isEmpty(proxyServerBinding.proxyServer.getText().toString())) {
                        proxyServerBinding.proxyServer.requestFocus();
                        proxyServerBinding.proxyEnable.setChecked(false);
                        Toast.makeText(getContext(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    } else if ("0".equals(proxyServerBinding.proxyPort.getText().toString()) ||
                            TxtUtils.isEmpty(proxyServerBinding.proxyPort.getText().toString())) {
                        proxyServerBinding.proxyPort.requestFocus();
                        proxyServerBinding.proxyEnable.setChecked(false);
                        Toast.makeText(getContext(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            TxtUtils.underline(proxyServerBinding.proxyType, AppState.get().proxyType);

            proxyServerBinding.proxyType.setOnClickListener(v1 -> {
                PopupMenu menu = new PopupMenu(v1.getContext(), v1);
                menu.getMenu().add(AppState.PROXY_HTTP).setOnMenuItemClickListener(item -> {
                    AppState.get().proxyType = AppState.PROXY_HTTP;
                    TxtUtils.underline(proxyServerBinding.proxyType, AppState.get().proxyType);
                    return false;
                });
                menu.getMenu().add(AppState.PROXY_SOCKS).setOnMenuItemClickListener(item -> {
                    AppState.get().proxyType = AppState.PROXY_SOCKS;
                    TxtUtils.underline(proxyServerBinding.proxyType, AppState.get().proxyType);
                    return false;
                });
                menu.show();
            });

            builder.setPositiveButton(R.string.apply, (dialog, which) -> {
                AppState.get().proxyEnable = proxyServerBinding.proxyEnable.isChecked();
                AppState.get().proxyServer = proxyServerBinding.proxyServer.getText().toString();

                try {
                    AppState.get().proxyPort = Integer.parseInt(proxyServerBinding.proxyPort.getText().toString());
                } catch (Exception e) {
                    AppState.get().proxyPort = 0;
                }

                AppState.get().proxyUser = proxyServerBinding.proxyUser.getText().toString().trim();
                AppState.get().proxyPassword = proxyServerBinding.proxyPassword.getText().toString().trim();

                OPDS.buildProxy();

                AppProfile.save(getActivity());
                Keyboards.close(proxyServerBinding.proxyServer);
            });

            builder.setNeutralButton(R.string.cancel, (dialog, which) -> { });

            proxyServerBinding.opdsLargeCovers.setChecked(AppState.get().opdsLargeCovers);
            proxyServerBinding.opdsLargeCovers.setOnCheckedChangeListener((buttonView, isChecked) -> AppState.get().opdsLargeCovers = isChecked);

            TxtUtils.underline(proxyServerBinding.downloadsPath, TxtUtils.lastTwoPath(BookCSS.get().downlodsPath));
            proxyServerBinding.downloadsPath.setOnClickListener(v12 -> ChooserDialogFragment.chooseFolder(requireActivity(),
                    BookCSS.get().downlodsPath).setOnSelectListener((nPath, dialog) -> {
                BookCSS.get().downlodsPath = nPath;
                TxtUtils.underline(proxyServerBinding.downloadsPath, TxtUtils.lastTwoPath(BookCSS.get().downlodsPath));
                dialog.dismiss();
                return false;
            }));

            builder.setView(proxyServerBinding.getRoot());
            builder.show();
        });
        OPDS.buildProxy();

        populate();
        onTintChanged();

        return opds2Binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        opds2Binding = null;
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

            AlertDialogs.showDialog(getActivity(), link.getDownloadName(), getString(R.string.download), new Runnable() {
                String bookPath;

                @Override
                public void run() {
                    new AsyncTask() {
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        protected Object doInBackground(Object... params) {
                            try {
                                OutputStream outStream;
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

                                    File file;
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
                            myProgressBar.setVisibility(View.VISIBLE);
                        }

                        @Override
                        protected void onPostExecute(Object result) {
                            myProgressBar.setVisibility(View.GONE);
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
            AddCatalogDialog.showDialogLogin(getActivity(), url, this::populate);
            return;
        }

        if (entries == null || entries.isEmpty()) {
            if ("/".equals(url)) {
                return;
            }
            Urls.openWevView(getActivity(), url, () -> url = popStack());
            url = popStack();
            return;
        }

        searchAdapter.clearItems();
        searchAdapter.getItemsList().addAll(entries);
        recyclerView.setAdapter(searchAdapter);

        if (title != null) {
            opds2Binding.titleView.setText("" + title.replaceAll("[\n\r\t ]+", " ").trim());
        }
        int isHomeVisible = url.equals("/") ? View.VISIBLE : View.GONE;
        opds2Binding.onPlus.setVisibility(isHomeVisible);
        opds2Binding.defaults.setVisibility(isHomeVisible);
        opds2Binding.faq.setVisibility(isHomeVisible);
        opds2Binding.onProxy.setVisibility(isHomeVisible);
        opds2Binding.view1.setVisibility(isHomeVisible);

        opds2Binding.starIcon.setVisibility(url.equals("/") ? View.GONE : View.VISIBLE);
        for (Entry cat : allCatalogs) {
            if (url.equals(cat.homeUrl)) {
                opds2Binding.starIcon.setVisibility(View.GONE);
                break;
            }
        }

        if (AppState.get().myOPDSLinks.contains(url)) {
            opds2Binding.starIcon.setImageResource(R.drawable.star_1);
        } else {
            opds2Binding.starIcon.setImageResource(R.drawable.star_2);
        }
        TintUtil.setTintImageWithAlpha(opds2Binding.starIcon, Color.WHITE);
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
