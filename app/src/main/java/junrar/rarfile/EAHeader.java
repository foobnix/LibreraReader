/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 27.11.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 * 
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
package junrar.rarfile;

import android.util.Log;

import junrar.io.Raw;


/**
 * extended archive CRC header
 *
 */
public class EAHeader 
extends SubBlockHeader 
{
	private static String TAG = EAHeader.class.getName();
	
	public static final short EAHeaderSize = 10;
	
	private int unpSize;
	private byte unpVer;
	private byte method;
	private int EACRC;
	
	public EAHeader(SubBlockHeader sb, byte[] eahead)
	{
		super(sb);
		int pos = 0;
		unpSize = Raw.readIntLittleEndian(eahead, pos);
		pos+=4;
		unpVer |= eahead[pos]&0xff;
		pos++;
		method |= eahead[pos]&0xff;
		pos++;
		EACRC = Raw.readIntLittleEndian(eahead, pos);
	}

	/**
	 * @return the eACRC
	 */
	public int getEACRC() {
		return EACRC;
	}

	/**
	 * @return the method
	 */
	public byte getMethod() {
		return method;
	}

	/**
	 * @return the unpSize
	 */
	public int getUnpSize() {
		return unpSize;
	}

	/**
	 * @return the unpVer
	 */
	public byte getUnpVer() {
		return unpVer;
	}
	
	public void print()
	{
		super.print();
		Log.i(TAG, "unpSize: "+unpSize);
		Log.i(TAG, "unpVersion: " + unpVer);
		Log.i(TAG, "method: "+method);
		Log.i(TAG, "EACRC:" + EACRC);
	}
}

