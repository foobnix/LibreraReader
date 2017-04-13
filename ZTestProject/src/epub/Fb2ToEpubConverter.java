package epub;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class Fb2ToEpubConverter {

    public static String container_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
            "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" + //
            "  <rootfiles>\n" + //
            "    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" + //
            "  </rootfiles>\n" + //
            "</container>";//

    public static String content_opf = "<?xml version=\"1.0\"?>\n" + //
            "<package version=\"2.0\" unique-identifier=\"uid\" xmlns=\"http://www.idpf.org/2007/opf\">\n" + //
            " <metadata xmlns:opf=\"http://www.idpf.org/2007/opf\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" + //
            "  <dc:title>title</dc:title>\n" + //
            "<meta name=\"cover\" content=\"cover.jpg\" />\n" + //
            " </metadata>\n" + //
            "\n" + "<manifest>\n" + //
            "  <item id=\"idBookFb2\" href=\"fb2.fb2\" media-type=\"application/xhtml+xml\"/>\n" + //
            "  <item id=\"idResourceFb2\" href=\"fb2.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n" + //
            " </manifest>\n" + //
            " \n" + //
            "<spine toc=\"idResourceFb2\">\n" + //
            "  <itemref idref=\"idBookFb2\"/>\n" + //
            "</spine>\n" + //
            "</package>";//

    public static String NCX = "<?xml version=\"1.0\"?>\n" + //
            "<ncx version=\"2005-1\" xml:lang=\"en\" xmlns=\"http://www.daisy.org/z3986/2005/ncx/\">\n" + //
            " <head>\n" + //
            " </head>\n" + //
            " <docTitle>\n" + //
            "  <text>title</text>\n" + //
            " </docTitle>\n" + //
            " <navMap>\n" + //
            "  \n" + //
            "%nav% \n" + //
            "   \n" + //
            " </navMap>\n" + //
            "</ncx>";//

    public static String mimetype = "application/epub+zip";

    public static void main(String[] args) throws Exception {
        String inputFile = "/mount/extHDD/help/Dropbox/Projects/BookTestingDB/FB2/Strugackiy_Tom-3-1961-1963.ZWHT1Q.328850.fb2";
        String outputDir = "/home/ivan-dev/dev/workspace/pdf4/ZTestProject/output";
        fb2ToEpub(inputFile, outputDir);
        System.out.println("sample.epub");

        getMetaInfo(inputFile);
    }

    public static void getMetaInfo(String inputFile) throws Exception {
        String encoding = getFB2Encoding(inputFile);

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        // factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        xpp.setInput(new FileInputStream(inputFile), encoding);
        int eventType = xpp.getEventType();

        String bookTitle = null;

        String firstName = "";
        String middleName = "";
        String lastName = "";

        boolean isLink = false;
        String sectionId = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (xpp.getName().equals("book-title")) {
                    bookTitle = xpp.nextText();
                } else if (xpp.getName().equals("first-name")) {
                    firstName = xpp.nextText();
                } else if (xpp.getName().equals("middle-name")) {
                    middleName = xpp.nextText();
                } else if (xpp.getName().equals("last-name")) {
                    lastName = xpp.nextText();
                } else if (xpp.getName().equals("a")) {
                    String link = xpp.getAttributeValue(null, "l:href");
                    String type = xpp.getAttributeValue(null, "type");
                    if ("note".equals(type)) {
                        System.out.println(xpp.nextText() + ">" + link);
                    }
                } else if (xpp.getName().equals("a")) {
                    String link = xpp.getAttributeValue(null, "l:href");
                    String type = xpp.getAttributeValue(null, "type");
                    if ("note".equals(type)) {
                        System.out.println(xpp.nextText() + ">" + link);
                    }
                } else if (xpp.getName().equals("section")) {
                    sectionId = xpp.getAttributeValue(null, "id");
                    if (sectionId != null) {
                        System.out.println(sectionId);
                    }
                }

            } else if (eventType == XmlPullParser.TEXT) {
                if (sectionId != null) {
                    String trim = xpp.getText().trim();
                    if (trim.length() > 0) {
                        System.out.println(trim);
                    }
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (xpp.getName().equals("section")) {
                    sectionId = null;
                }
            }

            eventType = xpp.next();
        }
        System.out.println(bookTitle);
        System.out.println(firstName + " " + middleName + " " + lastName);

    }

    public static void fb2ToEpub(String inputFile, String outputDir) throws Exception {

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(outputDir, "demo.epub")));
        zos.setLevel(0);

        writeToZip(zos, "mimetype", mimetype);
        writeToZip(zos, "META-INF/container.xml", container_xml);
        writeToZip(zos, "OEBPS/content.opf", content_opf);

        String encoding = getFB2Encoding(inputFile);
        List<String> titles = getFb2Titles(inputFile, encoding);

        String ncx = genetateNCX(titles);
        writeToZip(zos, "OEBPS/fb2.ncx", ncx);

        ByteArrayOutputStream generateFb2File = generateFb2File(inputFile, encoding);
        writeToZip(zos, "OEBPS/fb2.fb2", new ByteArrayInputStream(generateFb2File.toByteArray()));

        zos.close();

    }

    public static String genetateNCX(List<String> titles) {
        StringBuilder navs = new StringBuilder();
        for (int i = 0; i < titles.size(); i++) {
            navs.append(createNavPoint(i + 1, titles.get(i)));
        }
        return NCX.replace("%nav%", navs.toString());
    }

    public static ByteArrayOutputStream generateFb2File(String fb2, String encoding) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(fb2), encoding));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        String line;

        int count = 0;

        while ((line = input.readLine()) != null) {
            if (line.contains("windows-1251")) {
                line = line.replace("windows-1251", "utf-8");
            }
            if (line.contains("</title>")) {
                count++;
                line = line.replace("</title>", "<a id=\"" + count + "\"></a></title>");
            }
            if (line.contains("</subtitle>")) {
                count++;
                line = line.replace("</subtitle>", "<a id=\"" + count + "\"></a></subtitle>");
            }
            writer.println(line);
        }
        input.close();
        writer.close();

        return out;
    }

    public static String getFB2Encoding(String fb2) throws Exception {
        InputStream encodingCheck = new FileInputStream(fb2);
        String encoding = "UTF-8";
        byte[] header = new byte[80];
        encodingCheck.read(header);
        if (new String(header).contains("windows-1251")) {
            encoding = "cp1251";
        }
        encodingCheck.close();
        return encoding;
    }

    public static List<String> getFb2Titles(String fb2, String encoding) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();

        xpp.setInput(new FileInputStream(fb2), encoding);
        int eventType = xpp.getEventType();

        boolean isTitle = false;
        List<String> titles = new ArrayList<>();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (xpp.getName().equals("title") || xpp.getName().equals("subtitle")) {
                    isTitle = true;
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                isTitle = false;

            } else if (eventType == XmlPullParser.TEXT) {
                if (isTitle) {
                    String title = xpp.getText().trim();
                    if (title.length() != 0) {
                        titles.add(title);
                    }
                }
            }
            eventType = xpp.next();
        }
        return titles;
    }

    public static String createNavPoint(int id, String text) {
        return "<navPoint id=\"toc-" + id + "\" playOrder=\"" + id + "\">\n" + //
                "<navLabel>\n" + //
                "<text>" + text + "</text>\n" + //
                "</navLabel>\n" + //
                "<content src=\"fb2.fb2#" + id + "\"/>\n" + //
                "</navPoint>"; //
    }

    public static void writeToZip(ZipOutputStream zos, String name, InputStream stream) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zipCopy(stream, zos);
    }

    public static void writeToZip(ZipOutputStream zos, String name, String content) throws IOException {
        writeToZip(zos, name, new ByteArrayInputStream(content.getBytes()));
    }

    private static final int BUFFER_SIZE = 16 * 1024;

    public static void zipCopy(InputStream inputStream, OutputStream zipStream) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = inputStream.read(bytesIn)) != -1) {
            zipStream.write(bytesIn, 0, read);
        }
        inputStream.close();
    }

}
