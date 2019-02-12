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
package junrar.unpack.ppm;

/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class SEE2Context {
	public static final int size = 4;
	
    // ushort Summ;
	private int summ;

    // byte Shift;
	private int shift;

    // byte Count;
	private int count;

	public void init(int initVal) {
		shift = (ModelPPM.PERIOD_BITS - 4)&0xff;
		summ = (initVal << shift)&0xffff;
		count = 4;
	}

    public int getMean() {
        int retVal = summ >>> shift;
		summ -= retVal;
		return retVal + ((retVal == 0) ? 1 : 0);
	}

	public void update() {
		if (shift < ModelPPM.PERIOD_BITS && --count == 0) {
			summ += summ;
			count = (3 << shift++);
		}
        summ &= 0xffff;
        count &= 0xff;
        shift &= 0xff;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count&0xff;
	}

	public int getShift() {
		return shift;
	}

	public void setShift(int shift) {
		this.shift = shift&0xff;
	}

	public int getSumm() {
		return summ;
	}

	public void setSumm(int summ) {
		this.summ = summ&0xffff;
	}

    public void incSumm(int dSumm) {
        setSumm(getSumm() + dSumm);
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("SEE2Context[");
        buffer.append("\n  size=");
        buffer.append(size);
        buffer.append("\n  summ=");
        buffer.append(summ);
        buffer.append("\n  shift=");
        buffer.append(shift);
        buffer.append("\n  count=");
        buffer.append(count);
        buffer.append("\n]");
        return buffer.toString();
    }
}
