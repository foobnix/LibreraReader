/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 05.06.2007
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

import junrar.io.Raw;



/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class RarMemBlock extends Pointer
{

    public static final int size = 12;

	private int stamp, NU;

	private int next, prev; // Pointer RarMemBlock

	public RarMemBlock(byte[] mem)
	{
		super(mem);
	}

    public void insertAt(RarMemBlock p)
	{
		RarMemBlock temp = new RarMemBlock(mem);
		setPrev(p.getAddress());
		temp.setAddress(getPrev());
		setNext(temp.getNext());// prev.getNext();
		temp.setNext(this);// prev.setNext(this);
		temp.setAddress(getNext());
		temp.setPrev(this);// next.setPrev(this);
	}

	public void remove()
	{
		RarMemBlock temp = new RarMemBlock(mem);
		temp.setAddress(getPrev());
		temp.setNext(getNext());// prev.setNext(next);
		temp.setAddress(getNext());
		temp.setPrev(getPrev());// next.setPrev(prev);
//		next = -1;
//		prev = -1;
	}

	public int getNext()
	{
		if(mem!=null){
			next = Raw.readIntLittleEndian(mem,  pos+4);
		}
		return next;
	}

	public void setNext(RarMemBlock next)
	{
		setNext(next.getAddress());
	}

	public void setNext(int next)
	{
		this.next = next;
		if (mem != null) {
			Raw.writeIntLittleEndian(mem, pos + 4, next);
		}
	}

	public int getNU()
	{
		if(mem!=null){
			NU = Raw.readShortLittleEndian(mem,  pos+2)&0xffff;
		}
		return NU;
	}

	public void setNU(int nu)
	{
		NU = nu&0xffff;
		if (mem != null) {
			Raw.writeShortLittleEndian(mem, pos + 2, (short)nu);
		}
	}

	public int getPrev()
	{
		if(mem!=null){
			prev = Raw.readIntLittleEndian(mem,  pos+8);
		}
		return prev;
	}

	public void setPrev(RarMemBlock prev)
	{
		setPrev(prev.getAddress());
	}

	public void setPrev(int prev)
	{
		this.prev = prev;
		if (mem != null) {
			Raw.writeIntLittleEndian(mem, pos + 8, prev);
		}
	}

	public int getStamp()
	{
		if(mem!=null){
			stamp =  Raw.readShortLittleEndian(mem,  pos)&0xffff;
		}
		return stamp;
	}

	public void setStamp(int stamp)
	{
		this.stamp = stamp;
		if (mem != null) {
			Raw.writeShortLittleEndian(mem, pos, (short)stamp);
		}
	}
}
