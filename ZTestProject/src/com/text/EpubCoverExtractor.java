package com.text;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EpubCoverExtractor {

    private static final int BUFFER_SIZE = 4096;

    public static byte[] getImageCover(String path) throws IOException {
        System.out.println("Start " + path);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(path)));
        in.mark(Integer.MAX_VALUE);
        ZipInputStream zipInputStream = new ZipInputStream(in);

        ZipEntry nextEntry = null;
        String imageName = null;
        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String name = nextEntry.getName().toLowerCase();
            if (name.endsWith(".opf")) {

                System.out.println(name);

                Document parse = Jsoup.parse(new ByteArrayInputStream(getEntryAsByte(zipInputStream)), null, "");

                Elements select = parse.select("dc|title");
                System.out.println("== Title ==" + select.text());

                select = parse.select("meta[name=cover]");
                Element first = select.first();
                if (first != null) {
                    imageName = first.attr("content");
                }
                if (imageName == null) {
                    select = parse.select("item[id=cover-image]");
                    System.out.println(select.toString());
                    for (int i = 0; i < select.size(); i++) {
                        Element element = select.get(i);
                        String mediaType = element.attr("media-type");
                        imageName = element.attr("href");
                        System.out.println("item[id=cover-image] " + imageName);
                    }
                }
                if (imageName == null) {
                    select = parse.select("item[id=cover.jpg]");
                    System.out.println(select.toString());
                    for (int i = 0; i < select.size(); i++) {
                        Element element = select.get(i);
                        String mediaType = element.attr("media-type");
                        imageName = element.attr("href").toLowerCase();
                        System.out.println("item[id=cover.jpg] " + imageName);
                    }
                }
            }
        }

        if (imageName != null) {
            in.reset();
            zipInputStream = new ZipInputStream(in);
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String name = nextEntry.getName();
                if (name.contains(imageName)) {
                    return getEntryAsByte(zipInputStream);
                }
            }

        }

        in.reset();
        zipInputStream = new ZipInputStream(in);
        while ((nextEntry = zipInputStream.getNextEntry()) != null)

        {
            String name = nextEntry.getName().toLowerCase();
            if (name.contains("cover") && (name.endsWith(".jpg") || name.endsWith(".png"))) {
                return getEntryAsByte(zipInputStream);
            }
        }

        in.reset();
        zipInputStream = new ZipInputStream(in);
        while ((nextEntry = zipInputStream.getNextEntry()) != null)

        {
            String name = nextEntry.getName().toLowerCase();
            if (name.contains("cvi") && (name.endsWith(".jpg") || name.endsWith(".png"))) {
                return getEntryAsByte(zipInputStream);
            }
        }

        in.reset();
        zipInputStream = new ZipInputStream(in);
        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String name = nextEntry.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".png")) {
                return getEntryAsByte(zipInputStream);
            }
        }

        in.close();

        return

        getEntryAsByte(EpubCoverExtractor.class.getResourceAsStream("sample.jpg"));
    }

    public static byte[] getEntryAsByte(InputStream zipInputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
        out.close();
        return out.toByteArray();
    }

    public static void writeToStream(InputStream zipInputStream, OutputStream out) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
        out.close();
    }
}
