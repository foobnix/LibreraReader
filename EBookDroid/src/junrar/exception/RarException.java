/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 30.07.2007
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
package junrar.exception;

/**
 * DOCUMENT ME
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class RarException extends Exception
{
	private static final long serialVersionUID = 1L;
	private RarExceptionType type;
	
	public RarException(Exception e){
		super(RarExceptionType.unkownError.name(),e);
		this.type = RarExceptionType.unkownError;
	}
	
	public RarException(RarException e)
	{
		
		super(e.getMessage(),e);
		this.type = e.getType();
	}
	
	public RarException(RarExceptionType type){
		super(type.name());
		this.type = type;
	}
	
	
	
	public enum RarExceptionType{
		notImplementedYet,
		crcError,
		notRarArchive,
		badRarArchive,
		unkownError,
		headerNotInArchive,
		wrongHeaderType,
		ioError,
		rarEncryptedException ;
	}



	public RarExceptionType getType()
	{
		return type;
	}

	public void setType(RarExceptionType type)
	{
		this.type = type;
	}
}
