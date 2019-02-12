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
public enum VMOpType {
	VM_OPREG (0),
	VM_OPINT (1),
	VM_OPREGMEM (2),
	VM_OPNONE (3);
	
	private int opType;
	
	private VMOpType(int opType){
		this.opType=opType;
	}

	public int getOpType() {
		return opType;
	}


	public boolean equals(int opType){
		return this.opType == opType;
	}
	public static VMOpType findOpType(int opType){
		
		if (VM_OPREG.equals(opType)) {
			return VM_OPREG;
		}		 
		
		
		if (VM_OPINT.equals(opType)) {
			return VM_OPINT;
		}		 
		
		if (VM_OPREGMEM.equals(opType)) {
			return VM_OPREGMEM;
		}		
		
		if (VM_OPNONE.equals(opType)) {
			return VM_OPNONE;
		}		 
		return null;
	}
}
