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

import android.util.Log;

import junrar.io.Raw;


/**
 * The main header of an rar archive. holds information concerning the whole archive (solid, encrypted etc). 
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class MainHeader extends BaseBlock {
	private static String TAG = MainHeader.class.getName();
	public static final short mainHeaderSizeWithEnc = 7;
	public static final short mainHeaderSize = 6;
	private short highPosAv;
	private int posAv;
	private byte encryptVersion;
	
	public MainHeader(BaseBlock bb, byte[] mainHeader) {
		super(bb);
		int pos = 0;
		highPosAv = Raw.readShortLittleEndian(mainHeader, pos);
		pos += 2;
		posAv = Raw.readIntLittleEndian(mainHeader, pos);
		pos+=4;
		
		if(hasEncryptVersion()){
			encryptVersion |= mainHeader[pos]&0xff;
		}
	}
	
	/**
	 * old cmt block is present
	 * @return true if has cmt block
	 */
	public boolean hasArchCmt(){
		return (this.flags & BaseBlock.MHD_COMMENT)!=0;
	}
	/**
	 * the version the the encryption 
	 * @return
	 */
	public byte getEncryptVersion() {
		return encryptVersion;
	}
	
	public short getHighPosAv() {
		return highPosAv;
	}
	
	public int getPosAv() {
		return posAv;
	}
	
	/**
	 * returns whether the archive is encrypted 
	 * @return
	 */
	public boolean isEncrypted(){
		return (this.flags & BaseBlock.MHD_PASSWORD)!=0;
	}
	
	/**
	 * return whether the archive is a multivolume archive
	 * @return
	 */
	public boolean isMultiVolume(){
		return (this.flags & BaseBlock.MHD_VOLUME)!=0;
	}
	
	/**
	 * if the archive is a multivolume archive this method returns whether this instance is the first part of the multivolume archive
	 * @return
	 */
	public boolean isFirstVolume(){
		return (this.flags & BaseBlock.MHD_FIRSTVOLUME)!=0;
	}
	
	public void print(){
		super.print();
		StringBuilder str=new StringBuilder();
		str.append("posav: "+getPosAv());
		str.append("\nhighposav: "+getHighPosAv());
		str.append("\nhasencversion: "+hasEncryptVersion()+(hasEncryptVersion()?getEncryptVersion():""));
		str.append("\nhasarchcmt: "+hasArchCmt());
		str.append("\nisEncrypted: "+isEncrypted());
		str.append("\nisMultivolume: "+isMultiVolume());
		str.append("\nisFirstvolume: "+isFirstVolume());
		str.append("\nisSolid: "+isSolid());
		str.append("\nisLocked: "+isLocked());
		str.append("\nisProtected: "+isProtected());
		str.append("\nisAV: "+isAV());
		Log.i(TAG, str.toString());
	}
	
	/**
	 * returns whether this archive is solid. in this case you can only extract all file at once
	 * @return
	 */
	public boolean isSolid(){
		return (this.flags&MHD_SOLID)!=0;
	}
	
	public boolean isLocked(){
		return (this.flags&MHD_LOCK)!=0;
	}
	
	public boolean isProtected(){
		return (this.flags&MHD_PROTECT)!=0;
	}
	
	public boolean isAV(){
		return (this.flags&MHD_AV)!=0;
	}
	/**
	 * the numbering format a multivolume archive
	 * @return
	 */
	public boolean isNewNumbering(){
		return (this.flags&MHD_NEWNUMBERING)!=0;
	}
}
