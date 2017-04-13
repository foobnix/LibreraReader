package com.foobnix.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import com.foobnix.android.utils.LOG;
import com.rtfparserkit.parser.IRtfParser;
import com.rtfparserkit.parser.IRtfSource;
import com.rtfparserkit.parser.RtfListenerAdaptor;
import com.rtfparserkit.parser.RtfStreamSource;
import com.rtfparserkit.parser.standard.StandardRtfParser;
import com.rtfparserkit.rtf.Command;
import com.rtfparserkit.utils.HexUtils;

import android.text.TextUtils;

public class RtfExtract2 {

    public static final String OUT_FB2_XML = "rtf.html";

    public static FooterNote extract(String inputPath, final String outputDir) throws IOException {

        File file = new File(outputDir, OUT_FB2_XML);
        try {

            InputStream input = new FileInputStream(inputPath);
            final PrintWriter writer = new PrintWriter(file);

            writer.println("<!DOCTYPE html>");
            writer.println("<html>");

            writer.println("<body>");

            // InputStream is = new FileInputStream("/path/to/my/file.rtf");
            IRtfSource source = new RtfStreamSource(input);
            IRtfParser parser = new StandardRtfParser();

            parser.parse(source, new RtfListenerAdaptor() {

                boolean isBold;
                boolean isSkip = false;
                boolean isItalic;
                boolean isImage;
                String format = "jpg";
                int counter = 0;

                @Override
                public void processString(String string) {
                    if (isSkip) {
                        LOG.d("Skip text", string);
                        return;
                    }

                    if (isBold) {
                        writer.write("<b>");
                    }
                    if (isItalic) {
                        writer.write("<i>");
                    }

                    if (isImage) {
                        try {
                            String imageName = "image" + counter++ + ".rtf." + format;
                            FileOutputStream fileWriter = new FileOutputStream(new File(outputDir, imageName));
                            fileWriter.write(HexUtils.parseHexString(string));
                            fileWriter.flush();
                            fileWriter.close();

                            writer.write("<img src='" + imageName + "' />");

                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    } else if (string.length() < 1000) {
                        String txt = TextUtils.htmlEncode(string);
                        writer.println(txt);
                        // LOG.d("write", txt);

                        // writer.println(string);
                    }
                    if (isBold) {
                        writer.write("</b>");
                    }
                    if (isItalic) {
                        writer.write("</i>");
                    }

                }

                @Override
                public void processGroupStart() {
                    isBold = false;
                    isItalic = false;
                    isImage = false;
                }

                @Override
                public void processGroupEnd() {
                    isBold = false;
                    isItalic = false;
                    isImage = false;
                    isSkip = false;

                }

                @Override
                public void processCommand(Command command, int arg1, boolean arg2, boolean arg3) {
                    if (command == Command.b) {
                        isBold = false;
                    }
                    if (command == Command.i) {
                        isItalic = false;
                    }
                    if (command == Command.cbpat) {
                        writer.write("<br/>");
                    }
                    if (command == Command.par) {
                        writer.write("<br/>");
                    }
                    if (command == Command.line) {
                        writer.write("<br/>");
                    }

                    if (command == Command.pngblip) {
                        isImage = true;
                        format = "png";
                    }
                    if (command == Command.jpegblip) {
                        isImage = true;
                        format = "jpg";
                    }
                    if (
                    //
                    command == Command.fcharset || //
                    command == Command.datastore || //
                    command == Command.themedata || //
                    command == Command.datafield || //
                    command == Command.colorschememapping || //
                    command == Command.latentstyles || //
                    command == Command.template || //
                    command == Command.cs || //
                    command == Command.falt || //
                    command == Command.panose || //
                    command == Command.wmetafile //

                    //
                    ) {
                        isSkip = true;
                    }
                    // LOG.d("@command", command);
                }

            });

            writer.println("</body></html>");

            input.close();
            writer.close();

        } catch (Exception e) {
            LOG.e(e);
        }
        return new FooterNote(file.getPath(), null);
    }

    static byte[] decode = null;

    public static byte[] getImageCover(String path) {
        File file = new File(path);
        try {

            IRtfSource source = new RtfStreamSource(new FileInputStream(path));
            IRtfParser parser = new StandardRtfParser();

            decode = null;

            parser.parse(source, new RtfListenerAdaptor() {
                boolean pict = false;

                @Override
                public void processString(String string) {
                    if (decode == null && pict) {
                        decode = HexUtils.parseHexString(string);
                    }
                }

                @Override
                public void processCommand(Command command, int parameter, boolean hasParameter, boolean optional) {
                    if (command == Command.pngblip || command == Command.jpegblip) {
                        pict = true;
                    }
                }

            });
        } catch (Exception e) {
            LOG.e(e);
        }

        return decode;

    }

}
