package org.ebookdroid.droids;

import java.io.File;
import java.util.Map;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.pdf.info.JsonHelper;

public class Fb2Context extends PdfContext {

	File cacheFile;

	@Override
	public File getCacheFileName(String fileNameOriginal) {
		cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameOriginal.hashCode() + ".epub");
		return cacheFile;
	}

	@Override
	public CodecDocument openDocumentInner(final String fileName, String password) {

		if (fileName.endsWith("reflow.fb2")) {
			return new HtmlContext().openDocument(fileName, password);
		}

		String outName;
		if (cacheFile.isFile()) {
			outName = cacheFile.getPath();
		} else {
			outName = cacheFile.getPath();
			Fb2Extractor.get().convert(fileName, outName);
		}

		LOG.d("Fb2Context open", outName);

		final MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password);

		final File jsonFile = new File(cacheFile + ".json");
		if (jsonFile.isFile()) {
			muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile));
			LOG.d("Load notes from file", jsonFile);
		} else {

			new Thread() {
				@Override
				public void run() {
					Map<String, String> notes = Fb2Extractor.get().getFooterNotes(fileName);
					muPdfDocument.setFootNotes(notes);
					JsonHelper.mapToFile(jsonFile, notes);
					LOG.d("save notes to file", jsonFile);
				};
			}.start();
		}

		return muPdfDocument;
	}

}
