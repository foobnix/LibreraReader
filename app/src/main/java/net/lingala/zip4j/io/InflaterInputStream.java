/*
* Copyright 2010 Srikanth Reddy Lingala  
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License. 
* You may obtain a copy of the License at 
* 
* http://www.apache.org/licenses/LICENSE-2.0 
* 
* Unless required by applicable law or agreed to in writing, 
* software distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*/

package net.lingala.zip4j.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import net.lingala.zip4j.unzip.UnzipEngine;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Zip4jConstants;

public class InflaterInputStream extends PartInputStream {
	
	private Inflater inflater;
	private byte[] buff;
	private byte[] oneByteBuff = new byte[1];
	private UnzipEngine unzipEngine;
	private long bytesWritten;
	private long uncompressedSize;
	
	public InflaterInputStream(RandomAccessFile raf, long start, long len, UnzipEngine unzipEngine) {
		super(raf, start, len, unzipEngine);
		this.inflater = new Inflater(true);
		this.buff = new byte[InternalZipConstants.BUFF_SIZE];
		this.unzipEngine = unzipEngine;
		bytesWritten = 0;
		uncompressedSize = unzipEngine.getFileHeader().getUncompressedSize();
	}

	public int read() throws IOException {
		return read(oneByteBuff, 0, 1) == -1 ? -1 : oneByteBuff[0] & 0xff;
	}
	
	public int read(byte[] b) throws IOException {
		if (b == null) {
			throw new NullPointerException("input buffer is null");
		}
		
		return read(b, 0, b.length);
	}
	
	public int read(byte[] b, int off, int len) throws IOException {
		
		if (b == null) {
			throw new NullPointerException("input buffer is null");
		} else if (off < 0 || len < 0 || len > b.length - off) {
		    throw new IndexOutOfBoundsException();
		} else if (len == 0) {
		    return 0;
		}
		
		try {
		    int n;
		    if (bytesWritten >= uncompressedSize) {
		    	finishInflating();
		    	return -1;
		    }
		    while ((n = inflater.inflate(b, off, len)) == 0) {
				if (inflater.finished() || inflater.needsDictionary()) {
					finishInflating();
				    return -1;
				}
				if (inflater.needsInput()) {
					fill();
				}
		    }
		    bytesWritten += n;
		    return n;
		} catch (DataFormatException e) {
		    String s = "Invalid ZLIB data format";
		    if (e.getMessage() != null) {
		    	s = e.getMessage();
		    }
		    if (unzipEngine != null) {
		    	if (unzipEngine.getLocalFileHeader().isEncrypted() && 
		    			unzipEngine.getLocalFileHeader().getEncryptionMethod() == Zip4jConstants.ENC_METHOD_STANDARD) {
		    		s += " - Wrong Password?";
		    	}
		    }
		    throw new IOException(s);
		}
	}
	
	private void finishInflating() throws IOException {
		//In some cases, compelte data is not read even though inflater is complete
		//make sure to read complete data before returning -1
		byte[] b = new byte[1024];
		while (super.read(b, 0, 1024) != -1) {
			//read all data
		}
		checkAndReadAESMacBytes();
	}
	
	private void fill() throws IOException {
		int len = super.read(buff, 0, buff.length);
		if (len == -1) {
		    throw new EOFException("Unexpected end of ZLIB input stream");
		}
		inflater.setInput(buff, 0, len);
	}
	
	/**
     * Skips specified number of bytes of uncompressed data.
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped.
     * @exception IOException if an I/O error has occurred
     * @exception IllegalArgumentException if n < 0
     */
	public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
		int max = (int)Math.min(n, Integer.MAX_VALUE);
		int total = 0;
		byte[] b = new byte[512];
		while (total < max) {
		    int len = max - total;
		    if (len > b.length) {
		    	len = b.length;
		    }
		    len = read(b, 0, len);
		    if (len == -1) {
	            break;
		    }
		    total += len;
		}
		return total;
    }
	
	
	public void seek(long pos) throws IOException {
		super.seek(pos);
	}
	
	/**
     * Returns 0 after EOF has been reached, otherwise always return 1.
     * <p>
     * Programs should not count on this method to return the actual number
     * of bytes that could be read without blocking.
     *
     * @return     1 before EOF and 0 after EOF.
     * @exception  IOException  if an I/O error occurs.
     * 
     */
	public int available() {
		return inflater.finished() ? 0 : 1;
	}
	
	public void close() throws IOException {
		inflater.end();
		super.close();
	}
	
	public UnzipEngine getUnzipEngine() {
		return super.getUnzipEngine();
	}
}
