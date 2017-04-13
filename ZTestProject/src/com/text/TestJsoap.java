package com.text;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class TestJsoap {

    public static void main(String[] args) throws IOException {
        Document parse = Jsoup.parse(TestJsoap.class.getResourceAsStream("jsoap.txt"), null, "", Parser.xmlParser());
        Elements select = parse.select("[id]");
        System.out.println(select.html());
        System.out.println("-------------");
        Element first = select.first();
        System.out.println(first.parent());

    }
}
