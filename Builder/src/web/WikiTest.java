package web;

import java.io.IOException;

import org.json.JSONException;

public class WikiTest {

    public static void main(String[] args) throws JSONException, IOException {
        String in = "# Hello **What is your name?** My name is Igor";
        in = "* What is your name?";
        in = "# 7.11";
        in = "My life with * [How to convert PDF to EPUB](/wiki/faq/convert-pdf-to-epub/) and how to open a book";
        in = "[What is new](/wiki/what-is-new)";
        in = "**Fixes**";
        in = "**Speed reading RSVP (Rapid Serial Visual Presentation)**";
        in = "**Status bar position (Top or Bottom) in Book mode**";
        in = "[What is new](/wiki/what-is-new)";

        String ln = "ru";

        System.out.println(in);
        String out = WikiTranslate.traslateMD(in, ln);
        System.out.println(out);

    }
}
