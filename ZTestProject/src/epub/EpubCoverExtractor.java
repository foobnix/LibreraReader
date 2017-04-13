package epub;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class EpubCoverExtractor {
    public static void main(String[] args) throws Exception {

        String pathname = "/home/ivan-dev/dev/workspace/pdf4/ZTestProject/input/Komarovskiy_Zdorove.epub";
        InputStream inputStream = new FileInputStream(new File(pathname));
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        ZipEntry nextEntry = null;

        String title = null;
        String author = null;

        String coverName = null;
        String coverResource = null;

        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            String name = nextEntry.getName().toLowerCase();
            if (name.endsWith(".opf")) {

                XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
                xpp.setInput(zipInputStream, "utf-8");

                int eventType = xpp.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if ("dc:title".equals(xpp.getName())) {
                            title = xpp.nextText();
                        }
                        if ("dc:creator".equals(xpp.getName())) {
                            author = xpp.nextText();
                        }

                        if ("meta".equals(xpp.getName()) && "cover".equals(xpp.getAttributeValue(null, "name"))) {
                            coverResource = xpp.getAttributeValue(null, "content");
                        }

                        if (coverResource != null && "item".equals(xpp.getName()) && coverResource.equals(xpp.getAttributeValue(null, "id"))) {
                            coverName = xpp.getAttributeValue(null, "href");
                        }
                    }
                    eventType = xpp.next();
                }
                System.out.println(title);
                System.out.println(author);
                System.out.println("Cover name: " + coverName);

            }
        }

    }

}
