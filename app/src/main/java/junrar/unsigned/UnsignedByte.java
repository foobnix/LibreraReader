/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 04.06.2007
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
package junrar.unsigned;


/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class UnsignedByte {
	
	public static byte longToByte(long unsignedByte1){
		return (byte) (unsignedByte1&0xff);
	}
	public static byte intToByte(int unsignedByte1){
		return (byte) (unsignedByte1&0xff);
	}
	public static byte shortToByte(short unsignedByte1){
		return (byte) (unsignedByte1&0xff);
	}
	
	
	public static short add(byte unsignedByte1, byte unsignedByte2){
		return (short) (unsignedByte1 + unsignedByte2);
	}
	
	public static short sub(byte unsignedByte1, byte unsignedByte2){
		
		return (short) (unsignedByte1 - unsignedByte2);
	}
	
	
	public static void main(String[] args)
	{
	//tests unsigned (signed)
		//add
		System.out.println(add((byte)0xfe,(byte)0x01)); //255 (-1)
		System.out.println(add((byte)0xff,(byte)0x01)); //0 (0)
		System.out.println(add((byte)0x7f,(byte)0x01)); //128 (-128)
		System.out.println(add((byte)0xff,(byte)0xff)); //254 (-2)
		
		//sub
		System.out.println(sub((byte)0xfe,(byte)0x01)); //253 (-3)
		System.out.println(sub((byte)0x00,(byte)0x01)); //255 (-1)
		System.out.println(sub((byte)0x80,(byte)0x01)); //127 (127)
		//mul
		System.out.println((byte)-1*(byte)-1);
	}
}
