package com.mobi.m2;

public class MobiCommon
{
	public static boolean	debug		= false;

	// safe mode avoids changing the size of the mobi header
	//
	public static boolean	safeMode	= false;
	
	public static void logMessage(String message)
	{
		if (debug) System.out.println(message);
	}
}
