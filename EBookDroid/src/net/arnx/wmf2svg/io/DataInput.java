/*
 * Copyright 2007-2008 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.wmf2svg.io;

import java.io.*;
import java.nio.ByteOrder;

/**
 * @author Hidekatsu Izuno
 */
public class DataInput {
	private InputStream in;
	private ByteOrder endian;
	
	private byte[] buf = new byte[4];
	private int count = 0;

	/**
	 * Create a DataInput class instance using by native order.
	 *
	 * @param in the input stream that had better buffer by a BufferedInputStream.
	 **/	
	public DataInput(InputStream in) {
		this.endian = ByteOrder.nativeOrder();
		this.in = in;
	}

	/**
	 * Create a DataInput class instance.
	 *
	 * @param in the input stream that had better buffer by a BufferedInputStream.
	 * @param endian the endian of the input stream 
	 **/	
	public DataInput(InputStream in, ByteOrder endian) {
		if (endian != ByteOrder.BIG_ENDIAN && endian != ByteOrder.LITTLE_ENDIAN) {
			throw new IllegalArgumentException();
		}
		this.in = in;
		this.endian = endian;
	}
	
	/**
	 * Reads the next one byte of this input stream as a signed 8-bit integer.
	 *
	 * @return the <code>int</code> value as a signed 8-bit integer.
	 * @exception IOException if an I/O error occurs.
	 * @exception EOFException if this stream reaches the end before reading all the bytes.
	 **/
	public int readByte() throws IOException, EOFException {
		if (in.read(buf, 0, 1) == 1) {
			count += 1;
			return (0xff & buf[0]);
		}
		throw new EOFException();
	}

	/**
	 * Reads the next two bytes of this input stream as a signed 16-bit integer.
	 *
	 * @return the <code>int</code> value as a signed 16-bit integer.
	 * @exception IOException if an I/O error occurs.
	 * @exception EOFException if this stream reaches the end before reading all the bytes.
	 **/
	public int readInt16() throws IOException, EOFException {
		if (in.read(buf, 0, 2) == 2) {
			short value = 0;
			if (endian == ByteOrder.BIG_ENDIAN) {
				value |= (0xff & buf[1]);
				value |= (0xff & buf[0]) << 8;
			} else { 
				value |= (0xff & buf[0]);
				value |= (0xff & buf[1]) << 8;
			}
			count += 2;
			return value;
		}
		throw new EOFException();
	}

	/**
	 * Reads the next four bytes of this input stream as a signed 32-bit integer.
	 *
	 * @return the <code>int</code> value as a signed 32-bit integer.
	 * @exception IOException if an I/O error occurs.
	 * @exception EOFException if this stream reaches the end before reading all the bytes.
	 **/
	public int readInt32() throws IOException, EOFException {
		if (in.read(buf, 0, 4) == 4) {
			int value = 0;
			if (endian == ByteOrder.BIG_ENDIAN) {
				value |= (0xff & buf[3]);
				value |= (0xff & buf[2]) << 8;
				value |= (0xff & buf[1]) << 16;
				value |= (0xff & buf[0]) << 24;
			} else {
				value |= (0xff & buf[0]);
				value |= (0xff & buf[1]) << 8;
				value |= (0xff & buf[2]) << 16;
				value |= (0xff & buf[3]) << 24;
			}
			count += 4;
			return value;
		}
		throw new EOFException();
	}

	/**
	 * Reads the next two bytes of this input stream as a unsigned 16-bit integer.
	 *
	 * @return the <code>int</code> value as a unsigned 16-bit integer.
	 * @exception IOException if an I/O error occurs.
	 * @exception EOFException if this stream reaches the end before reading all the bytes.
	 **/
	public int readUint16() throws IOException, EOFException {
		if (in.read(buf, 0, 2) == 2) {
			int value = 0;
			if (endian == ByteOrder.BIG_ENDIAN) {
				value |= (0xff & buf[1]);
				value |= (0xff & buf[0]) << 8;
			} else {
				value |= (0xff & buf[0]);
				value |= (0xff & buf[1]) << 8;
			}
			count += 2;
			return value;
		}
		throw new EOFException();
	}

	/**
	 * Reads the next four bytes of this input stream as a unsigned 32-bit integer.
	 *
	 * @return the <code>long</code> value as a unsigned 32-bit integer.
	 * @exception IOException if an I/O error occurs.
	 * @exception EOFException if this stream reaches the end before reading all the bytes.
	 **/
	public long readUint32() throws IOException, EOFException {
		if (in.read(buf, 0, 4) == 4) {
			long value = 0;
			if (endian == ByteOrder.BIG_ENDIAN) {
				value |= (0xff & buf[3]);
				value |= (0xff & buf[2]) << 8;
				value |= (0xff & buf[1]) << 16;
				value |= (0xff & buf[0]) << 24;
			} else {
				value |= (0xff & buf[0]);
				value |= (0xff & buf[1]) << 8;
				value |= (0xff & buf[2]) << 16;
				value |= (0xff & buf[3]) << 24;
			}
			count += 4;
			return value;
		}
		throw new EOFException();
	}

	public byte[] readBytes(int n) throws IOException, EOFException {
		byte[] array = new byte[n];
		if (in.read(array) == n) {
			count += n;
			return array;
		}
		throw new EOFException();
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return count;
	}
	
	public void close() {
		try {
			in.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}