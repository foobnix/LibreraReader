/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 24.05.2007
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

import junrar.io.Raw;

/**
 * extended version info header
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class AVHeader extends BaseBlock {
	
	public static final int avHeaderSize = 7;
	
	private byte unpackVersion;
	private byte method;
	private byte avVersion;
	private int avInfoCRC;
	
	public AVHeader(BaseBlock bb, byte[] avHeader){
		super(bb);
		
		int pos =0;
		unpackVersion |= avHeader[pos]&0xff;
		pos++;
		method |= avHeader[pos]&0xff;
		pos++;
		avVersion |= avHeader[pos]&0xff;
		pos++;
		avInfoCRC = Raw.readIntLittleEndian(avHeader, pos);
	}
	
	public int getAvInfoCRC() {
		return avInfoCRC;
	}
	
	public byte getAvVersion() {
		return avVersion;
	}
	
	public byte getMethod() {
		return method;
	}
	
	public byte getUnpackVersion() {
		return unpackVersion;
	}
}
