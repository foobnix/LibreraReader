/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 01.06.2007
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
package junrar.unpack.ppm;


/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public enum BlockTypes
{
	BLOCK_LZ(0), BLOCK_PPM(1);

	private int blockType;

	private BlockTypes(int blockType)
	{
		this.blockType = blockType;
	}

	public int getBlockType()
	{
		return blockType;
	}

	public boolean equals(int blockType)
	{
		return this.blockType == blockType;
	}

	public static BlockTypes findBlockType(int blockType)
	{
		if (BLOCK_LZ.equals(blockType)) {
			return BLOCK_LZ;
		}
		if (BLOCK_PPM.equals(blockType)) {
			return BLOCK_PPM;
		}
		return null;
	}
}
