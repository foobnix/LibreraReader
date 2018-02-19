/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 30.05.2007
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

import java.io.EOFException;
import java.io.IOException;

/**
 * A File like access to a byte array.
 * (seek and read certain number of bytes)
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class ReadOnlyAccessByteArray implements IReadOnlyAccess{

	private int positionInFile;
	private byte[] file;
	
	/**
	 * Initialize with byte[ ]
	 * @param file the file given as byte array
	 */
	public ReadOnlyAccessByteArray(byte[] file){
		if(file == null){
			throw new NullPointerException("file must not be null!!");
		}
		this.file = file;
		this.positionInFile = 0;
	}

    public long getPosition() throws IOException {
		return positionInFile;
	}

	public void setPosition(long pos) throws IOException {
		if (pos < file.length && pos >= 0){
			this.positionInFile = (int)pos;
		}
        else{
			throw new EOFException();
		}
	}

    /** Read a single byte of data. */
    public int read() throws IOException {
        return file[positionInFile++];
    }

	/**
     * Read up to <tt>count</tt> bytes to the specified buffer.
     */
    public int read(byte[] buffer, int off, int count) throws IOException {
        int read = Math.min(count, file.length-positionInFile);
        System.arraycopy(file, positionInFile, buffer, off, read);
        positionInFile += read;
        return read;
    }

	public int readFully(byte[] buffer, int count) throws IOException {
		if(buffer == null ){
			throw new NullPointerException("buffer must not be null");
		}
		if(count == 0){
			throw new IllegalArgumentException("cannot read 0 bytes ;-)");
		}
		int read = Math.min(count, file.length-(int)positionInFile-1);	
		System.arraycopy(file, (int)positionInFile, buffer, 0, read );
		positionInFile+=read;
		return read;
	}

    public void close() throws IOException {
    }
}
