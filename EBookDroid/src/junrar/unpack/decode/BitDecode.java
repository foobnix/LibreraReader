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
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class BitDecode extends Decode
{
	/**
	 * 
	 */
	public BitDecode()
	{
		decodeNum = new int[Compress.BC];
	}
}
