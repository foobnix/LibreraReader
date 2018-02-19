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


/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class StateRef {

	private int symbol;

	private int freq;

	private int successor; // pointer ppmcontext

	public StateRef() {
	}

	public int getSymbol() {
		return symbol;
	}

	public void setSymbol(int symbol) {
		this.symbol = symbol&0xff;
	}

	public int getFreq() {
		return freq;
	}

	public void setFreq(int freq) {
		this.freq = freq&0xff;
	}

    public void incFreq(int dFreq) {
        freq = (freq + dFreq)&0xff;
    }

    public void decFreq(int dFreq) {
        freq = (freq - dFreq)&0xff;
    }

	public void setValues(State statePtr){
		setFreq(statePtr.getFreq());
		setSuccessor(statePtr.getSuccessor());
		setSymbol(statePtr.getSymbol());
	}
	
	public int getSuccessor() {
		return successor;
	}

	public void setSuccessor(PPMContext successor) {
		setSuccessor(successor.getAddress());
	}

	public void setSuccessor(int successor) {
		this.successor = successor;
	}

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("State[");
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
