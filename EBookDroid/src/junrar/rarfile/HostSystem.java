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
public enum HostSystem {
	msdos  ((byte)0),
	os2  ((byte)1),
	win32  ((byte)2),
	unix  ((byte)3),
	macos  ((byte)4),
	beos   ((byte)5);
	
	private byte hostByte;
	
	public static HostSystem findHostSystem(byte hostByte){
		if(HostSystem.msdos.equals(hostByte)){
			return HostSystem.msdos;
		}
		if(HostSystem.os2.equals(hostByte)){
			return HostSystem.os2;
		}
		if(HostSystem.win32.equals(hostByte)){
			return HostSystem.win32;
		}
		if(HostSystem.unix.equals(hostByte)){
			return HostSystem.unix;
		}
		if(HostSystem.macos.equals(hostByte)){
			return HostSystem.macos;
		}
		if(HostSystem.beos.equals(hostByte)){
			return HostSystem.beos;
		}
		return null;
	}
	
	
	private HostSystem(byte hostByte){
		this.hostByte = hostByte;
	}
	
	public boolean equals(byte hostByte){
		return this.hostByte == hostByte;
	}
	
	public byte getHostByte(){
		return hostByte;
	}
	//???? public static final byte max = 6; 
}
