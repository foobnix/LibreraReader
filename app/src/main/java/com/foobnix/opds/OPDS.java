package com.foobnix.opds;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.XmlParser;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.widget.AddCatalogDialog;
import com.foobnix.pdf.info.wrapper.PasswordState;
import com.foobnix.sys.TempHolder;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;

public class OPDS {
    public static final String CODE_401 = "401";
    final static Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<String, CachingAuthenticator>();
    static Cache cache = new Cache(CacheZipUtils.CACHE_WEB, 5 * 1024 * 1024);

    public static OkHttpClient.Builder builder = new OkHttpClient.Builder()//
            .cookieJar(new WebviewCookieHandler())//
            // Once: it was added again with every login sent, one more on each request
            .addInterceptor(new AuthenticationCacheInterceptor(authCache))//
            .connectTimeout(20, TimeUnit.SECONDS)//
            .writeTimeout(20, TimeUnit.SECONDS)//
            .readTimeout(30, TimeUnit.SECONDS)//
            .cache(cache);//

    public static OkHttpClient client = builder.build();//


    public static int random = new Random().nextInt();
    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; Librera) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.2997." + random + " Safari/537.36";

    public static void buildProxy() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (AppState.get().proxyEnable && TxtUtils.isNotEmpty(AppState.get().proxyServer) && AppState.get().proxyPort != 0) {
                    Type http = AppState.PROXY_SOCKS.equals(AppState.get().proxyType) ? Type.SOCKS : Type.HTTP;
                    LOG.d("Proxy: Server", http.name(), AppState.get().proxyServer, AppState.get().proxyPort);
                    builder.proxy(new Proxy(http, new InetSocketAddress(AppState.get().proxyServer, AppState.get().proxyPort)));
                    if (TxtUtils.isNotEmpty(AppState.get().proxyUser)) {
                        LOG.d("Proxy: User", AppState.get().proxyUser);
                        builder.proxyAuthenticator(new BasicAuthenticator(new Credentials(AppState.get().proxyUser, PasswordState.get().proxyPassword)));
                    }
                } else {
                    builder.proxy(null);
                }
                client = builder.build();
            }
        }, "@T buildProxy").start();

    }

    /**
     * The login answered only to the server it was given for. The client is one for the whole
     * app - covers, shared links, fonts go through it too - and any of their servers asking for
     * a login would otherwise have been sent the catalogue's.
     */
    static Authenticator forHost(String url, final Authenticator authenticator) {
        final HttpUrl parsed = HttpUrl.parse(url);
        final String host = parsed == null ? null : parsed.host();
        return new Authenticator() {
            @Override public Request authenticate(Route route, Response response) throws IOException {
                if (host == null || !host.equalsIgnoreCase(response.request().url().host())) {
                    return null;
                }
                return authenticator.authenticate(route, response);
            }
        };
    }

    public static String getHttpResponseNoException(String url) {
        try {
            return getHttpResponse(url, "", "");
        } catch (Exception e) {
            return "";
        }

    }

    public static String getHttpResponse(String url, String login, String password) throws IOException {
        LOG.d("getHttpResponse", url, login);

        Request request = new Request.Builder()//
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", AppState.get().getAppLang())

                .cacheControl(new CacheControl.Builder()//
                        .maxAge(10, TimeUnit.MINUTES)//
                        .build())//
                .url(url)//
                .build();//

        Response response = client//
                .newCall(request)//
                .execute();


        LOG.d("Header: >>", url);
        LOG.d("Header: Status code:", response.code());

        for (int i = 0; i < response.headers().size(); i++) {
            String name = response.headers().name(i);
            String value = response.headers().value(i);
            LOG.d("Header: ", name, value);
        }

        if (response.code() == 401 && TxtUtils.isEmpty(login)) {
            response.close();
            return CODE_401;
        } else if (response.code() == 401 || response.code() == 400) {
            // Asked again with the login only when the server asked for it: every page was
            // fetched twice before

            {
                Credentials credentials = new Credentials(login, password);
                final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);
                final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);

                DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()//
                        .with("digest", digestAuthenticator)//
                        .with("basic", basicAuthenticator)//
                        .build();


                client = builder //
                        .authenticator(forHost(url, new CachingAuthenticatorDecorator(authenticator, authCache))) //
                        .build();

                LOG.d("getHttpResponse Credentials", login);

                response.close();
                response = client.newCall(request).execute();

                LOG.d("getHttpResponse response.code 1", response.code());
            }


            if (response.code() == 401 || response.code() == 400) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.e(e);
                }
                response.close();

                {
                    Credentials credentials = new Credentials(login, password);
                    final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);
                    final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);

                    DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()//
                            .with("digest", digestAuthenticator)//
                            .with("basic", basicAuthenticator)//
                            .build();


                    client = builder //
                            .authenticator(forHost(url, new CachingAuthenticatorDecorator(authenticator, authCache))) //
                                .build();

                    response = client.newCall(request).execute();
                }

                LOG.d("getHttpResponse response.code 2", response.code());

                if (response.code() == 401 || response.code() == 400) {
                    response.close();
                    return CODE_401;
                }
            }
        }

        String string = response.body().string();
        string = string.replace("\u0001M\u0001J", "");
        response.close();
        return string;
    }

    public static ResponseBody getHttpResponseBody(String url) throws IOException {

        Request request = new Request.Builder()//
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", AppState.get().getAppLang())

                .cacheControl(new CacheControl.Builder()//
                        .maxAge(10, TimeUnit.MINUTES)//
                        .build())//
                .url(url)//
                .build();//

        Response response = client//
                .newCall(request)//
                .execute();


        LOG.d("Header: >>", url);
        LOG.d("Header: Status code:", response.code());

        for (int i = 0; i < response.headers().size(); i++) {
            String name = response.headers().name(i);
            String value = response.headers().value(i);
            LOG.d("Header: ", name, value);
        }

        if (response.code() == 401 && TxtUtils.isEmpty(TempHolder.get().login)) {
            return response.body();
        } else {

            Credentials credentials = new Credentials(TempHolder.get().login, TempHolder.get().password);
            final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);
            final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);

            DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()//
                    .with("digest", digestAuthenticator)//
                    .with("basic", basicAuthenticator)//
                    .build();


            client = builder //
                    .authenticator(forHost(url, new CachingAuthenticatorDecorator(authenticator, authCache))) //
                    .build();

            response.close();
            response = client.newCall(request).execute();

            if (response.code() == 401) {
                return response.body();
            }
        }

        return response.body();
    }

    /** The Atom search address read from each OpenSearch description, "" where it has none. */
    private static final Map<String, String> openSearch = new ConcurrentHashMap<String, String>();

    /**
     * A catalog that points to an OpenSearch description for its search, and has no search
     * address of its own (Ebooks libres et gratuits, textos.info...), had no search field:
     * its description is read once and its Atom address becomes the feed's search link.
     */
    public static void resolveOpenSearch(Feed feed, String feedUrl, Context c) {
        for (Link l : feed.links) {
            if (l.isSearchLink()) {
                return;
            }
        }
        for (Link l : feed.links) {
            if (!"search".equals(l.rel) || l.href == null || l.type == null
                    || !l.type.startsWith("application/opensearchdescription+xml")) {
                continue;
            }
            final String osd = Hrefs.fixHref(l.href, feedUrl);
            String template = openSearch.get(osd);
            if (template == null) {
                template = openSearchTemplate(osd, c);
                openSearch.put(osd, template);
            }
            if (TxtUtils.isNotEmpty(template)) {
                l.href = template;
                l.type = Link.APPLICATION_ATOM_XML;
            }
            return;
        }
    }

    private static String openSearchTemplate(String osd, Context c) {
        try {
            final String xml = responseWithSavedLogin(osd, c);
            final XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new StringReader(xml));
            for (int event = xpp.getEventType(); event != XmlPullParser.END_DOCUMENT; event = xpp.next()) {
                if (event == XmlPullParser.START_TAG && "Url".equalsIgnoreCase(xpp.getName())) {
                    final String type = xpp.getAttributeValue(null, "type");
                    final String template = xpp.getAttributeValue(null, "template");
                    if (type != null && type.startsWith(Link.APPLICATION_ATOM_XML) && template != null) {
                        // Optional parameters ({startPage?}) left out, as a search asks for none
                        return Hrefs.fixHref(template.replaceAll("\\{[^}]*\\?\\}", ""), osd);
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(e, osd);
        }
        return "";
    }

    /** The answer of a server, with the login kept for it when there is one. */
    private static String responseWithSavedLogin(String url, Context c) throws IOException {
        final SharedPreferences sp = c.getSharedPreferences(AddCatalogDialog.OPDS, android.content.Context.MODE_PRIVATE);
        final String[] split = sp.getString(Uri.parse(url).getHost(), "").split(TxtUtils.TTS_PAUSE);
        return split.length == 2 ? getHttpResponse(url, split[0], split[1]) : getHttpResponse(url, "", "");
    }

    public static Feed getFeed(String url, Context c) {
        try {
            if (url.endsWith(SamlibOPDS.LIBRERA_MOBI)) {
                return null;
            }
            final SharedPreferences sp = c.getSharedPreferences(AddCatalogDialog.OPDS, android.content.Context.MODE_PRIVATE);
            final String host = Uri.parse(url).getHost();
            LOG.d("uri-host", host);
            final String string = sp.getString(host, "");
            String res = "";
            if (TxtUtils.isNotEmpty(string)) {
                try {
                    final String[] split = string.split(TxtUtils.TTS_PAUSE);
                    String login = split[0];
                    String password = split[1];
                    res = getHttpResponse(url, login, password);
                } catch (Exception e) {
                    LOG.e(e);
                    res = getHttpResponse(url, "", "");
                }
            } else {
                res = getHttpResponse(url, "", "");
            }


            if (res.equals(CODE_401) || res.equals("400")) {
                authCache.clear();
                Feed feed = new Feed(url);
                feed.isNeedLoginPassword = true;
                return feed;
            }
            return getFeedByXml(url, res);
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;

    }

    public static Feed getFeedByXml(String url, String xmlString) throws Exception {
        LOG.d("getFeedByXml", xmlString);

        XmlPullParser xpp = XmlParser.buildPullParser();
        xpp.setInput(new StringReader(xmlString));

        int eventType = xpp.getEventType();

        Feed feed = new Feed(url);

        Entry entry = null;

        boolean isEntry = false;
        boolean isAuthor = false;
        boolean isContent = false, isTitle = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (!isEntry) {
                    if ("title".equals(xpp.getName())) {
                        feed.title = xpp.nextText();
                    }
                    if ("subtitle".equals(xpp.getName())) {
                        feed.subtitle = xpp.nextText();
                    }
                    if ("updated".equals(xpp.getName())) {
                        feed.updated = xpp.nextText();
                    }
                    if ("icon".equals(xpp.getName())) {
                        feed.icon = xpp.nextText();
                    }
                    if ("link".equals(xpp.getName())) {
                        feed.links.add(new Link(xpp));
                    }
                }

                if ("entry".equals(xpp.getName())) {
                    entry = new Entry();
                    isEntry = true;
                }
                if ("author".equals(xpp.getName())) {
                    isAuthor = true;
                }
                if (isEntry && "content".equals(xpp.getName())) {
                    isContent = true;
                }
                if (isEntry && "title".equals(xpp.getName())) {
                    isTitle = true;
                }

                if (isEntry) {
                    if ("summary".equals(xpp.getName())) {
                        entry.summary = xpp.nextText();
                    }
                    if ("dc:issued".equals(xpp.getName())) {
                        entry.year = xpp.nextText();
                    }
                    if ("updated".equals(xpp.getName())) {
                        entry.updated = xpp.nextText();
                    }
                    if ("id".equals(xpp.getName())) {
                        entry.id = xpp.nextText();
                    }
                    if (isAuthor && "name".equals(xpp.getName())) {
                        entry.author = xpp.nextText();
                    }
                    if (isAuthor && "uri".equals(xpp.getName())) {
                        entry.authorUrl = xpp.nextText();
                    }
                    if ("category".equals(xpp.getName())) {
                        String label = xpp.getAttributeValue(null, "label");
                        String term = xpp.getAttributeValue(null, "term");
                        if (TxtUtils.isNotEmpty(label)) {
                            entry.category += ", " + label;
                        } else if (TxtUtils.isNotEmpty(term)) {
                            entry.category += ", " + term;
                        }
                    }

                    if ("link".equals(xpp.getName())) {
                        entry.links.add(new Link(xpp));
                    }
                }

            }
            if (eventType == XmlPullParser.TEXT) {
                if (isContent) {
                    String text = xpp.getText();
                    if (TxtUtils.isNotEmpty(text) && !text.equals("\n") && !text.equals("\r")) {
                        text = text.replace(":\n", ":").trim();
                        entry.content += text + "<br/>";
                    }
                }
                if (isTitle) {
                    entry.title += " " + xpp.getText();
                }
            }
            if (eventType == XmlPullParser.END_TAG) {
                if ("entry".equals(xpp.getName())) {
                    isEntry = false;
                    entry.category = TxtUtils.replaceFirst(entry.category, ", ", "");
                    feed.entries.add(entry);
                }
                if ("author".equals(xpp.getName())) {
                    isAuthor = false;
                }
                if ("content".equals(xpp.getName())) {
                    isContent = false;
                }
                if ("title".equals(xpp.getName())) {
                    isTitle = false;
                }
            }

            eventType = xpp.next();
        }
        return feed;

    }

}
