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
 * 
 * the optional End header
 * 
 */
public class EndArcHeader extends BaseBlock{
	
	public static final short endArcArchiveDataCrcSize = 4;
	public static final short endArcVolumeNumberSize = 2;
	
	private int archiveDataCRC;
	private short volumeNumber;
	
	
	public EndArcHeader(BaseBlock bb, byte[] endArcHeader){
		super(bb);
		
		int pos = 0;
		if(hasArchiveDataCRC()){
			archiveDataCRC =Raw.readIntLittleEndian(endArcHeader, pos);
			pos+=4;
		}
		if(hasVolumeNumber()){
			volumeNumber = Raw.readShortLittleEndian(endArcHeader, pos);
		}
	}
	
	public int getArchiveDataCRC() {
		return archiveDataCRC;
	}

	public short getVolumeNumber() {
		return volumeNumber;
	}
}
