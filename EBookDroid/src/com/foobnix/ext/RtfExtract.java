package com.foobnix.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import com.foobnix.android.utils.LOG;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.rtfparserkit.converter.text.StringTextConverter;
import com.rtfparserkit.parser.IRtfParser;
import com.rtfparserkit.parser.IRtfSource;
import com.rtfparserkit.parser.RtfListenerAdaptor;
import com.rtfparserkit.parser.RtfStreamSource;
import com.rtfparserkit.parser.standard.StandardRtfParser;
import com.rtfparserkit.rtf.Command;
import com.rtfparserkit.utils.HexUtils;

import android.text.TextUtils;

public class RtfExtract {

	public static FooterNote extract(String inputPath, final String outputDir, final String fileName)
			throws IOException {

		File file = new File(outputDir, fileName);
		try {

			final PrintWriter writer = new PrintWriter(file);

			writer.println("<!DOCTYPE html>");
			writer.println("<html>");

			writer.println("<body>");

			InputStream is = new FileInputStream(inputPath);
			IRtfSource source = new RtfStreamSource(is);
			IRtfParser parser = new StandardRtfParser();

            if (BookCSS.get().isAutoHypens) {
                HypenUtils.applyLanguage(BookCSS.get().hypenLang);
            }

			parser.parse(source, new StringTextConverter() {
				boolean isImage;
				boolean isBR;
				String format = "jpg";
				int counter = 0;

				@Override
				public void processExtractedText(String text) {

					String htmlEncode = TextUtils.htmlEncode(text);
                    if (BookCSS.get().isAutoHypens) {
                        htmlEncode = HypenUtils.applyHypnes(htmlEncode);
                    }
                    writer.println(htmlEncode);
					isBR = false;
				}

				@Override
				public void processString(String string) {
					super.processString(string);
					if (isImage) {
						try {
							isImage = false;
							String imageName = fileName + counter++ + ".rtf." + format;
							FileOutputStream fileWriter = new FileOutputStream(new File(outputDir, imageName));
							fileWriter.write(HexUtils.parseHexString(string));
							fileWriter.flush();
							fileWriter.close();

							writer.write("<img src='" + imageName + "' />");

						} catch (Exception e) {
							LOG.e(e);
						}
					}
				}

				@Override
				public void processCommand(Command command, int parameter, boolean hasParameter, boolean optional) {
					super.processCommand(command, parameter, hasParameter, optional);
					if (command == Command.cbpat || command == Command.par || command == Command.line) {
						if (!isBR) {
							writer.write("<br/>");
							isBR = true;
						}
					}
					if (command == Command.pngblip) {
						isImage = true;
						format = "png";
					}
					if (command == Command.jpegblip) {
						isImage = true;
						format = "jpg";
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
