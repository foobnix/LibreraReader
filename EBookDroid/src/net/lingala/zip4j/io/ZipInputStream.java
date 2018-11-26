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

import java.io.IOException;
import java.io.InputStream;

import net.lingala.zip4j.exception.ZipException;

public class ZipInputStream extends InputStream {
	
	private BaseInputStream is;
	
	public ZipInputStream(BaseInputStream is) {
		this.is = is;
	}

	public int read() throws IOException {
		int readByte = is.read();
		if (readByte != -1) {
			is.getUnzipEngine().updateCRC(readByte);
		}
		return readByte;
	}
	
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	public int read(byte[] b, int off, int len) throws IOException {
		int readLen = is.read(b, off, len);
		if (readLen > 0 && is.getUnzipEngine() != null) {
			is.getUnzipEngine().updateCRC(b, off, readLen);
		}
		return readLen;
	}
	
	/**
	 * Closes the input stream and releases any resources.
	 * This method also checks for the CRC of the extracted file.
	 * If CRC check has to be skipped use close(boolean skipCRCCheck) method
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		close(false);
	}
	
	/**
	 * Closes the input stream and releases any resources.
	 * If skipCRCCheck flag is set to true, this method skips CRC Check
	 * of the extracted file
	 * 
	 * @throws IOException
	 */
	public void close(boolean skipCRCCheck) throws IOException {
		try {
			is.close();
			if (!skipCRCCheck && is.getUnzipEngine() != null) {
				is.getUnzipEngine().checkCRC();
			}
		} catch (ZipException e) {
			throw new IOException(e.getMessage());
		}
	}
	
	public int available() throws IOException {
		return is.available();
	}
	
	public long skip(long n) throws IOException {
		return is.skip(n);
	}
	
}
