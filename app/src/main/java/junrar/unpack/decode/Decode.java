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
package junrar.unpack.decode;

/**
 * Used to store information for lz decoding
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class Decode
{
	private int maxNum;

	private final int[] decodeLen = new int[16];

	private final int[] decodePos = new int[16];

	protected int[] decodeNum = new int[2];

	/**
	 * returns the decode Length array
	 * @return decodeLength
	 */
	public int[] getDecodeLen()
	{
		return decodeLen;
	}

	/**
	 * returns the decode num array
	 * @return decodeNum
	 */
	public int[] getDecodeNum()
	{
		return decodeNum;
	}

	/**
	 * returns the decodePos array
	 * @return decodePos
	 */
	public int[] getDecodePos()
	{
		return decodePos;
	}

	/**
	 * returns the max num
	 * @return maxNum
	 */
	public int getMaxNum()
	{
		return maxNum;
	}

	/**
	 * sets the max num
	 * @param maxNum to be set to maxNum
	 */
	public void setMaxNum(int maxNum)
	{
		this.maxNum = maxNum;
	}

}
