package org.ebookdroid.droids;

import org.ebookdroid.core.codec.CodecDocument;

/**
 * A {@code .doc}: Word 97-2003, or what is often saved under that name - RTF, a .docx, plain
 * text. See {@link OfficeContext}.
 */
public class DocContext extends OfficeContext {

    @Override
    protected CodecDocument openOther(String fileName, String password) {
        // neither Word nor RTF: as text, as the name was given to it
        return new TxtContext().openDocumentInner(fileName, password);
    }
}
