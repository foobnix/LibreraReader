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

import junrar.io.Raw;

/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class State extends Pointer {

	public static final int size = 6;

	public State(byte[] mem) {
		super(mem);
	}

    public State init(byte[] mem) {
		this.mem = mem;
        pos = 0;
        return this;
    }

	public int getSymbol() {
        return mem[pos]&0xff;
	}

	public void setSymbol(int symbol) {
        mem[pos] = (byte)symbol;
	}

	public int getFreq() {
        return mem[pos+1]&0xff;
	}

	public void setFreq(int freq) {
        mem[pos + 1] = (byte)freq;
	}

    public void incFreq(int dFreq) {
        mem[pos + 1] += dFreq;
    }
	
	public int getSuccessor() {
        return Raw.readIntLittleEndian(mem, pos+2);
	}

	public void setSuccessor(PPMContext successor) {
		setSuccessor(successor.getAddress());
	}

	public void setSuccessor(int successor) {
        Raw.writeIntLittleEndian(mem, pos + 2, successor);
	}

	public void setValues(StateRef state){
		setSymbol(state.getSymbol());
		setFreq(state.getFreq());
		setSuccessor(state.getSuccessor());
	}

	public void setValues(State ptr){
        System.arraycopy(ptr.mem, ptr.pos, mem, pos, size);
	}

	public State decAddress(){
		setAddress(pos-size);
		return this;
	}

    public State incAddress(){
		setAddress(pos+size);
		return this;
	}

    public static void ppmdSwap(State ptr1, State ptr2) {
        byte[] mem1=ptr1.mem, mem2=ptr2.mem;
		for (int i=0, pos1=ptr1.pos, pos2=ptr2.pos; i < size; i++, pos1++, pos2++) {
			byte temp = mem1[pos1];
			mem1[pos1] = mem2[pos2];
			mem2[pos2] = temp;
		}
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("State[");
        buffer.append("\n  pos=");
        buffer.append(pos);
        buffer.append("\n  size=");
        buffer.append(size);
        buffer.append("\n  symbol=");
        buffer.append(getSymbol());
        buffer.append("\n  freq=");
        buffer.append(getFreq());
        buffer.append("\n  successor=");
        buffer.append(getSuccessor());
        buffer.append("\n]");
        return buffer.toString();
    }
}
