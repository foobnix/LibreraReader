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
public class AudioVariables {
	int k1, k2, k3, k4, k5;

	int d1, d2, d3, d4;

	int lastDelta;

	int dif[] = new int[11];

	int byteCount;

	int lastChar;

	public int getByteCount() {
		return byteCount;
	}

	public void setByteCount(int byteCount) {
		this.byteCount = byteCount;
	}

	public int getD1() {
		return d1;
	}

	public void setD1(int d1) {
		this.d1 = d1;
	}

	public int getD2() {
		return d2;
	}

	public void setD2(int d2) {
		this.d2 = d2;
	}

	public int getD3() {
		return d3;
	}

	public void setD3(int d3) {
		this.d3 = d3;
	}

	public int getD4() {
		return d4;
	}

	public void setD4(int d4) {
		this.d4 = d4;
	}

	public int[] getDif() {
		return dif;
	}

	public void setDif(int[] dif) {
		this.dif = dif;
	}

	public int getK1() {
		return k1;
	}

	public void setK1(int k1) {
		this.k1 = k1;
	}

	public int getK2() {
		return k2;
	}

	public void setK2(int k2) {
		this.k2 = k2;
	}

	public int getK3() {
		return k3;
	}

	public void setK3(int k3) {
		this.k3 = k3;
	}

	public int getK4() {
		return k4;
	}

	public void setK4(int k4) {
		this.k4 = k4;
	}

	public int getK5() {
		return k5;
	}

	public void setK5(int k5) {
		this.k5 = k5;
	}

	public int getLastChar() {
		return lastChar;
	}

	public void setLastChar(int lastChar) {
		this.lastChar = lastChar;
	}

	public int getLastDelta() {
		return lastDelta;
	}

	public void setLastDelta(int lastDelta) {
		this.lastDelta = lastDelta;
	}

	
}
