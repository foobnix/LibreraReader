package org.ebookdroid.droids;

import java.io.File;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.RtfExtract;
import com.foobnix.pdf.info.model.BookCSS;

public class RtfContext extends PdfContext {

	File cacheFile;

	@Override
	public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang;
		cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameOriginal.hashCode() + ".html");
		return cacheFile;
	}

	@Override
	public CodecDocument openDocumentInner(String fileName, String password) {
        if (!cacheFile.isFile()) {
			try {
				RtfExtract.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath(), cacheFile.getName());
			} catch (Exception e) {
				LOG.e(e);
			}
		}

		MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, cacheFile.getPath(), password);
		return muPdfDocument;
	}
}
