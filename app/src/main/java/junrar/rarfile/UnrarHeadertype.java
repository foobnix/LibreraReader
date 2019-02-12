/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 22.05.2007
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

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public enum UnrarHeadertype {
	
	
	/**
	 * 
	 */
	MainHeader		((byte)0x73),
	
	/**
	 * 
	 */
	MarkHeader		((byte)0x72),
	
	/**
	 * 
	 */
	FileHeader 		((byte) 0x74),
	
	/**
	 * 
	 */
	CommHeader  	((byte) 0x75),
	
	/**
	 * 
	 */
	AvHeader 		((byte) 0x76),
	
	/**
	 * 
	 */
	SubHeader 		((byte)  0x77),
	
	/**
	 * 
	 */
	ProtectHeader  	((byte) 0x78),
	
	/**
	 * 
	 */
	SignHeader 		((byte)  0x79),
	
	/**
	 * 
	 */
	NewSubHeader 	((byte) 0x7a),
	
	/**
	 * 
	 */
	EndArcHeader 	((byte)  0x7b);
	
	/**
	 * Returns the enum according to the given byte or null 
	 * @param headerType the headerbyte
	 * @return the enum or null
	 */
	public static UnrarHeadertype findType(byte headerType) 
	{
		if(UnrarHeadertype.MarkHeader.equals(headerType)){
			return UnrarHeadertype.MarkHeader;
		}
		if(UnrarHeadertype.MainHeader.equals(headerType)){
			return UnrarHeadertype.MainHeader;
		}
		if(UnrarHeadertype.FileHeader.equals(headerType)){
			return UnrarHeadertype.FileHeader;
		}
		if(UnrarHeadertype.EndArcHeader.equals(headerType)){
			return UnrarHeadertype.EndArcHeader;
		}
		if(UnrarHeadertype.NewSubHeader.equals(headerType)){
			return UnrarHeadertype.NewSubHeader;
		}
		if(UnrarHeadertype.SubHeader.equals(headerType)){
			return UnrarHeadertype.SubHeader;
		}
		if(UnrarHeadertype.SignHeader.equals(headerType)){
			return UnrarHeadertype.SignHeader;
		}
		if(UnrarHeadertype.ProtectHeader.equals(headerType)){
			return UnrarHeadertype.ProtectHeader;
		}
		if(UnrarHeadertype.MarkHeader.equals(headerType)){
			return UnrarHeadertype.MarkHeader;
		}
		if(UnrarHeadertype.MainHeader.equals(headerType)){
			return UnrarHeadertype.MainHeader;
		}
		if(UnrarHeadertype.FileHeader.equals(headerType)){
			return UnrarHeadertype.FileHeader;
		}
		if(UnrarHeadertype.EndArcHeader.equals(headerType)){
			return UnrarHeadertype.EndArcHeader;
		}
		if(UnrarHeadertype.CommHeader.equals(headerType)){
			return UnrarHeadertype.CommHeader;
		}
		if(UnrarHeadertype.AvHeader.equals(headerType)){
			return UnrarHeadertype.AvHeader;
		}
		return null;
	}

	
	
	private byte headerByte;
	
	private UnrarHeadertype(byte headerByte)
	{
		this.headerByte = headerByte;
	}

	
	/**
	 * Return true if the given byte is equal to the enum's byte
	 * @param header
	 * @return true if the given byte is equal to the enum's byte
	 */
	public boolean equals(byte header)
	{
		return headerByte == header;
	}


	/**
	 * the header byte of this enum
	 * @return the header byte of this enum
	 */
	public byte getHeaderByte() {
		return headerByte;
	}


	
	
}
