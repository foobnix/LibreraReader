/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 26.06.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 * 
 * the unrar licence applies to all junrar source and binary distributions 
 * you are not allowed to use this source to re-create the RAR compression
 * algorithm
 * 
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;" 
 */
package junrar.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class ReadOnlyAccessInputStream extends InputStream {
	private IReadOnlyAccess file;
	
	private long curPos;
	private final long endPos;
	
	public ReadOnlyAccessInputStream(IReadOnlyAccess file, long startPos,
            long endPos) throws IOException {
		super();
		this.file = file;
		curPos = startPos;
		this.endPos = endPos;
		file.setPosition(curPos);
	}

	@Override
	public int read() throws IOException {
        if (curPos == endPos) {
            return -1;
        }
        else {
            int b = file.read();
            curPos++;
            return b;
        }
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (curPos == endPos) {
            return -1;
        }
        int bytesRead = file.read(b, off,
                (int)Math.min(len, endPos - curPos));
        curPos += bytesRead;
        return bytesRead;
	}

	@Override
	public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
	}
//
//    public void close() throws IOException {
//        file.close();
//    }
}
