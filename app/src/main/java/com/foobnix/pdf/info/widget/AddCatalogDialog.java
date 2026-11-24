package com.foobnix.pdf.info.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.foobnix.android.utils.AsyncTasks;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.opds.Entry;
import com.foobnix.opds.Feed;
import com.foobnix.opds.Hrefs;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.MyProgressBar;
import com.foobnix.sys.TempHolder;

import com.foobnix.LibreraApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AddCatalogDialog {

    public static final String OPDS = "opds";

    public static void showDialogLogin(final Activity a, String url, final Runnable onRefresh) {
        LOG.d("showDialogLogin", url);
        AlertDialog.Builder builder = new AlertDialog.Builder(a);

        builder.setTitle(R.string.authentication_required);
        View dialog = LayoutInflater.from(a).inflate(R.layout.dialog_add_catalog_login, null, false);

        final SharedPreferences sp = a.getSharedPreferences(OPDS, Context.MODE_PRIVATE);

        final EditText login = (EditText) dialog.findViewById(R.id.login);

        final EditText password = (EditText) dialog.findViewById(R.id.password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // The login kept for this server, where it was saved: never the one of another
        final String string = sp.getString(Uri.parse(url).getHost(), "");
        final String[] split = string.split(TxtUtils.TTS_PAUSE);
        login.setText(split.length == 2 ? split[0] : "");
        password.setText(split.length == 2 ? split[1] : "");


        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        builder.setView(dialog);
        final AlertDialog infoDialog = builder.create();
        infoDialog.show();
        infoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String l = login.getText().toString().trim();
                String p = password.getText().toString().trim();
                if (TxtUtils.isEmpty(l) || TxtUtils.isEmpty(p)) {
                    Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }

                TempHolder.get().login = l;
                TempHolder.get().password = p;
                sp.edit().putString(Uri.parse(url).getHost(), l + TxtUtils.TTS_PAUSE + p).commit();
                LOG.d("showDialogLogin SAVE", Uri.parse(url).getHost(), url);

                infoDialog.dismiss();
                onRefresh.run();
            }

        });

    }

    private static final List<String> PATHS = Arrays.asList("/opds", "/opds/", "/opds-catalog", "/catalog",
                                                            "/feeds/opds", "/opds/v1.2/catalog", "/opds/v2/catalog",
                                                            "/opds-books/", "/feed.php", "/opds.xml", "/catalog.atom",
                                                            "/ebooks.opds/", "/api/opds");

    /**
     * Where a catalog may be, from what was typed: the address itself, the same with "/opds"
     * after it, and the places OPDS servers keep their catalog on that server (Calibre-Web
     * /opds, Calibre /opds, Kavita, Komga...). So the page of a server opened in a browser, its
     * login page too, or "192.168.1.5:8083" alone is enough. Typed without "http", a server at
     * home is asked over http first; one at home is asked the other way, too.
     */
    static List<String> candidates(String typed) {
        String text = typed.trim();
        final List<String> res = new ArrayList<>();
        if (text.isEmpty()) {
            return res;
        }
        final boolean schemeGiven = text.matches("(?i)^[a-z][a-z0-9+.-]*://.*");
        String bare = text.replaceFirst("(?i)^[a-z][a-z0-9+.-]*://", "");
        if (bare.isEmpty()) {
            return res;
        }
        final String host = bare.split("[/?#:]", 2)[0].toLowerCase(Locale.US);
        final boolean local = isLocal(host);
        final List<String> schemes = new ArrayList<>();
        if (schemeGiven) {
            schemes.add(text.substring(0, text.indexOf("://") + 3).toLowerCase(Locale.US));
        } else {
            schemes.add(local ? "http://" : "https://");
        }
        if (local) {
            final String other = schemes.get(0).equals("http://") ? "https://" : "http://";
            if (!schemes.contains(other)) {
                schemes.add(other);
            }
        }
        for (String scheme : schemes) {
            final String given = scheme + bare;
            final Uri uri = Uri.parse(given);
            if (uri.getHost() == null) {
                continue;
            }
            final String root = uri.getScheme() + "://" + uri.getEncodedAuthority();
            final String path = uri.getEncodedPath() == null ? "" : uri.getEncodedPath();
            add(res, given);
            if (path.length() > 1 && !path.toLowerCase(Locale.US).contains("opds")) {
                add(res, root + TxtUtils.replaceLast(path, "/", "") + "/opds");
            }
            for (String p : PATHS) {
                add(res, root + p);
            }
        }
        return res;
    }

    private static void add(List<String> list, String url) {
        if (!list.contains(url)) {
            list.add(url);
        }
    }

    private static boolean isLocal(String host) {
        return host.equals("localhost") || host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home")
                || host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$");
    }

    /** The server of an address as typed, "192.168.1.5:8083" too: the key its login is kept under. */
    private static String hostOf(String typed) {
        final List<String> all = candidates(typed);
        return all.isEmpty() ? null : Uri.parse(all.get(0)).getHost();
    }

    /** The login kept for the server of this address: {user, password}, empty when there is none. */
    static String[] savedLogin(Context c, String url) {
        final String host = hostOf(url);
        final String[] split = host == null ? new String[0]
                : c.getSharedPreferences(OPDS, Context.MODE_PRIVATE).getString(host, "").split(TxtUtils.TTS_PAUSE);
        return split.length == 2 ? split : new String[]{"", ""};
    }

    /**
     * Keeps the login given for the server of this address, the one the catalog is then asked
     * with. Both fields emptied forget it while a catalog is edited; a new one left without
     * keeps the login another catalog of that server has. Only a user name, or only a
     * password, is not kept.
     */
    static void saveLogin(Context c, String url, String user, String pass, boolean forgetIfEmpty) {
        final String host = hostOf(url);
        if (host == null) {
            return;
        }
        final SharedPreferences sp = c.getSharedPreferences(OPDS, Context.MODE_PRIVATE);
        if (TxtUtils.isEmpty(user) && TxtUtils.isEmpty(pass)) {
            if (forgetIfEmpty && sp.contains(host)) {
                sp.edit().remove(host).commit();
            }
        } else if (TxtUtils.isNotEmpty(user) && TxtUtils.isNotEmpty(pass)) {
            sp.edit().putString(host, user + TxtUtils.TTS_PAUSE + pass).commit();
        }
    }

    public static void showDialog(final Activity a, final Runnable onRefresh, final Entry e, final boolean validate) {

        AlertDialog.Builder builder = new AlertDialog.Builder(a);

        View dialog = LayoutInflater.from(a).inflate(R.layout.dialog_add_catalog, null, false);

        final EditText url = (EditText) dialog.findViewById(R.id.url);

        url.setText("https://");

        url.setSelection(url.getText().length());
        url.setEnabled(validate);

        final EditText name = (EditText) dialog.findViewById(R.id.name);
        final EditText description = (EditText) dialog.findViewById(R.id.description);
        final EditText login = (EditText) dialog.findViewById(R.id.login);
        final EditText password = (EditText) dialog.findViewById(R.id.password);
        final String optional = " (" + a.getString(R.string.optional) + ")";
        login.setHint(a.getString(R.string.user_name) + optional);
        password.setHint(a.getString(R.string.password) + optional);
        // A favourite link is saved as it is, with no check: no login asked for it
        login.setVisibility(validate ? View.VISIBLE : View.GONE);
        password.setVisibility(validate ? View.VISIBLE : View.GONE);
        final MyProgressBar MyProgressBar = (MyProgressBar) dialog.findViewById(R.id.MyProgressBarAdd);
        TintUtil.setDrawableTint(MyProgressBar.getIndeterminateDrawable().getCurrent(), TintUtil.color);
        final ImageView image = (ImageView) dialog.findViewById(R.id.image);
        final CheckBox addAsWEb = (CheckBox) dialog.findViewById(R.id.addAsWEb);
        addAsWEb.setVisibility(View.GONE);
        final String editAppState = e != null ? e.appState : null;
        if (editAppState != null) {
            String line[] = e.appState.replace(";", "").split(",");
            url.setText(line[0]);
            name.setText(line[1]);
            description.setText(line[2]);
            //ImageLoader.getInstance().displayImage(line[3], image, IMG.displayCacheMemoryDisc);
            Glide.with(LibreraApp.context).load(line[3]).into(image);

            if (e.logo != null) {
                image.setTag(e.logo);
            }
            final String[] saved = savedLogin(a, line[0]);
            login.setText(saved[0]);
            password.setText(saved[1]);
        }

        MyProgressBar.setVisibility(View.GONE);
        image.setVisibility(View.GONE);

        builder.setView(dialog);
        builder.setTitle(a.getString(R.string.add_catalog)+" (OPDS, Calibre)");

        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Keyboards.close(a);
            }
        });

        final AlertDialog infoDialog = builder.create();
        infoDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.close(a);

            }
        });
        infoDialog.show();

        url.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                infoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.add);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        infoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            AsyncTask asyncTask;

            @Override
            public void onClick(View v) {
                final String feedUrl = url.getText().toString().trim();
                saveLogin(a, feedUrl, login.getText().toString().trim(), password.getText().toString().trim(), editAppState != null);
                //url.setText(feedUrl);
                boolean isOk = infoDialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().equals(a.getString(R.string.ok));
                if (isOk || addAsWEb.isChecked() || !validate) {
                    Entry entry = new Entry();
                    entry.setAppState(feedUrl, name.getText().toString(), description.getText().toString(), image.getTag().toString());
                    if (editAppState != null) {
                        AppState.get().allOPDSLinks = AppState.get().allOPDSLinks.replace(editAppState, "");
                    }
                    AppState.get().allOPDSLinks = entry.appState + AppState.get().allOPDSLinks;
                    onRefresh.run();
                    infoDialog.dismiss();
                    AppProfile.save(a);
                    return;

                }

                if (AsyncTasks.isRunning(asyncTask)) {
                    AsyncTasks.toastPleaseWait(a);
                    return;
                }

                asyncTask = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object... params) {
                        // The catalog where it is given, else where servers keep it: a login
                        // asked for is a catalog found, the login is asked for it
                        for (final String uri : candidates(feedUrl)) {
                            LOG.d("OPDS-uri", uri);
                            Feed feed = com.foobnix.opds.OPDS.getFeed(uri, a);
                            if (feed != null && (feed.isNeedLoginPassword || !feed.entries.isEmpty())) {
                                LOG.d("OPDS-uri-success", uri, feed.isNeedLoginPassword);
                                a.runOnUiThread(() -> url.setText(uri));
                                return feed;
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPreExecute() {
                        MyProgressBar.setVisibility(View.VISIBLE);
                        image.setVisibility(View.GONE);
                    }


                    @Override
                    protected void onPostExecute(Object result) {
                        try {
                            MyProgressBar.setVisibility(View.GONE);
                            if (result == null || ((Feed) result).entries.isEmpty()) {
                                if (result != null && ((Feed) result).isNeedLoginPassword) {
                                    // Once the login is given the catalog is looked at again
                                    final String found = url.getText().toString().trim();
                                    AddCatalogDialog.showDialogLogin(a, found, () -> {
                                        // The login given there is the one the check goes on with
                                        final String[] given = savedLogin(a, found);
                                        login.setText(given[0]);
                                        password.setText(given[1]);
                                        infoDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                                    });

                                } else {
                                    Toast.makeText(a, a.getString(R.string.incorrect_value) + " OPDS " + feedUrl, Toast.LENGTH_LONG).show();
                                    infoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.add);
                                    addAsWEb.setVisibility(View.VISIBLE);
                                    name.setText(feedUrl);
                                    image.setTag("assets://opds/web.png");
                                }
                                return;
                            }
                            Feed feed = (Feed) result;
                            name.setText(TxtUtils.nullToEmpty(feed.title));
                            if (TxtUtils.isNotEmpty(feed.subtitle)) {
                                description.setText(TxtUtils.nullToEmpty(feed.subtitle));
                            }

                            if (feed.icon != null) {
                                image.setVisibility(View.VISIBLE);
                                feed.icon = Hrefs.fixHref(feed.icon, feed.homeUrl);
                                image.setTag(feed.icon);
                                LOG.d("feed.icon",feed.icon);
                                IMG.with(a).load(feed.icon).into(image);
                            } else {
                                image.setTag("assets://opds/web.png");
                            }

                            infoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.ok);
                        } catch (Exception e) {
                            LOG.e(e);
                        }

                    }
                }.execute();

            }
        });
    }

}
