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
package junrar.unpack;

import junrar.unpack.vm.VMPreparedProgram;

/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class UnpackFilter {

	private int BlockStart;

	private int BlockLength;

	private int ExecCount;

	private boolean NextWindow;

	// position of parent filter in Filters array used as prototype for filter
	// in PrgStack array. Not defined for filters in Filters array.
	private int ParentFilter;

	private VMPreparedProgram Prg  = new VMPreparedProgram();

	public int getBlockLength() {
		return BlockLength;
	}

	public void setBlockLength(int blockLength) {
		BlockLength = blockLength;
	}

	public int getBlockStart() {
		return BlockStart;
	}

	public void setBlockStart(int blockStart) {
		BlockStart = blockStart;
	}

	public int getExecCount() {
		return ExecCount;
	}

	public void setExecCount(int execCount) {
		ExecCount = execCount;
	}

	public boolean isNextWindow() {
		return NextWindow;
	}

	public void setNextWindow(boolean nextWindow) {
		NextWindow = nextWindow;
	}

	public int getParentFilter() {
		return ParentFilter;
	}

	public void setParentFilter(int parentFilter) {
		ParentFilter = parentFilter;
	}

	public VMPreparedProgram getPrg() {
		return Prg;
	}

	public void setPrg(VMPreparedProgram prg) {
		Prg = prg;
	}

	
	
}
