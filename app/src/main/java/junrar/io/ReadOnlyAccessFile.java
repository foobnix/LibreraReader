/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 23.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 * 
 * the unrar licence applies to all junrar source and binary distributions 
 * you are not allowed to use this source to re-create the RAR compression algorithm
 * 
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;" 
 */
package junrar.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class ReadOnlyAccessFile extends RandomAccessFile
        implements IReadOnlyAccess{

	/**
	 * @param file the file
	 * @throws FileNotFoundException
	 */
	public ReadOnlyAccessFile(File file) throws FileNotFoundException {
		super(file, "r");
	}

	public int readFully(byte[] buffer, int count) throws IOException {
        assert (count > 0) : count;
        this.readFully(buffer, 0, count);
        return count;
    }

	public long getPosition() throws IOException {
        return this.getFilePointer();
	}

	public void setPosition(long pos) throws IOException {
        this.seek(pos);
	}
}
