package com.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.rtfparserkit.converter.text.StringTextConverter;
import com.rtfparserkit.parser.IRtfParser;
import com.rtfparserkit.parser.IRtfSource;
import com.rtfparserkit.parser.RtfStreamSource;
import com.rtfparserkit.parser.standard.StandardRtfParser;
import com.rtfparserkit.rtf.Command;
import com.rtfparserkit.utils.HexUtils;

public class DemoRTF {

    public static void main(String[] args) throws IOException {

        InputStream inputStream = new FileInputStream(new File("/mount/extHDD/help/Dropbox/Projects/BookTestingDB/RTF/Языки и системы программирования.rtf"));

        // InputStream is = new FileInputStream("/path/to/my/file.rtf");
        IRtfSource source = new RtfStreamSource(inputStream);
        IRtfParser parser = new StandardRtfParser();

        parser.parse(source, new StringTextConverter() {

            boolean pict = false;
            int counter = 0;
            String format = "png";

            public void processString1(String arg0) {
                if (pict) {

                    try {
                        FileOutputStream fileWriter = new FileOutputStream(new File("/home/ivan-dev/dev/workspace/pdf4/ZTestProject/images/image" + counter++ + ".rtf." + format));
                        fileWriter.write(HexUtils.parseHexString(arg0));
                        fileWriter.flush();
                        fileWriter.close();

                    } catch (Exception e) {

                    }
                }
            }

            int i = 0;

            @Override
            public void processString(String text) {
                if (i++ < 400) {
                    System.out.println(text);
                }
            }

            @Override
            public void processGroupStart() {
                System.out.println("[");

            }

            @Override
            public void processGroupEnd() {
                System.out.println("]");
                pict = false;

            }

            @Override
            public void processCommand(Command command, int arg1, boolean arg2, boolean arg3) {
                if (i > 400) {
                    return;
                }
                if (command == Command.b) {
                    System.out.println("<b>");
                }
                if (command == Command.i) {
                    System.out.println("<i>");
                }
                if (command == Command.cbpat) {
                    System.out.println("<br>");
                }
                if (command == Command.pict) {
                    pict = true;
                }
                if (command == Command.pngblip) {
                    format = "png";
                }
                if (command == Command.jpegblip) {
                    format = "jpg";
                }
                System.out.print("@" + command + " ");

            }
        });

    }
}
