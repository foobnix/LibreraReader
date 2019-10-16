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
public class VMPreparedCommand {
	private VMCommands OpCode;
	private boolean ByteMode;
	private VMPreparedOperand Op1 = new VMPreparedOperand();
	private VMPreparedOperand Op2 = new VMPreparedOperand();
	
	public boolean isByteMode() {
		return ByteMode;
	}
	public void setByteMode(boolean byteMode) {
		ByteMode = byteMode;
	}
	public VMPreparedOperand getOp1() {
		return Op1;
	}
	public void setOp1(VMPreparedOperand op1) {
		Op1 = op1;
	}
	public VMPreparedOperand getOp2() {
		return Op2;
	}
	public void setOp2(VMPreparedOperand op2) {
		Op2 = op2;
	}
	public VMCommands getOpCode() {
		return OpCode;
	}
	public void setOpCode(VMCommands opCode) {
		OpCode = opCode;
	}
	 
}
