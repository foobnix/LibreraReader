package com.text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

public class Fb2Extractor {
    public static final String OUT_FB2_XML = "fb2.html";

    public static byte[] getImageCover(String path) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(path)));
        Document parse = Jsoup.parse(in, null, "", Parser.xmlParser());
        Element select = parse.select("binary").first();
        if (select != null) {
            return Base64.getMimeDecoder().decode(select.text());
        } else {
            return EpubCoverExtractor.getEntryAsByte(Fb2Extractor.class.getResourceAsStream("sample.jpg"));
        }
    }

    public static byte[] getImageCoverZip(String path) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(path)));
        ZipInputStream zipInputStream = new ZipInputStream(in);
        ZipEntry nextEntry = null;
        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String name = nextEntry.getName().toLowerCase();
            if (name.endsWith(".fb2")) {
                Document parse = Jsoup.parse(zipInputStream, null, "", Parser.xmlParser());
                Element select = parse.select("binary").first();
                if (select != null) {
                    return Base64.getMimeDecoder().decode(select.text());
                } else {
                    return EpubCoverExtractor.getEntryAsByte(Fb2Extractor.class.getResourceAsStream("sample.jpg"));
                }
            }
        }

        return EpubCoverExtractor.getEntryAsByte(Fb2Extractor.class.getResourceAsStream("sample.jpg"));

    }

    public static String extract(String inputPath, String outputDir) throws IOException {
        // inputPath = new String(inputPath.getBytes("UTF-8"));
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(inputPath)));
        try {
            if (inputPath.endsWith(".zip")) {
                ZipInputStream zipInputStream = new ZipInputStream(in);
                ZipEntry nextEntry = null;
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName().toLowerCase();
                    if (name.endsWith(".fb2")) {
                        File file = new File(outputDir, "out.fb2.xml");
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        EpubCoverExtractor.writeToStream(zipInputStream, fileOutputStream);
                        fileOutputStream.close();
                        in = new BufferedInputStream(new FileInputStream(file));
                    }
                }
            }
        } catch (Exception e) {
        }
        in.mark(80);
        byte[] header = new byte[80];
        in.read(header);
        String str = new String(header);
        System.out.println(str);

        String encoding = "UTF-8";
        if (str.contains("windows-1251")) {
            encoding = "cp1251";
        }
        in.reset();

        System.out.println("Encoding: " + encoding);

        Document parse = Jsoup.parse(in, encoding, "", Parser.xmlParser());

        Elements select = parse.select("binary");
        for (int i = 0; i < select.size(); i++) {
            Element element = select.get(i);
            String contentType = element.attr("content-type");
            String id = element.attr("id");
            String body = element.text();

            // byte[] decode = Base64.decode(body, Base64.DEFAULT);
            byte[] decode = Base64.getMimeDecoder().decode(body);
            FileOutputStream fileOutputStream = new FileOutputStream(new File(outputDir, id));
            fileOutputStream.write(decode);
            fileOutputStream.close();
            System.out.println(id);

            element.remove();
        }

        Elements images = parse.select("image");
        for (int i = 0; i < select.size(); i++) {
            Element element = images.get(i);
            String attr = element.attr("l:href");
            attr = attr.replace("#", "");
            Element a = new Element(Tag.valueOf("img"), "");
            a.attr("src", attr);
            element.replaceWith(a);
        }
        // <section id="n_1">
        // <a l:href="#n_1" type="note">[1]</a>

        StringBuffer outNotes = new StringBuffer();
        outNotes.append("<notes>");

        Elements snoski = parse.select("a[type=note]");
        for (int i = 0; i < snoski.size(); i++) {
            Element item = snoski.get(i);
            String sID = item.text();
            String id = item.attr("l:href").replace("#", "");

            Elements note = parse.select(" section[id=" + id + "]");
            note.select("title").remove();
            String text = note.text();
            // System.out.println(sID + " " + id + " " + text);
            outNotes.append(String.format("<item id=\"%s\">%s</item>\n", sID, text));
        }
        outNotes.append("</notes>");
        // System.out.println(outNotes.toString());
        FileOutputStream nout = new FileOutputStream(new File(outputDir, "notes.xml"));
        nout.write(outNotes.toString().getBytes());
        nout.close();

        File file = new File(outputDir, OUT_FB2_XML);

        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), encoding));
        PrintWriter writer = new PrintWriter(file);
        String line;
        boolean bodyEnd = false;
        while ((line = input.readLine()) != null) {
            line = line.replace("FictionBook", "html").replace("fictionbook", "html");
            line = line.replace("<image l:href=\"#", "<img src=\"");
            if (line.contains("</body>")) {
                bodyEnd = true;
                writer.println(line);
            }
            if (!bodyEnd) {
                writer.println(line + " ");
            } else if (line.contains("</html>")) {
                writer.println(line);
            }

        }
        input.close();
        writer.close();

        return file.getPath();
    }

    public static Map<String, String> readSnoski() throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File("notes.xml")));
        Document parse = Jsoup.parse(in, null, "", Parser.xmlParser());

        Elements snoski = parse.select("item");
        for (int i = 0; i < snoski.size(); i++) {
            Element item = snoski.get(i);
            map.put(item.attr("id"), item.text());
        }

        return map;

    }

    public static void main(String[] args) throws IOException {
        extract("/home/ivan-dev/Downloads/Books/кузя/Aleksandrova_Domovyonok-Kuzka.297647.fb2", "/home/ivan-dev/Downloads/Books/кузя/out");
    }

    public static void main1(String[] args) {
        String input = "helllo<binary id=\"i_001.jpg\"  content-type=\"image/jpeg\">/9j/4QDSRXhpZgAASUkqAAgAAAAFABIBAwAB\r\n\tAAAAAQAAADEBAgA+AAAASgAAADIBAgAUAAAAiAAAABMCAwABAAAAAQAAAGmHBAABAAAAnAAAAAAAAADP8O7j8ODs7OAg9uj08O7i7ukg7uHw4OHu8uroIOjn7uHw4Obl7ejpIOru7O/g7ejoIEFDRCBTeXN0ZW1zADIwMTI6MDk6MjggMTk6<binary>end";
        input = input.replaceAll("(?s)<binary(.*)<binary>", "+");

        System.out.println(input);
    }
}
