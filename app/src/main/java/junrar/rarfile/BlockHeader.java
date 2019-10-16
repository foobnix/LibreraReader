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
 * Base class of headers that contain data
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class BlockHeader extends BaseBlock{
	public static final short blockHeaderSize = 4;
	
	private static String TAG = BlockHeader.class.getName();
	
	private long dataSize;
	private long packSize;
    
    public BlockHeader(){
    	
    }
    
    public BlockHeader(BlockHeader bh){
    	super(bh);
    	this.packSize = bh.getDataSize();
    	this.dataSize = packSize;
    	this.positionInFile = bh.getPositionInFile();
    }
    
    public BlockHeader(BaseBlock bb, byte[] blockHeader) 
    {
    	super(bb);
    	
    	this.packSize = Raw.readIntLittleEndianAsLong(blockHeader, 0);
    	this.dataSize  = this.packSize;
    }
    
	public long getDataSize() {
		return dataSize;
	}
	
	public long getPackSize() {
		return packSize;
	}
    
    public void print(){
    	super.print();
    	String s = "DataSize: "+getDataSize()+" packSize: "+getPackSize();
    	Log.i(TAG, s);
    }
}
