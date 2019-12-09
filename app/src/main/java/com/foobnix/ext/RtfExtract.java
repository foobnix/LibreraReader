package com.foobnix.ext;

import android.text.TextUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.model.BookCSS;
import com.rtfparserkit.converter.text.AbstractTextConverter;
import com.rtfparserkit.parser.IRtfParser;
import com.rtfparserkit.parser.IRtfSource;
import com.rtfparserkit.parser.RtfListenerAdaptor;
import com.rtfparserkit.parser.RtfStreamSource;
import com.rtfparserkit.parser.standard.StandardRtfParser;
import com.rtfparserkit.rtf.Command;
import com.rtfparserkit.utils.HexUtils;

import net.arnx.wmf2svg.gdi.svg.SvgGdi;
import net.arnx.wmf2svg.gdi.wmf.WmfParser;
import net.arnx.wmf2svg.util.ImageUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class RtfExtract {

    private static final String WMF = "wmf";

    public static FooterNote extract(String inputPath, final String outputDir, final String fileName) throws IOException {

        File file = new File(outputDir, fileName);
        try {

            final PrintWriter writer = new PrintWriter(file);

            writer.println("<!DOCTYPE html>");
            writer.println("<html>");

            writer.println("<body>");

            InputStream is = new FileInputStream(inputPath);
            IRtfSource source = new RtfStreamSource(is);
            IRtfParser parser = new StandardRtfParser() {
                @Override
                public void processCommand(Command command, int parameter, boolean hasParameter, boolean optional) {
                    try {
                        super.processCommand(command, parameter, hasParameter, optional);
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }
            };

            boolean isEnableHypens = BookCSS.get().isAutoHypens && TxtUtils.isNotEmpty(AppSP.get().hypenLang);
            if (isEnableHypens) {
                HypenUtils.applyLanguage(AppSP.get().hypenLang);
            }

            HypenUtils.resetTokenizer();

            parser.parse(source, new AbstractTextConverter() {
                boolean isImage;
                String format = "jpg";
                int counter = 0;


                @Override
                public void processExtractedText(String text) {

                    String htmlEncode = TextUtils.htmlEncode(text);
                    if (isEnableHypens) {
                        htmlEncode = HypenUtils.applyHypnes(htmlEncode);
                    }
                    printText(htmlEncode);
                    writer.print(" ");
                }

                @Override
                public void processString(String string) {
                    super.processString(string);
                    if (isImage) {
                        try {
                            isImage = false;

                            if (WMF.equals(format)) {
                                String imageName = fileName + counter++ + ".png";

                                ImageUtil.testOut = new File(outputDir, imageName).toString();

                                byte[] bytes = HexUtils.parseHexString(string);
                                InputStream in = new ByteArrayInputStream(bytes);

                                WmfParser parser = new WmfParser();
                                SvgGdi gdi = new SvgGdi();
                                parser.parse(in, gdi);


                                // FileOutputStream fileWriter = new FileOutputStream(new File(outputDir,
                                // "1.svg"));
                                // gdi.write(fileWriter);
                                // fileWriter.close();

                                ImageUtil.testOut = null;

                                writer.write("<img src='" + imageName + "' />");

                            } else {
                                String imageName = fileName + counter++ + ".rtf." + format;

                                FileOutputStream fileWriter = new FileOutputStream(new File(outputDir, imageName));
                                byte[] in = HexUtils.parseHexString(string);
                                fileWriter.write(in);
                                fileWriter.flush();
                                fileWriter.close();
                                writer.write("<img src='" + imageName + "' />");
                            }

                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                }

                Set<Command> stack = new HashSet<Command>();

                private void printText(String txt) {


                    if (stack.contains(Command.par))
                        writer.print("<p></p>");

                    if (stack.contains(Command.sub))
                        writer.print("<sub>");

                    if (stack.contains(Command.supercmd))
                        writer.print("<sup>");

                    if (stack.contains(Command.b))
                        writer.print("<b>");

                    if (stack.contains(Command.i))
                        writer.print("<i>");

                    writer.print(txt);

                    if (stack.contains(Command.par)) {
                        //writer.print("</p>");
                        stack.remove(Command.par);
                    }

                    if (stack.contains(Command.sub)) {
                        writer.print("</sub>");
                        stack.remove(Command.sub);
                    }

                    if (stack.contains(Command.supercmd)) {
                        writer.print("</sup>");
                        stack.remove(Command.supercmd);
                    }

                    if (stack.contains(Command.b)) {
                        writer.print("</b>");
                        stack.remove(Command.b);
                    }

                    if (stack.contains(Command.i)) {
                        writer.print("</i>");
                        stack.remove(Command.i);
                    }

                }

                @Override
                // http://latex2rtf.sourceforge.net/rtfspec_62.html
                public void processCommand(Command command, int parameter, boolean hasParameter, boolean optional) {
                    try {
                        super.processCommand(command, parameter, hasParameter, optional);
                    } catch (Exception e) {
                        LOG.e(e);
                    }


                    if (command == Command.cbpat || command == Command.line) {
                        writer.write("<br/>");
                    }

                    //writer.write("[" + command + "]");

                    if (parameter == 0 && (command == Command.i || command == Command.b)) {
                        //skip
                    } else {
                        stack.add(command);
                    }

                    if (command == Command.pngblip) {
                        isImage = true;
                        format = "png";
                    }
                    if (command == Command.jpegblip) {
                        isImage = true;
                        format = "jpg";
                    }
                    if (command == Command.wmetafile) {
                        isImage = true;
                        format = WMF;
                    }
                }

            });

            writer.println("</body></html>");

            writer.close();

        } catch (Exception e) {
            LOG.e(e);
        }
        return new FooterNote(file.getPath(), null);
    }

    static byte[] decode = null;

    public static byte[] getImageCover(String path) {
        try {

            final FileInputStream in = new FileInputStream(path);
            final BufferedInputStream fileInputStream = new BufferedInputStream(in);
            IRtfSource source = new RtfStreamSource(fileInputStream);
            IRtfParser parser = new StandardRtfParser();

            decode = null;

            parser.parse(source, new RtfListenerAdaptor() {
                boolean pict = false;

                @Override
                public void processString(String string) {
                    if (decode == null && pict) {
                        decode = HexUtils.parseHexString(string);
                        try {
                            in.close();
                            fileInputStream.close();
                        } catch (IOException e) {
                            LOG.e(e);
                        }
                    }
                }

                @Override
                public void processCommand(Command command, int parameter, boolean hasParameter, boolean optional) {
                    if (command == Command.pngblip || command == Command.jpegblip) {
                        pict = true;
                    }
                }

            });
            in.close();
            fileInputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }


        return decode;

    }

}
