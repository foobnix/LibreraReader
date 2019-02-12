/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 31.05.2007
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
package junrar.unpack.vm;

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class BitInput {
	/**
	 * the max size of the input
	 */
	public static final int MAX_SIZE = 0x8000;
	protected int inAddr;
	protected int inBit;
	protected byte[] inBuf;
	
	/**
	 * 
	 */
	public void InitBitInput()
    {
      inAddr=0;
      inBit=0;
    }
    /**
     * @param Bits 
     */
    public void addbits(int Bits)
    {
      Bits+=inBit;
      inAddr+=Bits>>3;
      inBit=Bits&7;
    }
    
    /**
     * @return the bits (unsigned short)
     */
    public int getbits()
    {
//      int BitField=0;
//      BitField|=(int)(inBuf[inAddr] << 16)&0xFF0000;
//      BitField|=(int)(inBuf[inAddr+1] << 8)&0xff00;
//      BitField|=(int)(inBuf[inAddr+2])&0xFF;
//      BitField >>>= (8-inBit);
//      return (BitField & 0xffff);
      return (((((inBuf[inAddr] & 0xff) << 16) +
              ((inBuf[inAddr+1] & 0xff) << 8) +
              ((inBuf[inAddr+2] & 0xff))) >>> (8-inBit)) & 0xffff);
    }

    /**
     *  
     */
    public BitInput()
    {
      inBuf=new byte[MAX_SIZE];
    }

    /**
     * @param Bits add the bits
     */
    public void faddbits(int Bits)
    {
      addbits(Bits);
    }


    /**
     * @return get the bits
     */
    public int fgetbits()
    {
      return(getbits());
    }
    
    /**
     * Indicates an Overfow
     * @param IncPtr how many bytes to inc
     * @return true if an Oververflow would occur
     */
    public boolean Overflow(int IncPtr) {
    	return(inAddr+IncPtr>=MAX_SIZE);
    }
	public byte[] getInBuf()
	{
		return inBuf;
	}
    
    
}
