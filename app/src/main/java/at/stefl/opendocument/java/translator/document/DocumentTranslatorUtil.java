package at.stefl.opendocument.java.translator.document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.stefl.commons.lwxml.writer.LWXMLMultiWriter;
import at.stefl.commons.lwxml.writer.LWXMLStreamWriter;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.odf.OpenDocument;
import at.stefl.opendocument.java.odf.OpenDocumentPresentation;
import at.stefl.opendocument.java.odf.OpenDocumentSpreadsheet;
import at.stefl.opendocument.java.odf.OpenDocumentText;
import at.stefl.opendocument.java.translator.settings.TranslationSettings;
import at.stefl.opendocument.java.util.FileCache;

public class DocumentTranslatorUtil {

	private static final List<String> SINGLE_TITLE = Collections
			.singletonList("");
	private static final String DEFAULT_CACHE_PREFIX = "odf_";
	private static final String DEFAULT_CACHE_SUFFIX = ".html";

	public static class Output {
		private FileCache cache;
		private int count;
		private List<String> names;
		private List<String> titles;
		private LWXMLMultiWriter writer;

		private Output() {
		}

		public Output(FileCache cache, List<String> names, List<String> titles,
				LWXMLWriter[] outs) {
			if ((names.size() != titles.size())
					|| (names.size() != outs.length))
				throw new IllegalArgumentException("different sizes");

			this.cache = cache;
			this.count = names.size();
			this.names = new ArrayList<String>(names);
			this.titles = new ArrayList<String>(titles);
			this.writer = new LWXMLMultiWriter(outs);
		}

		@Override
		public String toString() {
			return names.toString();
		}

		public FileCache getFileCache() {
			return cache;
		}

		public int getCount() {
			return count;
		}

		public List<String> getNames() {
			return names;
		}

		public List<String> getTitles() {
			return titles;
		}

		public LWXMLMultiWriter getWriter() {
			return writer;
		}
	}

	public static Output provideOutput(OpenDocument document,
			TranslationSettings settings) throws IOException {
		return provideOutput(document, settings, DEFAULT_CACHE_PREFIX,
				DEFAULT_CACHE_SUFFIX);
	}

	// TODO: sub file cache
	public static Output provideOutput(OpenDocument document,
			TranslationSettings settings, String cachePrefix, String cacheSuffix)
			throws IOException {
		Output output = new Output();
		FileCache cache = settings.getCache();

		if (!settings.isSplitPages() || (document instanceof OpenDocumentText)) {
			output.count = 1;
			output.titles = SINGLE_TITLE;
		} else {
			if (document instanceof OpenDocumentSpreadsheet) {
				output.count = document.getAsSpreadsheet().getTableCount();
				output.titles = document.getAsSpreadsheet().getTableNames();
			} else if (document instanceof OpenDocumentPresentation) {
				output.count = document.getAsPresentation().getPageCount();
				output.titles = document.getAsPresentation().getPageNames();
			} else {
				throw new IllegalStateException("unsupported document");
			}
		}

		output.names = new ArrayList<String>();
		LWXMLWriter[] outs = new LWXMLWriter[output.count];

		for (int i = 0; i < output.count; i++) {
			String name = cachePrefix + i + cacheSuffix;
			output.names.add(name);

			File file = cache.create(name);
			outs[i] = new LWXMLStreamWriter(new FileWriter(file));
		}

		output.cache = cache;
		output.names = Collections.unmodifiableList(output.names);
		output.titles = Collections.unmodifiableList(output.titles);
		output.writer = new LWXMLMultiWriter(outs);

		return output;
	}

	public static Output translate(OpenDocument document,
			TranslationSettings settings) throws IOException {
		Output output = provideOutput(document, settings);
		translate(document, settings, output);
		output.writer.close();
		return output;
	}

	public static void translate(OpenDocument document,
			TranslationSettings settings, Output output) throws IOException {
		DocumentTranslator translator;

		if (!settings.isSplitPages() || (document instanceof OpenDocumentText)) {
			if (document instanceof OpenDocumentText) {
				translator = new TextTranslator();
			} else if (document instanceof OpenDocumentSpreadsheet) {
				translator = new SpreadsheetTranslator();
			} else if (document instanceof OpenDocumentPresentation) {
				translator = new PresentationTranslator();
			} else {
				throw new IllegalStateException("unsupported document");
			}
		} else {
			if (document instanceof OpenDocumentSpreadsheet) {
				translator = new BulkSpreadsheetTranslator();
			} else if (document instanceof OpenDocumentPresentation) {
				translator = new BulkPresentationTranslator();
			} else {
				throw new IllegalStateException("unsupported document");
			}
		}

		translator.translate(document, output.getWriter(), settings);
	}

	private DocumentTranslatorUtil() {
	}

}
