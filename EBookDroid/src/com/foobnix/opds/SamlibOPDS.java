package com.foobnix.opds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.wrapper.AppState;

import android.support.v4.util.Pair;

public class SamlibOPDS {

    public static final String LIBRERA_MOBI = "?from=librera.mobi";
    private static final String AUTHORS = "?authors";
    private static final String AUTHORS_LIST = "?AUTHORS_LIST";
    private static final String AUTHORS_BOOKS = "?AUTHORS_BOOKS";
    private static final String FORM = "?form";
    private static final String GENRE = "?genre";
    private static final String BOOK = "?BOOK";
    private static final String ROOT = "http://samlib.ru";
    public static final String ROOT_AWARDS = "My:Awards";
    public static final String ROOT_FAVORITES = "My:Favorites";

    public static boolean isSamlibUrl(String url) {
        return url.startsWith(ROOT) || url.startsWith(ROOT_AWARDS) || url.startsWith(ROOT_FAVORITES);
    }

    private static List<Entry> getHomeAwards() {
        List<Entry> list = new ArrayList<Entry>();
        list.add(Entry.Home("https://www.worldswithoutend.com/books.asp", "Book Awards"));
        list.add(Entry.Home("https://www.worldswithoutend.com/lists.asp", "Book Lists"));
        list.add(Entry.Home("http://fantlab.ru/awards", "Награды и премии"));
        list.add(Entry.Home("https://www.goodreads.com/shelf/show/top-rated", "Popular Top Rated Books"));
        list.add(Entry.Home("https://www.amazon.com/b?node=8192263011", "100 Books to Read in a Lifetime from Amazon"));
        list.add(Entry.Home("https://issuu.com/", "All World Magazines "));
        return list;
    }

    private static List<Entry> getHome() {
        List<Entry> list = new ArrayList<Entry>();
        list.add(new Entry("http://samlib.ru/" + LIBRERA_MOBI, "Переход на сайт", "http://samlib.ru", null));
        list.add(new Entry("http://samlib.ru/rating/expert/", "Избранное", "Рейтинг экспертов", null));
        list.add(new Entry("http://samlib.ru/rating/top40/", "Рейтинг Топ-40", "Рейтинг по оценкам", null));
        list.add(new Entry("http://samlib.ru/rating/top100/", "Рейтинг Топ-100", "Рейтинг по оценкам", null));
        list.add(new Entry("http://samlib.ru/r/redaktor/rating1.shtml", "Топ-30 Редактора", "Список лучших работ", null));
        list.add(new Entry("http://samlib.ru/short.shtml", "Последние поступления", "7 дней", null));
        list.add(new Entry("http://samlib.ru/" + GENRE, "Жанры", "Все жанры", null));
        list.add(new Entry("http://samlib.ru/" + FORM, "Формы", "Все формы", null));
        list.add(new Entry("http://samlib.ru/" + AUTHORS, "Авторы", "Все авторы", null));
        return list;
    }

    public static String getTitle(String url) {
        if (url.startsWith(ROOT_AWARDS)) {
            return ROOT_AWARDS;
        }
        for (Entry e : getHome()) {
            if (url.equals(e.homeUrl)) {
                return e.title;
            }
        }
        for (Entry e : getForms()) {
            if (url.equals(e.homeUrl)) {
                return e.title;
            }
        }
        for (Entry e : getGenres()) {
            if (url.equals(e.homeUrl)) {
                return e.title;
            }
        }
        return "Samlib.ru";
    }

    private static List<Entry> getGenres() {
        List<Entry> list = new ArrayList<Entry>();
        list.add(new Entry(ROOT + "/janr/index_janr_5-1.shtml", "Проза"));
        list.add(new Entry(ROOT + "/janr/index_janr_4-1.shtml", "Поэзия"));
        list.add(new Entry(ROOT + "/janr/index_janr_3-1.shtml", "Лирика"));
        list.add(new Entry(ROOT + "/janr/index_janr_19-1.shtml", "Мемуары"));
        list.add(new Entry(ROOT + "/janr/index_janr_26-1.shtml", "История"));
        list.add(new Entry(ROOT + "/janr/index_janr_29-1.shtml", "Детская"));
        list.add(new Entry(ROOT + "/janr/index_janr_2-1.shtml", "Детектив"));
        list.add(new Entry(ROOT + "/janr/index_janr_25-1.shtml", "Приключения"));
        list.add(new Entry(ROOT + "/janr/index_janr_1-1.shtml", "Фантастика"));
        list.add(new Entry(ROOT + "/janr/index_janr_24-1.shtml", "Фэнтези"));
        list.add(new Entry(ROOT + "/janr/index_janr_22-1.shtml", "Киберпанк"));
        list.add(new Entry(ROOT + "/janr/index_janr_35-1.shtml", "Фанфик"));
        list.add(new Entry(ROOT + "/janr/index_janr_11-1.shtml", "Публицистика"));
        list.add(new Entry(ROOT + "/janr/index_janr_32-1.shtml", "События"));
        list.add(new Entry(ROOT + "/janr/index_janr_23-1.shtml", "Литобзор"));
        list.add(new Entry(ROOT + "/janr/index_janr_9-1.shtml", "Критика"));
        list.add(new Entry(ROOT + "/janr/index_janr_15-1.shtml", "Философия"));
        list.add(new Entry(ROOT + "/janr/index_janr_13-1.shtml", "Религия"));
        list.add(new Entry(ROOT + "/janr/index_janr_14-1.shtml", "Эзотерика"));
        list.add(new Entry(ROOT + "/janr/index_janr_18-1.shtml", "Оккультизм"));
        list.add(new Entry(ROOT + "/janr/index_janr_17-1.shtml", "Мистика"));
        list.add(new Entry(ROOT + "/janr/index_janr_30-1.shtml", "Хоррор"));
        list.add(new Entry(ROOT + "/janr/index_janr_28-1.shtml", "Политика"));
        list.add(new Entry(ROOT + "/janr/index_janr_12-1.shtml", "Любовный роман"));
        list.add(new Entry(ROOT + "/janr/index_janr_20-1.shtml", "Естествознание"));
        list.add(new Entry(ROOT + "/janr/index_janr_21-1.shtml", "Изобретательство"));
        list.add(new Entry(ROOT + "/janr/index_janr_8-1.shtml", "Юмор"));
        list.add(new Entry(ROOT + "/janr/index_janr_27-1.shtml", "Байки"));
        list.add(new Entry(ROOT + "/janr/index_janr_31-1.shtml", "Пародии"));
        list.add(new Entry(ROOT + "/janr/index_janr_10-1.shtml", "Переводы"));
        list.add(new Entry(ROOT + "/janr/index_janr_16-1.shtml", "Сказки"));
        list.add(new Entry(ROOT + "/janr/index_janr_6-1.shtml", "Драматургия"));
        list.add(new Entry(ROOT + "/janr/index_janr_33-1.shtml", "Постмодернизм"));
        list.add(new Entry(ROOT + "/janr/index_janr_34-1.shtml", "Foreign+Translat"));
        return list;
    }

    private static List<Entry> getForms() {
        List<Entry> list = new ArrayList<Entry>();
        list.add(new Entry(ROOT + "/type/index_type_1-1.shtml", "Роман"));
        list.add(new Entry(ROOT + "/type/index_type_2-1.shtml", "Повесть"));
        list.add(new Entry(ROOT + "/type/index_type_16-1.shtml", "Глава"));
        list.add(new Entry(ROOT + "/type/index_type_10-1.shtml", "Сборник рассказов"));
        list.add(new Entry(ROOT + "/type/index_type_3-1.shtml", "Рассказ"));
        list.add(new Entry(ROOT + "/type/index_type_11-1.shtml", "Поэма"));
        list.add(new Entry(ROOT + "/type/index_type_7-1.shtml", "Сборник стихов"));
        list.add(new Entry(ROOT + "/type/index_type_6-1.shtml", "Стихотворение"));
        list.add(new Entry(ROOT + "/type/index_type_13-1.shtml", "Эссе"));
        list.add(new Entry(ROOT + "/type/index_type_4-1.shtml", "Очерк"));
        list.add(new Entry(ROOT + "/type/index_type_5-1.shtml", "Статья"));
        list.add(new Entry(ROOT + "/type/index_type_17-1.shtml", "Монография"));
        list.add(new Entry(ROOT + "/type/index_type_18-1.shtml", "Справочник"));
        list.add(new Entry(ROOT + "/type/index_type_12-1.shtml", "Песня"));
        list.add(new Entry(ROOT + "/type/index_type_15-1.shtml", "Новелла"));
        list.add(new Entry(ROOT + "/type/index_type_9-1.shtml", "Пьеса; сценарий"));
        list.add(new Entry(ROOT + "/type/index_type_8-1.shtml", "Миниатюра"));
        list.add(new Entry(ROOT + "/type/index_type_14-1.shtml", "Интервью"));
        return list;
    }

    private static List<Entry> getAuthors() {
        List<Entry> list = new ArrayList<Entry>();
        list.add(new Entry(ROOT + "/a/" + AUTHORS_LIST, "А"));
        list.add(new Entry(ROOT + "/b/" + AUTHORS_LIST, "Б"));
        list.add(new Entry(ROOT + "/w/" + AUTHORS_LIST, "В"));
        list.add(new Entry(ROOT + "/g/" + AUTHORS_LIST, "Г"));
        list.add(new Entry(ROOT + "/d/" + AUTHORS_LIST, "Д"));
        list.add(new Entry(ROOT + "/e/" + AUTHORS_LIST, "Е"));
        list.add(new Entry(ROOT + "/e/index_yo.shtml" + AUTHORS_LIST, "Ё"));
        list.add(new Entry(ROOT + "/z/index_zh.shtml" + AUTHORS_LIST, "Ж"));
        list.add(new Entry(ROOT + "/z/" + AUTHORS_LIST, "З"));
        list.add(new Entry(ROOT + "/i/" + AUTHORS_LIST, "И"));
        list.add(new Entry(ROOT + "/j/index_ij.shtml" + AUTHORS_LIST, "Й"));
        list.add(new Entry(ROOT + "/k/" + AUTHORS_LIST, "К"));
        list.add(new Entry(ROOT + "/l/" + AUTHORS_LIST, "Л"));
        list.add(new Entry(ROOT + "/m/" + AUTHORS_LIST, "М"));
        list.add(new Entry(ROOT + "/n/" + AUTHORS_LIST, "Н"));
        list.add(new Entry(ROOT + "/o/" + AUTHORS_LIST, "О"));
        list.add(new Entry(ROOT + "/p/" + AUTHORS_LIST, "П"));
        list.add(new Entry(ROOT + "/r/" + AUTHORS_LIST, "Р"));
        list.add(new Entry(ROOT + "/s/" + AUTHORS_LIST, "С"));
        list.add(new Entry(ROOT + "/t/" + AUTHORS_LIST, "Т"));
        list.add(new Entry(ROOT + "/u/" + AUTHORS_LIST, "У"));
        list.add(new Entry(ROOT + "/f/" + AUTHORS_LIST, "Ф"));
        list.add(new Entry(ROOT + "/h/" + AUTHORS_LIST, "Х"));
        list.add(new Entry(ROOT + "/c/" + AUTHORS_LIST, "Ц"));
        list.add(new Entry(ROOT + "/c/index_ch.shtml" + AUTHORS_LIST, "Ч"));
        list.add(new Entry(ROOT + "/s/index_sh.shtml" + AUTHORS_LIST, "Ш"));
        list.add(new Entry(ROOT + "/s/index_sw.shtml" + AUTHORS_LIST, "Щ"));
        list.add(new Entry(ROOT + "/x/" + AUTHORS_LIST, "Ъ"));
        list.add(new Entry(ROOT + "/y/" + AUTHORS_LIST, "Ы"));
        list.add(new Entry(ROOT + "/x/" + AUTHORS_LIST, "Ь"));
        list.add(new Entry(ROOT + "/e/index_ae.shtml" + AUTHORS_LIST, "Э"));
        list.add(new Entry(ROOT + "/j/index_ju.shtml" + AUTHORS_LIST, "Ю"));
        list.add(new Entry(ROOT + "/j/index_ja.shtml" + AUTHORS_LIST, "Я"));
        list.add(new Entry(ROOT + "/0/index_0.shtml+AUTHORS_LIST", "0"));
        list.add(new Entry(ROOT + "/1/index_1.shtml" + AUTHORS_LIST, "1"));
        list.add(new Entry(ROOT + "//index_.shtml" + AUTHORS_LIST, ""));
        list.add(new Entry(ROOT + "/1/index_1.shtml" + AUTHORS_LIST, "1"));
        list.add(new Entry(ROOT + "/2/index_2.shtml" + AUTHORS_LIST, "2"));
        list.add(new Entry(ROOT + "/3/index_3.shtml" + AUTHORS_LIST, "3"));
        list.add(new Entry(ROOT + "/4/index_4.shtml" + AUTHORS_LIST, "4"));
        list.add(new Entry(ROOT + "/5/index_5.shtml" + AUTHORS_LIST, "5"));
        list.add(new Entry(ROOT + "/6/index_6.shtml" + AUTHORS_LIST, "6"));
        list.add(new Entry(ROOT + "/7/index_7.shtml" + AUTHORS_LIST, "7"));
        list.add(new Entry(ROOT + "/8/index_8.shtml" + AUTHORS_LIST, "8"));
        list.add(new Entry(ROOT + "/9/index_9.shtml" + AUTHORS_LIST, "9"));
        list.add(new Entry(ROOT + "/a/index_a.shtml" + AUTHORS_LIST, "A"));
        list.add(new Entry(ROOT + "/b/index_b.shtml" + AUTHORS_LIST, "B"));
        list.add(new Entry(ROOT + "/c/index_c.shtml" + AUTHORS_LIST, "C"));
        list.add(new Entry(ROOT + "/d/index_d.shtml" + AUTHORS_LIST, "D"));
        list.add(new Entry(ROOT + "/e/index_e.shtml" + AUTHORS_LIST, "E"));
        list.add(new Entry(ROOT + "/f/index_f.shtml" + AUTHORS_LIST, "F"));
        list.add(new Entry(ROOT + "/g/index_g.shtml" + AUTHORS_LIST, "G"));
        list.add(new Entry(ROOT + "/h/index_h.shtml" + AUTHORS_LIST, "H"));
        list.add(new Entry(ROOT + "/i/index_i.shtml" + AUTHORS_LIST, "I"));
        list.add(new Entry(ROOT + "/j/index_j.shtml" + AUTHORS_LIST, "J"));
        list.add(new Entry(ROOT + "/k/index_k.shtml" + AUTHORS_LIST, "K"));
        list.add(new Entry(ROOT + "/l/index_l.shtml" + AUTHORS_LIST, "L"));
        list.add(new Entry(ROOT + "/m/index_m.shtml" + AUTHORS_LIST, "M"));
        list.add(new Entry(ROOT + "/n/index_n.shtml" + AUTHORS_LIST, "N"));
        list.add(new Entry(ROOT + "/o/index_o.shtml" + AUTHORS_LIST, "O"));
        list.add(new Entry(ROOT + "/p/index_p.shtml" + AUTHORS_LIST, "P"));
        list.add(new Entry(ROOT + "/q/index_q.shtml" + AUTHORS_LIST, "Q"));
        list.add(new Entry(ROOT + "/r/index_r.shtml" + AUTHORS_LIST, "R"));
        list.add(new Entry(ROOT + "/s/index_s.shtml" + AUTHORS_LIST, "S"));
        list.add(new Entry(ROOT + "/t/index_t.shtml" + AUTHORS_LIST, "T"));
        list.add(new Entry(ROOT + "/u/index_u.shtml" + AUTHORS_LIST, "U"));
        list.add(new Entry(ROOT + "/v/index_v.shtml" + AUTHORS_LIST, "V"));
        list.add(new Entry(ROOT + "/w/index_w.shtml" + AUTHORS_LIST, "W"));
        list.add(new Entry(ROOT + "/x/index_x.shtml" + AUTHORS_LIST, "X"));
        list.add(new Entry(ROOT + "/y/index_y.shtml" + AUTHORS_LIST, "Y"));
        list.add(new Entry(ROOT + "/z/index_z.shtml" + AUTHORS_LIST, "Z"));
        return list;
    }

    public static Pair<List<Entry>, String> getSamlibResult(String url) {
        if (ROOT.equals(url)) {
            return Pair.create(getHome(), getTitle(url));
        }
        if (ROOT_AWARDS.equals(url)) {
            return Pair.create(getHomeAwards(), getTitle(url));
        }
        if (ROOT_FAVORITES.equals(url)) {
            String[] list = AppState.get().myOPDSLinks.split(";");
            List<Entry> res = new ArrayList<Entry>();
            for (String line : list) {
                if (TxtUtils.isEmpty(line)) {
                    continue;
                }
                if (line.contains("star_1.png")) {
                    String[] it = line.split(",");
                    res.add(new Entry(it[0], it[1], it[2], it[3], true));
                }
            }
            return Pair.create(res, ROOT_FAVORITES);

        }

        if (url.endsWith(GENRE)) {
            return Pair.create(getGenres(), getTitle(url));
        }
        if (url.endsWith(FORM)) {
            return Pair.create(getForms(), getTitle(url));
        }
        if (url.endsWith(AUTHORS)) {
            return Pair.create(getAuthors(), getTitle(url));
        }

        if ("http://samlib.ru/short.shtml".equals(url)) {
            return Pair.create(parseShort(url), getTitle(url));
        }
        if ("http://samlib.ru/long.shtml".equals(url)) {
            return Pair.create(parseShort(url), getTitle(url));
        }
        if ("http://samlib.ru/rating/top40/".equals(url)) {
            return Pair.create(parseShort(url), getTitle(url));
        }
        if ("http://samlib.ru/rating/top100/".equals(url)) {
            return Pair.create(parseShort(url), getTitle(url));
        }
        if ("http://samlib.ru/rating/expert/".equals(url)) {
            List<Entry> parseRating = parseRating(url, 0);
            parseRating.add(new Entry("http://samlib.ru/rating/expert/index-2.shtml", "Страница 2"));
            return Pair.create(parseRating, getTitle(url));
        }
        if ("http://samlib.ru/rating/expert/index-2.shtml".equals(url)) {
            return Pair.create(parseRating(url, 0), getTitle(url));
        }

        if ("http://samlib.ru/r/redaktor/rating1.shtml".equals(url)) {
            return Pair.create(parseRating(url, 0), getTitle(url));
        }

        if (url.contains("/janr/") || url.contains("/type/")) {
            return Pair.create(parseRating(url, -1), getTitle(url));
        }

        if (url.endsWith(BOOK)) {
            return parseBook(url);
        }
        if (url.endsWith(AUTHORS_LIST)) {
            return Pair.create(parseAuthors(url), getTitle(url));
        }

        if (url.endsWith(AUTHORS_BOOKS)) {
            return parseAuthorBooks(url);
        }

        return null;

    }

    private static Pair<List<Entry>, String> parseAuthorBooks(String url) {
        List<Entry> list = new ArrayList<Entry>();
        String res = getHTTP(url);
        Document doc = Jsoup.parse(res, "koi8");
        Elements items = doc.select("li");

        String authorName = doc.select("h3").first().ownText().replace(":", "");
        Element author = new Element(Tag.valueOf("a"), "");
        author.text(authorName);
        author.attr("href", url);

        for (int i = 0; i < items.size(); i++) {
            Element root = items.get(i);
            if (root.select("b").first() != null && root.select("small").first() != null) {
                Element book = root.select("a").first();

                String bookLink = book.attr("href");

                String replace = url.replace(AUTHORS_BOOKS, "");
                if (replace.endsWith(".shtml")) {
                    replace = replace.substring(0, replace.lastIndexOf("/") + 1);
                }
                book.attr("href", replace + bookLink);

                String size = root.select("a + b").first().ownText();
                Elements select = root.select("dd");
                String annotation = "";
                if (select != null) {
                    annotation = select.text();
                }
                String genre = root.select("small").first().ownText().replace("\"", "");

                Entry makeEntry = makeEntry(book, author);
                makeEntry.content += "<b>Размер:</b> " + size + "<br/>";
                makeEntry.content += "<b>Жанр: </b>" + genre + "<br/>";
                makeEntry.content += annotation;

                list.add(makeEntry);
            }
        }
        return Pair.create(list, authorName);

    }

    private static List<Entry> parseRating(String url, int dx) {
        List<Entry> list = new ArrayList<Entry>();
        String res = getHTTP(url);
        Document doc = Jsoup.parse(res, "koi8");
        Elements items = doc.select("li");
        for (int i = 0; i < items.size(); i++) {
            Elements select = items.get(i).select("a");
            if (select.size() < 2) {
                continue;
            }

            Element author = select.get(1 + dx);
            Element title = select.get(2 + dx);
            Entry makeEntry = makeEntry(title, author);
            makeEntry.content = items.get(i).select("dd").text();
            list.add(makeEntry);
        }
        return list;

    }

    public static List<Entry> parseAuthors(String url) {
        List<Entry> list = new ArrayList<Entry>();

        String res = getHTTP(url);
        Document doc = Jsoup.parse(res, "koi8");
        Elements items = doc.select("dl");
        for (int i = 0; i < items.size(); i++) {
            Element root = items.get(i);
            Elements author = root.select("a");

            Link link = new Link(ROOT + author.attr("href") + AUTHORS_BOOKS, Link.APPLICATION_ATOM_XML);

            Entry entry = new Entry(author.text(), link);
            entry.content = root.text();
            list.add(entry);
        }

        return list;

    }

    public static Pair<List<Entry>, String> parseBook(String url) {
        List<Entry> list = new ArrayList<Entry>();

        String res = getHTTP(url);
        Document doc = Jsoup.parse(res, "koi8");
        Elements items = doc.select("h2");

        String title = items.first().text().trim();

        Element authorElement = doc.select("li:contains(Copyright)").first();

        Link link1 = new Link(url + LIBRERA_MOBI, Link.WEB_LINK);
        Link link2 = new Link(url, Link.APPLICATION_ATOM_XML_PROFILE, filterAuthror(authorElement.text()));

        Entry entry = new Entry(title, link1, link2);

        String date = doc.select("li:contains(Размещен)").first().text().replace("© Copyright", "").replace("Статистика.", "").trim();

        entry.content = date + "<br/>";

        Element annotFirst = doc.select("li:contains(Аннотация)").first();
        if (annotFirst != null) {
            String annotation = annotFirst.text().replace("© Copyright", "").replace("Статистика.", "").trim();
            entry.content += annotation;
        }

        Link link = new Link(url.replace(".shtml", ".fb2.zip"), "application/fb-ebook+zip", "Скачать FB2");
        String authorTxt = filterAuthror(authorElement.text());
        if (authorTxt.contains("(")) {
            authorTxt = authorTxt.substring(0, authorTxt.indexOf("("));
        }

        link.parentTitle = authorTxt.trim() + " - " + title.trim();

        File book = new File(AppState.get().downlodsPath, link.getDownloadName());
        if (book.isFile()) {
            link.filePath = book.getPath();
        }

        entry.links.add(link);
        entry.links.add(new Link(url, Link.WEB_LINK, "WEB"));

        list.add(entry);

        return Pair.create(list, title);

    }

    public static List<Entry> parseShort(String url) {
        String res = getHTTP(url);
        Document doc = Jsoup.parse(res, "koi8");
        Elements items = doc.select("tr");

        List<Entry> list = new ArrayList<Entry>();

        for (int i = 0; i < items.size(); i++) {
            Element item = items.get(i);
            Elements a = item.select("td > a");

            if (a.size() < 2) {
                continue;
            }
            Element title = a.get(0);
            Element author = a.get(1);
            if (title == null || author == null) {
                continue;
            }
            if (!title.attr("href").endsWith(".shtml")) {
                continue;
            }

            list.add(makeEntry(title, author));
        }

        return list;

    }

    public static String filterAuthror(String txt) {
        String text = txt.replace("\"", "").replace("© Copyright", "").trim();
        return text;
    }

    public static Link aLink(Element e) {
        String text = filterAuthror(e.text());
        String url = e.attr("href").replace("\"", "");
        text = text + " [ " + url + " ] ";

        return new Link(ROOT + url, Link.APPLICATION_ATOM_XML_SUBLINE, text);
    }

    public static Entry makeEntry(Element bookItem, Element author) {
        String aURL = author.attr("href") + AUTHORS_BOOKS;

        String tURL = bookItem.attr("href");
        tURL = tURL.startsWith("http") ? tURL : ROOT + tURL;

        String authorTxt = author.text().replace("\"", "");
        String titleTxt = bookItem.text().replace("\"", "");

        Link link1 = new Link(tURL + BOOK);

        Link link2 = new Link(aURL.startsWith("http") ? aURL : ROOT + aURL, Link.APPLICATION_ATOM_XML_SUBLINE, authorTxt);

        Link download = new Link(tURL.replace(".shtml", ".fb2.zip"), "application/fb-ebook+zip", "Скачать FB2");
        download.parentTitle = filterAuthror(authorTxt.trim() + " - " + titleTxt.trim());

        Link web = new Link(tURL + LIBRERA_MOBI, Link.WEB_LINK, "WEB");

        File book = new File(AppState.get().downlodsPath, download.getDownloadName());
        if (book.isFile()) {
            download.filePath = book.getPath();
        }

        return new Entry(titleTxt, link1, link2, download, web);
    }

    private static String getHTTP(String url) {
        try {
            return OPDS.getHttpResponse(url);
        } catch (Exception e) {
            return null;
        }
    }

}
