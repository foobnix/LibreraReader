package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.pdf.info.model.BookCSS;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;
import org.zwobble.mammoth.images.Image;
import org.zwobble.mammoth.images.ImageConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DocxContext extends PdfContext {

    File cacheFile;

    @Override protected EpubProcessingSettings.Scope captureProcessingSettings(String path) {
        return EpubProcessingSettings.capture();
    }

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + EpubProcessingSettings.key();
        cacheFile = new File(new File(CacheZipUtils.CACHE_BOOK_DIR,
                ConversionCache.key(fileNameOriginal) + "-docx-v2"), "book.html");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        if (cacheFile == null) cacheFile = getCacheFileName(fileName);
        try (BookCacheLeases.PublishedFile output = ConversionCache.buildDirectory(
                cacheFile.getParentFile(), cacheFile.getName(),
                directory -> convertDocx(new File(fileName), new File(directory, cacheFile.getName())))) {
            MuPdfDocument document = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF,
                    output.file.getPath(), password);
            document.retainCacheSource(cacheFile.getParentFile());
            return document;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot convert DOCX book", failure);
        }
    }

    private void convertDocx(File source, File destination) throws IOException {
            DocumentConverter converter = new DocumentConverter().
                    imageConverter(new ImageConverter.ImgElement() {
                        @Override
                        public Map<String, String> convert(Image image) throws IOException {


                            String imageName = destination.getName() + "+" + image.hashCode() + "." + image.getContentType().replace("image/", "");
                            LOG.d("ImageConverter name", imageName);

                            FileOutputStream out = new FileOutputStream(new File(destination.getParent(), imageName));
                            IOUtils.copyClose(image.getInputStream(), out);


                            Map<String, String> map = new HashMap<>();
                            map.put("src", imageName);
                            return map;
                        }
                    });


            try {
                Result<String> result = converter.convertToHtml(source);

                String html = result.getValue();
                html = html.replace("<br /><br />", "<empty-line />");
                if (EpubProcessingSettings.isEnableBBCode()) {
                    html = TxtUtils.convertBBCodeToHtml(html);
                }
                if (EpubProcessingSettings.isAutoHypens() && TxtUtils.isNotEmpty(EpubProcessingSettings.language())) {
                    LOG.d("docx-isAutoHypens", EpubProcessingSettings.isAutoHypens());
                    HypenUtils.applyLanguage(EpubProcessingSettings.language());
                    HypenUtils.resetTokenizer();
                    html = HypenUtils.applyHypnes(html);
                }

                try (FileOutputStream out = new FileOutputStream(destination)) {
                    out.write("<html><head></head><body>".getBytes());
                    out.write(html.getBytes());
                    out.write("</body></html>".getBytes());
                    out.getFD().sync();
                }

            } catch (IOException e) {
                throw e;
            }
    }
}
