package com.text;

import java.io.UnsupportedEncodingException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public class EncodingCp1251 {

    public static void main(String[] args) throws UnsupportedEncodingException {

        String in = "Ðå÷íîé âîêçàë, â ïðîøëîì íåáîëüøàÿ ñòàíöèÿ ìåòðîïîëèòåíà íà ñåâåðå ñòîëèöû, íåñìîòðÿ íà óäàëåííîñòü îò öåíòðà, íàçåìíàÿ ÷àñòü êîòîðîé ðàíüøå áûëà îæèâëåííûì ìåñòîì, ïðèòÿãèâàë ïîñåòèòåëåé, æåëàþùèõ êóïèòü êàêèå-íèáóäü íåíóæíûå âåùè çà áîëüøóþ öåíó. Êàæäûé äåíü ìíîæåñòâî ëþäåé ïðèåçæàëè è óåçæàëè ñî ñòàíöèè.";
        // in = "Речной вокзал, в прошлом небольшая станция метрополитена на
        // севере столицы, несмотря на удаленность от центра, наземная часть
        // которой раньше была оживленным местом, притягивал посетителей,
        // желающих купить какие-нибудь ненужные вещи за большую цену. Каждый
        // день множество людей приезжали и уезжали со станции.";
        // byte[] bytes = in.getBytes(StandardCharsets.UTF_8);
        // System.out.println(new String(new
        // String(in.getBytes("utf-8")).getBytes("cp1251")));
        // System.out.println(in);

        String html = "<?xml version=\"1.0\" encoding=\"UTF-8\"><tests><test><id>xxx</id><status>xxx</status></test><test><id>xxx</id><status>xxx</status></test></tests></xml>";
        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
        // System.out.println(doc.);
                
    }
}
