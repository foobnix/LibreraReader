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
package junrar.unpack.vm;

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class VMCmdFlags {
	public static final byte VMCF_OP0		= 0;
	public static final byte VMCF_OP1		= 1;
	public static final byte VMCF_OP2		= 2;
	public static final byte VMCF_OPMASK	= 3;
	public static final byte VMCF_BYTEMODE	= 4;
	public static final byte VMCF_JUMP     = 8;
	public static final byte VMCF_PROC     = 16;
	public static final byte VMCF_USEFLAGS = 32;
	public static final byte VMCF_CHFLAGS  = 64;

	public static byte VM_CmdFlags[]=
	{
	  /* VM_MOV   */ VMCF_OP2 | VMCF_BYTEMODE                                ,
	  /* VM_CMP   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_ADD   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_SUB   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_JZ    */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS                    ,
	  /* VM_JNZ   */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS                    ,
	  /* VM_INC   */ VMCF_OP1 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_DEC   */ VMCF_OP1 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_JMP   */ VMCF_OP1 | VMCF_JUMP                                    ,
	  /* VM_XOR   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_AND   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_OR    */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_TEST  */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_JS    */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS                    ,
	  /* VM_JNS   */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS                    ,
	  /* VM_JB    */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS                    ,
	  /* VM_JBE   */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS                    ,
	  /* VM_JA    */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS                    ,
	  /* VM_JAE   */ VMCF_OP1 | VMCF_JUMP | VMCF_USEFLAGS                    ,
	  /* VM_PUSH  */ VMCF_OP1                                                ,
	  /* VM_POP   */ VMCF_OP1                                                ,
	  /* VM_CALL  */ VMCF_OP1 | VMCF_PROC                                    ,
	  /* VM_RET   */ VMCF_OP0 | VMCF_PROC                                    ,
	  /* VM_NOT   */ VMCF_OP1 | VMCF_BYTEMODE                                ,
	  /* VM_SHL   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_SHR   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_SAR   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_NEG   */ VMCF_OP1 | VMCF_BYTEMODE | VMCF_CHFLAGS                 ,
	  /* VM_PUSHA */ VMCF_OP0                                                ,
	  /* VM_POPA  */ VMCF_OP0                                                ,
	  /* VM_PUSHF */ VMCF_OP0 | VMCF_USEFLAGS                                ,
	  /* VM_POPF  */ VMCF_OP0 | VMCF_CHFLAGS                                 ,
	  /* VM_MOVZX */ VMCF_OP2                                                ,
	  /* VM_MOVSX */ VMCF_OP2                                                ,
	  /* VM_XCHG  */ VMCF_OP2 | VMCF_BYTEMODE                                ,
	  /* VM_MUL   */ VMCF_OP2 | VMCF_BYTEMODE                                ,
	  /* VM_DIV   */ VMCF_OP2 | VMCF_BYTEMODE                                ,
	  /* VM_ADC   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_USEFLAGS | VMCF_CHFLAGS ,
	  /* VM_SBB   */ VMCF_OP2 | VMCF_BYTEMODE | VMCF_USEFLAGS | VMCF_CHFLAGS ,
	  /* VM_PRINT */ VMCF_OP0
	};

}
