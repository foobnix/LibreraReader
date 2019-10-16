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
public enum VMCommands {
	VM_MOV(0), VM_CMP(1), VM_ADD(2), VM_SUB(3), VM_JZ(4), VM_JNZ(5), VM_INC(6), VM_DEC(
			7), VM_JMP(8), VM_XOR(9), VM_AND(10), VM_OR(11), VM_TEST(12), VM_JS(
			13), VM_JNS(14), VM_JB(15), VM_JBE(16), VM_JA(17), VM_JAE(18), VM_PUSH(
			19), VM_POP(20), VM_CALL(21), VM_RET(22), VM_NOT(23), VM_SHL(24), VM_SHR(
			25), VM_SAR(26), VM_NEG(27), VM_PUSHA(28), VM_POPA(29), VM_PUSHF(30), VM_POPF(
			31), VM_MOVZX(32), VM_MOVSX(33), VM_XCHG(34), VM_MUL(35), VM_DIV(36), VM_ADC(
			37), VM_SBB(38), VM_PRINT(39),

	// #ifdef VM_OPTIMIZE
	VM_MOVB(40), VM_MOVD(41), VM_CMPB(42), VM_CMPD(43),

	VM_ADDB(44), VM_ADDD(45), VM_SUBB(46), VM_SUBD(47), VM_INCB(48), VM_INCD(49), VM_DECB(
			50), VM_DECD(51), VM_NEGB(52), VM_NEGD(53),
	// #endif*/

	VM_STANDARD(54);

	private int vmCommand;

	private VMCommands(int vmCommand) {
		this.vmCommand = vmCommand;
	}

	public int getVMCommand() {
		return vmCommand;
	}

	public boolean equals(int vmCommand) {
		return this.vmCommand == vmCommand;
	}

	public static VMCommands findVMCommand(int vmCommand) {
		if (VM_MOV.equals(vmCommand)) {
			return VM_MOV;
		}
		if (VM_CMP.equals(vmCommand)) {
			return VM_CMP;
		}
		if (VM_ADD.equals(vmCommand)) {
			return VM_ADD;
		}
		if (VM_SUB.equals(vmCommand)) {
			return VM_SUB;
		}
		if (VM_JZ.equals(vmCommand)) {
			return VM_JZ;
		}
		if (VM_JNZ.equals(vmCommand)) {
			return VM_JNZ;
		}
		if (VM_INC.equals(vmCommand)) {
			return VM_INC;
		}
		if (VM_DEC.equals(vmCommand)) {
			return VM_DEC;
		}
		if (VM_JMP.equals(vmCommand)) {
			return VM_JMP;
		}
		if (VM_XOR.equals(vmCommand)) {
			return VM_XOR;
		}
		if (VM_AND.equals(vmCommand)) {
			return VM_AND;
		}
		if (VM_OR.equals(vmCommand)) {
			return VM_OR;
		}
		if (VM_TEST.equals(vmCommand)) {
			return VM_TEST;
		}
		if (VM_JS.equals(vmCommand)) {
			return VM_JS;
		}
		if (VM_JNS.equals(vmCommand)) {
			return VM_JNS;
		}
		if (VM_JB.equals(vmCommand)) {
			return VM_JB;
		}
		if (VM_JBE.equals(vmCommand)) {
			return VM_JBE;
		}
		if (VM_JA.equals(vmCommand)) {
			return VM_JA;
		}
		if (VM_JAE.equals(vmCommand)) {
			return VM_JAE;
		}
		if (VM_PUSH.equals(vmCommand)) {
			return VM_PUSH;
		}
		if (VM_POP.equals(vmCommand)) {
			return VM_POP;
		}
		if (VM_CALL.equals(vmCommand)) {
			return VM_CALL;
		}
		if (VM_RET.equals(vmCommand)) {
			return VM_RET;
		}
		if (VM_NOT.equals(vmCommand)) {
			return VM_NOT;
		}
		if (VM_SHL.equals(vmCommand)) {
			return VM_SHL;
		}
		if (VM_SHR.equals(vmCommand)) {
			return VM_SHR;
		}
		if (VM_SAR.equals(vmCommand)) {
			return VM_SAR;
		}
		if (VM_NEG.equals(vmCommand)) {
			return VM_NEG;
		}
		if (VM_PUSHA.equals(vmCommand)) {
			return VM_PUSHA;
		}
		if (VM_POPA.equals(vmCommand)) {
			return VM_POPA;
		}
		if (VM_PUSHF.equals(vmCommand)) {
			return VM_PUSHF;
		}
		if (VM_POPF.equals(vmCommand)) {
			return VM_POPF;
		}
		if (VM_MOVZX.equals(vmCommand)) {
			return VM_MOVZX;
		}
		if (VM_MOVSX.equals(vmCommand)) {
			return VM_MOVSX;
		}
		if (VM_XCHG.equals(vmCommand)) {
			return VM_XCHG;
		}
		if (VM_MUL.equals(vmCommand)) {
			return VM_MUL;
		}
		if (VM_DIV.equals(vmCommand)) {
			return VM_DIV;
		}
		if (VM_ADC.equals(vmCommand)) {
			return VM_ADC;
		}
		if (VM_SBB.equals(vmCommand)) {
			return VM_SBB;
		}
		if (VM_PRINT.equals(vmCommand)) {
			return VM_PRINT;
		}
		if (VM_MOVB.equals(vmCommand)) {
			return VM_MOVB;
		}
		if (VM_MOVD.equals(vmCommand)) {
			return VM_MOVD;
		}
		if (VM_CMPB.equals(vmCommand)) {
			return VM_CMPB;
		}
		if (VM_CMPD.equals(vmCommand)) {
			return VM_CMPD;
		}
		if (VM_ADDB.equals(vmCommand)) {
			return VM_ADDB;
		}
		if (VM_ADDD.equals(vmCommand)) {
			return VM_ADDD;
		}
		if (VM_SUBB.equals(vmCommand)) {
			return VM_SUBB;
		}
		if (VM_SUBD.equals(vmCommand)) {
			return VM_SUBD;
		}
		if (VM_INCB.equals(vmCommand)) {
			return VM_INCB;
		}
		if (VM_INCD.equals(vmCommand)) {
			return VM_INCD;
		}
		if (VM_DECB.equals(vmCommand)) {
			return VM_DECB;
		}
		if (VM_DECD.equals(vmCommand)) {
			return VM_DECD;
		}
		if (VM_NEGB.equals(vmCommand)) {
			return VM_NEGB;
		}
		if (VM_NEGD.equals(vmCommand)) {
			return VM_NEGD;
		}
		if (VM_STANDARD.equals(vmCommand)) {
			return VM_STANDARD;
		}
		return null;
	}
}
