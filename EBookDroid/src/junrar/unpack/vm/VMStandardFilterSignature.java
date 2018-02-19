/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 04.06.2007
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
package junrar.unpack.vm;

/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class VMStandardFilterSignature {
	private int length;

	private int CRC;

	private VMStandardFilters type;

	public VMStandardFilterSignature(int length, int crc, VMStandardFilters type) {
		super();
		this.length = length;
		CRC = crc;
		this.type = type;
	}

	public int getCRC() {
		return CRC;
	}

	public void setCRC(int crc) {
		CRC = crc;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public VMStandardFilters getType() {
		return type;
	}

	public void setType(VMStandardFilters type) {
		this.type = type;
	}

}
