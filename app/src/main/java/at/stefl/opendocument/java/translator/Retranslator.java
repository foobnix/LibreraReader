package at.stefl.opendocument.java.translator;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import at.stefl.commons.io.ByteStreamUtil;
import at.stefl.commons.io.CharStreamUtil;
import at.stefl.commons.io.CloseableOutputStream;
import at.stefl.commons.io.JoinInputStream;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLIllegalEventException;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.reader.LWXMLStreamReader;
import at.stefl.commons.lwxml.reader.LWXMLTeeReader;
import at.stefl.commons.lwxml.writer.LWXMLStreamWriter;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.odf.OpenDocument;
import at.stefl.opendocument.java.odf.OpenDocumentFile;

// TODO: improve
public class Retranslator {

	public static final String EVENT_NUMBER_PREFIX = "ODR ";

	public static void writeEventNumber(LWXMLPushbackReader in, LWXMLWriter out)
			throws IOException {
		out.writeComment(Retranslator.EVENT_NUMBER_PREFIX
				+ in.getCurrentEventNumber());
	}

	public static void flushValues(LWXMLReader in) throws IOException {
		while (in.readEvent() != LWXMLEvent.END_DOCUMENT)
			in.readValue();
	}

	public static void flushValuesUntilEvent(LWXMLReader in, LWXMLEvent event)
			throws IOException {
		if (!event.hasValue())
			throw new LWXMLIllegalEventException(event);

		while (true) {
			LWXMLEvent currentEvent = in.readEvent();
			if (currentEvent == LWXMLEvent.END_DOCUMENT)
				throw new EOFException();
			if (currentEvent == event)
				return;

			in.readValue();
		}
	}

	public static void flushValuesUntilEventNumber(LWXMLReader in,
			long eventNumber) throws IOException {
		while (true) {
			LWXMLEvent event = in.readEvent();
			if (event == LWXMLEvent.END_DOCUMENT)
				throw new EOFException();
			if (in.getCurrentEventNumber() >= eventNumber)
				return;

			in.readValue();
		}
	}

	public static void retranslate(OpenDocument document, InputStream[] htmls,
			OutputStream out) throws IOException {
		retranslate(document, new JoinInputStream(htmls), out);
	}

	public static void retranslate(OpenDocument document, InputStream html,
			OutputStream out) throws IOException {
		CharStreamUtil charStreamUtil = new CharStreamUtil();

		ZipOutputStream zout = new ZipOutputStream(out);
		zout.putNextEntry(new ZipEntry("content.xml"));

		LWXMLReader contentIn = new LWXMLStreamReader(document.getContent());
		LWXMLReader htmlIn = new LWXMLStreamReader(html);
		LWXMLWriter contentOut = new LWXMLStreamWriter(
				new CloseableOutputStream(zout));

		LWXMLReader tee = new LWXMLTeeReader(contentIn, contentOut);

		while (true) {
			try {
				do {
					LWXMLUtil.flushUntilEvent(htmlIn, LWXMLEvent.COMMENT);
				} while (!CharStreamUtil
						.matchChars(htmlIn, EVENT_NUMBER_PREFIX));
			} catch (EOFException e) {
				break;
			}

			// get original characters event number
			long eventNumber = Long.parseLong(htmlIn.readValue());
			System.out.println(eventNumber);
			// next event has to be characters
			if (htmlIn.readEvent() != LWXMLEvent.CHARACTERS)
				throw new LWXMLIllegalEventException(htmlIn);

			// flush content until characters event number
			flushValuesUntilEventNumber(tee, eventNumber);
			// current event has to be characters
			if (tee.getCurrentEvent() != LWXMLEvent.CHARACTERS)
				throw new LWXMLIllegalEventException(tee);
			// skip original characters
			charStreamUtil.flush(contentIn);

			// copy html content
			contentOut.write(htmlIn);
		}

		flushValues(tee);
		contentOut.close();

		OpenDocumentFile documentFile = document.getDocumentFile();

		for (String fileName : documentFile.getFileNames()) {
			if (fileName.equals("content.xml"))
				continue;

			zout.putNextEntry(new ZipEntry(fileName));
			ByteStreamUtil.writeStreamBuffered(
					documentFile.getFileStream(fileName), zout);
		}

		zout.close();
	}

	private Retranslator() {
	}

}