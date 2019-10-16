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
public class Compress {
	public static final int CODEBUFSIZE   		= 0x4000;
	public static final int MAXWINSIZE    		= 0x400000;
	public static final int MAXWINMASK      	= (MAXWINSIZE-1);

	public static final int LOW_DIST_REP_COUNT 	= 16;

	public static final int NC 					= 299;  /* alphabet = {0, 1, 2, ..., NC - 1} */
	public static final int DC  				= 60;
	public static final int LDC 				= 17;
	public static final int RC  				= 28;
	public static final int HUFF_TABLE_SIZE 	= (NC+DC+RC+LDC);
	public static final int BC  				= 20;

	public static final int NC20 				= 298;  /* alphabet = {0, 1, 2, ..., NC - 1} */
	public static final int DC20 				= 48;
	public static final int RC20 				= 28;
	public static final int BC20 				= 19;
	public static final int MC20 				= 257;
}
