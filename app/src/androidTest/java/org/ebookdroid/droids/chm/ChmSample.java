package org.ebookdroid.droids.chm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

/**
 * The CHM the tests open: a title page with a red picture, two chapters, an appendix under a
 * heading of the contents that opens no page itself, and a page nothing lists or links to.
 * <p>
 * It carries the habits of real help files: names in the contents and links that differ in case
 * from the files ({@code Chapter1.htm} for {@code /chapter1.htm}), a {@code ms-its:} link, paths
 * relative to a subfolder, {@code &nbsp;}, Office's {@code <o:p>}, a comment holding
 * {@code --}, a script, {@code <a name>} targets, a stylesheet with a {@code url()}. The two
 * chapters are long enough that the compressed section spans several 32 KB frames and two LZX
 * resets, and the picture is stored in section 0 while the pages are compressed in section 1.
 */
final class ChmSample {

    static final String TITLE = "The CHM Sample Book";
    static final String WELCOME = "Welcome to the compiled help sample";
    static final String CHAPTER_ONE = "The first chapter begins here";
    static final String SECTION = "Section one point one starts";
    static final String CHAPTER_TWO = "The second chapter begins here";
    static final String APPENDIX = "The appendix closes the book";
    static final String ORPHAN = "A page that nothing links to";

    private static final String SENTENCE = "The sample speaks of beginnings and of carefully"
            + " numbered pages, written out at length so that the engine has something to"
            + " paginate and the compressor something to work through.";

    private ChmSample() {
    }

    static byte[] build() throws IOException {
        final ChmWriter chm = new ChmWriter();
        chm.add("/#SYSTEM", ChmWriter.system("Sample.hhc", "index.htm", TITLE, 0x0409), false);
        chm.add("/images/red.png", png(), false);
        chm.add("/sample.hhc", hhc(), true);
        // Sizes pinned the way help files pin them, which would not follow the reader's size
        chm.add("/style.css", "body { color: #000; background: #fff url(Images/Red.PNG) no-repeat -9999px 0; font-size: 11px; }\n"
                + "/* the body text */ .text { font-family: Verdana, sans-serif; font-size: 9pt; line-height: 14px; }\n"
                + "h1 { font: bold 18pt/24pt Arial, sans-serif; }\n"
                + ".nav { font-size: x-small; }\n", true);
        chm.add("/index.htm", page("Title page",
                "<h1>" + TITLE + "</h1><p>" + WELCOME + "&nbsp;&mdash; read on.</p>"
                        + "<p><img src=\"Images/Red.PNG\" width=\"600\" height=\"600\" alt=\"red\"></p>"
                        + "<p class=\"nav\"><a href=\"Chapter1.htm\">Chapter one</a></p>"
                        + "<p><font size=\"4\">A larger line</font></p>"), true);
        chm.add("/chapter1.htm", page("Chapter one",
                "<h1>Chapter one</h1><p>" + CHAPTER_ONE + ".<o:p></o:p></p>"
                        + "<!-- a comment -- with dashes -->"
                        + "<script>document.write('<p>never</p>')</script>"
                        + paragraphs(120)
                        + "<h2><a name=\"sec11\"></a>Section 1.1</h2><p>" + SECTION + ".</p>"
                        + paragraphs(120)
                        + "<p><a href=\"ms-its:sample.chm::/chapters/Chapter2.HTM\">next</a></p>"), true);
        chm.add("/chapters/chapter2.htm", page("Chapter two",
                "<h1>Chapter two</h1><p>" + CHAPTER_TWO + ".</p>"
                        + paragraphs(200)
                        + "<p><a href=\"../appendix.htm\">appendix</a> "
                        + "<a href=\"../index.htm#top\">top</a> "
                        + "<a href=\"http://www.example.com/\">away</a></p>"), true);
        chm.add("/appendix.htm", page("Appendix", "<h1>Appendix A</h1><p>" + APPENDIX + ".</p>"), true);
        chm.add("/orphan.htm", page("Orphan", "<p>" + ORPHAN + ".</p>"), true);
        return chm.build();
    }

    private static String hhc() {
        return "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML//EN\">\n<HTML><HEAD>\n"
                + "<meta name=\"GENERATOR\" content=\"Microsoft&reg; HTML Help Workshop 4.1\">\n"
                + "<!-- Sitemap 1.0 --></HEAD><BODY>\n"
                + "<OBJECT type=\"text/site properties\"><param name=\"Window Styles\" value=\"0x800025\"></OBJECT>\n"
                + "<UL>\n"
                + item("Introduction", "index.htm")
                + item("Chapter One", "Chapter1.htm")
                + "<UL>\n" + item("Section 1.1", "chapter1.htm#sec11") + "</UL>\n"
                + item("Chapter Two", "chapters\\Chapter2.htm")
                + "<LI> <OBJECT type=\"text/sitemap\"><param name=\"Name\" value=\"Appendices\"></OBJECT>\n"
                + "<UL>\n" + item("Appendix A", "APPENDIX.HTM") + "</UL>\n"
                + "</UL>\n</BODY></HTML>\n";
    }

    private static String item(final String name, final String local) {
        return "<LI> <OBJECT type=\"text/sitemap\">\n"
                + "<param name=\"Name\" value=\"" + name + "\">\n"
                + "<param name=\"Local\" value=\"" + local + "\">\n"
                + "</OBJECT>\n";
    }

    private static String page(final String title, final String body) {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n"
                + "<HTML><HEAD><META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"
                + "<TITLE>" + title + "</TITLE>"
                + "<LINK rel=\"stylesheet\" type=\"text/css\" href=\"/Style.css\"></HEAD>"
                + "<BODY onload=\"init()\">" + body + "</BODY></HTML>\n";
    }

    private static String paragraphs(final int n) {
        final StringBuilder s = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            s.append("<P class=\"text\">Paragraph ").append(i).append(". ").append(SENTENCE).append("\n");
        }
        return s.toString();
    }

    /** A solid red square, written out as a PNG by hand so the test carries no binary of its own. */
    static byte[] png() throws IOException {
        final int side = 60;
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        for (int y = 0; y < side; y++) {
            raw.write(0);
            for (int x = 0; x < side; x++) {
                raw.write(255);
                raw.write(0);
                raw.write(0);
            }
        }
        final ByteArrayOutputStream png = new ByteArrayOutputStream();
        png.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        final ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        writeInt(ihdr, side);
        writeInt(ihdr, side);
        ihdr.write(8);
        ihdr.write(2);
        ihdr.write(0);
        ihdr.write(0);
        ihdr.write(0);
        chunk(png, "IHDR", ihdr.toByteArray());
        final ByteArrayOutputStream z = new ByteArrayOutputStream();
        final DeflaterOutputStream deflate = new DeflaterOutputStream(z);
        deflate.write(raw.toByteArray());
        deflate.close();
        chunk(png, "IDAT", z.toByteArray());
        chunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static void chunk(final ByteArrayOutputStream png, final String type, final byte[] data) throws IOException {
        writeInt(png, data.length);
        final byte[] t = type.getBytes("US-ASCII");
        png.write(t);
        png.write(data);
        final CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        writeInt(png, (int) crc.getValue());
    }

    private static void writeInt(final ByteArrayOutputStream out, final int v) {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }
}
