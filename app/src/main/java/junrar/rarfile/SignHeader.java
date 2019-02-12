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
 * sign header
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class SignHeader extends BaseBlock {
	
	public static final short signHeaderSize = 8;
	
	private int creationTime=0;
	private short arcNameSize=0;
	private short userNameSize=0;
	
	
	public SignHeader(BaseBlock bb, byte[] signHeader){
		super(bb);
		
		int pos = 0;
		creationTime = Raw.readIntLittleEndian(signHeader, pos);
		pos +=4;
		arcNameSize = Raw.readShortLittleEndian(signHeader, pos);
		pos+=2;
		userNameSize = Raw.readShortLittleEndian(signHeader, pos);
	}
	
	public short getArcNameSize() {
		return arcNameSize;
	}
	
	public int getCreationTime() {
		return creationTime;
	}
	
	public short getUserNameSize() {
		return userNameSize;
	}
}
