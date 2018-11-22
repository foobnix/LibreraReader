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

package net.lingala.zip4j.util;

import java.io.DataInput;
import java.io.IOException;

import net.lingala.zip4j.exception.ZipException;

public class Raw
{
	public static long readLongLittleEndian(byte[] array,int pos){
		long temp = 0;
		temp |= array[pos+7]&0xff;
		temp <<=8;
		temp |= array[pos+6]&0xff;
		temp <<=8;
		temp |= array[pos+5]&0xff;
		temp <<=8;
		temp |= array[pos+4]&0xff;
		temp <<=8;
		temp |= array[pos+3]&0xff;
		temp <<=8;
		temp |= array[pos+2]&0xff;
		temp <<=8;
		temp |= array[pos+1]&0xff;
		temp <<=8;
		temp |= array[pos]&0xff;
		return temp;
	}
	
	public static int readLeInt(DataInput di, byte[] b) throws ZipException{
		try {
			di.readFully(b, 0, 4);
		} catch (IOException e) {
			throw new ZipException(e);
		}
	    return ((b[0] & 0xff) | (b[1] & 0xff) << 8)
		    | ((b[2] & 0xff) | (b[3] & 0xff) << 8) << 16;
	}

	public static int readShortLittleEndian(byte[] b, int off){
	    return (b[off] & 0xff) | (b[off+1] & 0xff) << 8;
	}
	
	public static final short readShortBigEndian(byte[] array, int pos) {
		short temp = 0;
		temp |= array[pos] & 0xff;
		temp <<= 8;
		temp |= array[pos + 1] & 0xff;
		return temp;
	}

	public static int readIntLittleEndian(byte[] b, int off){
	    return ((b[off] & 0xff) | (b[off+1] & 0xff) << 8)
		    | ((b[off+2] & 0xff) | (b[off+3] & 0xff) << 8) << 16;
	}
	
	public static byte[] toByteArray(int in,int outSize) {
		byte[] out = new byte[outSize];
		byte[] intArray = toByteArray(in);
		for( int i=0; i<intArray.length && i<outSize; i++ ) {
			out[i] = intArray[i];
		}
		return out;
	}
	
	public static byte[] toByteArray(int in) {
		byte[] out = new byte[4];

		out[0] = (byte)in;
		out[1] = (byte)(in >> 8);
		out[2] = (byte)(in >> 16);
		out[3] = (byte)(in >> 24);

		return out;
	}
	
	public static final void writeShortLittleEndian(byte[] array, int pos,
			short value) {
		array[pos +1] = (byte) (value >>> 8);
		array[pos ] = (byte) (value & 0xFF);

	}
	
	public static final void writeIntLittleEndian(byte[] array, int pos,int value) {
		array[pos+3] = (byte) (value >>>24); 
		array[pos+2] = (byte) (value >>>16);
		array[pos+1] = (byte) (value >>>8);
		array[pos] = (byte) (value &0xFF);
		
	}
	
	public static void writeLongLittleEndian(byte[] array, int pos, long value){
		array[pos+7] = (byte) (value >>>56); 
		array[pos+6] = (byte) (value >>>48);
		array[pos+5] = (byte) (value >>>40);
		array[pos+4] = (byte) (value >>>32);
		array[pos+3] = (byte) (value >>>24); 
		array[pos+2] = (byte) (value >>>16);
		array[pos+1] = (byte) (value >>>8);
		array[pos] = (byte) (value &0xFF);
	}
	
	public static byte bitArrayToByte(int[] bitArray) throws ZipException {
		if (bitArray == null) {
			throw new ZipException("bit array is null, cannot calculate byte from bits");
		}
		
		if (bitArray.length != 8) {
			throw new ZipException("invalid bit array length, cannot calculate byte");
		}
		
		if(!checkBits(bitArray)) {
			throw new ZipException("invalid bits provided, bits contain other values than 0 or 1");
		}
		
		int retNum = 0;
		for (int i = 0; i < bitArray.length; i++) {
			retNum += Math.pow(2, i) * bitArray[i];
		}
		
		return (byte)retNum;
	}
	
	private static boolean checkBits(int[] bitArray) {
		for (int i = 0; i < bitArray.length; i++) {
			if (bitArray[i] != 0 && bitArray[i] != 1) {
				return false;
			}
		}
		return true;
	}
	
	public static void prepareBuffAESIVBytes(byte[] buff, int nonce, int length) {
		buff[0] = (byte)nonce;
		buff[1] = (byte)(nonce >> 8);
		buff[2] = (byte)(nonce >> 16);
		buff[3] = (byte)(nonce >> 24);
		buff[4] = 0;
		buff[5] = 0;
		buff[6] = 0;
		buff[7] = 0;
		buff[8] = 0;
		buff[9] = 0;
		buff[10] = 0;
		buff[11] = 0;
		buff[12] = 0;
		buff[13] = 0;
		buff[14] = 0;
		buff[15] = 0;
	}
	
	/**
	 * Converts a char array to byte array 
	 * @param charArray
	 * @return byte array representation of the input char array
	 */
	public static byte[] convertCharArrayToByteArray(char[] charArray) {
		if (charArray == null) {
			throw new NullPointerException();
		}
		
		byte[] bytes = new byte[charArray.length];
		for(int i=0;i<charArray.length;i++) {
		   bytes[i] = (byte) charArray[i];
		}
		return bytes;
	}
}
