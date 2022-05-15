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

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OPDS {
    public static final String CODE_401 = "401";
    static Cache cache = new Cache(CacheZipUtils.CACHE_WEB, 5 * 1024 * 1024);

    public static OkHttpClient.Builder builder = new OkHttpClient.Builder()//
            .cookieJar(new WebviewCookieHandler())//
            .connectTimeout(20, TimeUnit.SECONDS)//
            .writeTimeout(20, TimeUnit.SECONDS)//
            .readTimeout(30, TimeUnit.SECONDS)//
            .cache(cache);//

    public static OkHttpClient client = builder.build();//

    final static Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<String, CachingAuthenticator>();

    public static int random = new Random().nextInt();
    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.2997." + random + " Safari/537.36";

    public static void buildProxy() {
        new Thread(() -> {
            if (AppState.get().proxyEnable && TxtUtils.isNotEmpty(AppState.get().proxyServer) && AppState.get().proxyPort != 0) {
                Type http = AppState.PROXY_SOCKS.equals(AppState.get().proxyType) ? Type.SOCKS : Type.HTTP;
                LOG.d("Proxy: Server", http.name(), AppState.get().proxyServer, AppState.get().proxyPort);
                builder.proxy(new Proxy(http, new InetSocketAddress(AppState.get().proxyServer, AppState.get().proxyPort)));
                if (TxtUtils.isNotEmpty(AppState.get().proxyUser)) {
                    LOG.d("Proxy: User", AppState.get().proxyUser, AppState.get().proxyPassword);
                    builder.proxyAuthenticator(new BasicAuthenticator(new Credentials(AppState.get().proxyUser, AppState.get().proxyPassword)));
                }
            } else {
                builder.proxy(null);
            }
            client = builder.build();
        },"@T buildProxy").start();
    }

    public static String getHttpResponseNoException(String url) {
        try {
            return getHttpResponse(url, "", "");
        } catch (Exception e) {
            return "";
        }

    }

    public static String getHttpResponse(String url, String login, String password) throws IOException {
        LOG.d("getHttpResponse", url, login, password);

        Request request = new Request.Builder()//
                .header("User-Agent", USER_AGENT)

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
        } else {

            {
                Credentials credentials = new Credentials(login, password);
                final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);
                final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);

                DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()//
                        .with("digest", digestAuthenticator)//
                        .with("basic", basicAuthenticator)//
                        .build();


                client = builder //
                        .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache)) //
                        .addInterceptor(new AuthenticationCacheInterceptor(authCache)) //
                        .build();

                LOG.d("getHttpResponse Credentials", login, password);

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
                            .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache)) //
                            .addInterceptor(new AuthenticationCacheInterceptor(authCache)) //
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
        string = string.replace("\u0001M\u0001J","");
        response.close();
        return string;
    }

    public static ResponseBody getHttpResponseBody(String url) throws IOException {

        Request request = new Request.Builder()//
                .header("User-Agent", USER_AGENT)

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
                    .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache)) //
                    .addInterceptor(new AuthenticationCacheInterceptor(authCache)) //
                    .build();

            response.close();
            response = client.newCall(request).execute();

            if (response.code() == 401) {
                return response.body();
            }
        }

        return response.body();
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
