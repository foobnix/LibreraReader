/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 20.11.2007
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

public enum SubBlockHeaderType 
{
	EA_HEAD 	((short)0x100),
	UO_HEAD 	((short)0x101),
	MAC_HEAD 	((short)0x102),
	BEEA_HEAD 	((short)0x103),
    NTACL_HEAD 	((short)0x104),
    STREAM_HEAD ((short)0x105);
	
	private short subblocktype;

	private SubBlockHeaderType(short subblocktype)
	{
		this.subblocktype = subblocktype;
	}
	
	/**
	 * Return true if the given value is equal to the enum's value
	 * @param subblocktype
	 * @return true if the given value is equal to the enum's value
	 */
	public boolean equals(short subblocktype)
	{
		return this.subblocktype == subblocktype;
	}

	/**
	 * find the header type for the given short value
	 * @param SubType the short value
	 * @return the correspo nding enum or null
	 */
	public static SubBlockHeaderType findSubblockHeaderType(short subType)
	{
		if(EA_HEAD.equals(subType)){
			return EA_HEAD;
		}else if(UO_HEAD.equals(subType)){
			return UO_HEAD;
		}else if(MAC_HEAD.equals(subType)){
			return MAC_HEAD;
		}else if(BEEA_HEAD.equals(subType)){
			return BEEA_HEAD;
		}else if(NTACL_HEAD.equals(subType)){
			return NTACL_HEAD;
		}else if(STREAM_HEAD.equals(subType)){
			return STREAM_HEAD;
		}
		return null;
	}
	
	/**
	 * @return the short representation of this enum
	 */
	public short getSubblocktype() {
		return subblocktype;
	}
}
