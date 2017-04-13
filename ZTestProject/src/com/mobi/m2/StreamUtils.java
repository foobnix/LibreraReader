package com.mobi.m2;

import java.io.*;

public class StreamUtils
{
	public static String readCString(InputStream in, int len)
		throws IOException
	{
		byte[]	buffer		= new byte[len];
		int		bytesLeft	= len;
		int		offset		= 0;

		while (bytesLeft > 0)
		{
			int bytesRead = in.read(buffer, offset, bytesLeft);
			if (bytesRead == -1)
				throw new IOException("Supposed to read a "
									  + len
									  + " byte C string, but could not");
			offset		+= bytesRead;
			bytesLeft	-= bytesRead;
		}

		String s = byteArrayToString(buffer);
		MobiCommon.logMessage("readCString: " + s);
		return s;
	}

	public static byte readByte(InputStream in)
		throws IOException
	{
		int b = in.read();
		if (b == -1)
			throw new IOException("Supposed to read a byte, but could not");
		MobiCommon.logMessage("readByte: " + b);
		return (byte)(b & 0xff);
	}

	public static void readByteArray(InputStream in, byte[] buffer)
		throws IOException
	{
		int len			= buffer.length;
		int bytesLeft	= len;
		int offset		= 0;

		while (bytesLeft > 0)
		{
			int bytesRead = in.read(buffer, offset, bytesLeft);
			if (bytesRead == -1)
				throw new IOException("Supposed to read a "
									  + len
									  + " byte array, but could not");
			offset		+= bytesRead;
			bytesLeft	-= bytesRead;
		}

		if (MobiCommon.debug)
		{
			MobiCommon.logMessage(dumpByteArray(buffer));
		}
	}

	public static String byteArrayToString(byte[] buffer)
	{
		return byteArrayToString(buffer, null);
	}

	public static String byteArrayToString(byte[] buffer, String encoding)
	{
		int				len			= buffer.length;
		int				zeroIndex	= -1;
		for (int i=0; i<len; i++)
		{
			byte b = buffer[i];
			if (b == 0)
			{
				zeroIndex = i;
				break;
			}
		}

		if (encoding != null)
		{
			try
			{
				if (zeroIndex == -1)
					return new String(buffer, encoding);
				else
					return new String(buffer, 0, zeroIndex, encoding);
			}
			catch (java.io.UnsupportedEncodingException e)
			{
				// let it fall through and use the default encoding
			}
		}

		if (zeroIndex == -1)
			return new String(buffer);
		else
			return new String(buffer, 0, zeroIndex);
	}

	public static int byteArrayToInt(byte[] buffer)
	{
		int total	= 0;
		int len		= buffer.length;
		for (int i=0; i<len; i++)
		{
			total = (total << 8) + (buffer[i] & 0xff);
		}

		return total;
	}

	public static long byteArrayToLong(byte[] buffer)
	{
		long	total	= 0;
		int		len		= buffer.length;
		for (int i=0; i<len; i++)
		{
			total = (total << 8) + (buffer[i] & 0xff);
		}

		return total;
	}

	public static void intToByteArray(int value, byte[] dest)
	{
		int lastIndex = dest.length - 1;
		for (int i=lastIndex; i >=0; i--)
		{
			dest[i] = (byte)(value & 0xff);
			value = value >> 8;
		}
	}

	public static void longToByteArray(long value, byte[] dest)
	{
		int lastIndex = dest.length - 1;
		for (int i=lastIndex; i >=0; i--)
		{
			dest[i] = (byte)(value & 0xff);
			value = value >> 8;
		}
	}

	public static byte[] stringToByteArray(String s)
	{
		return stringToByteArray(s, null);
	}

	public static byte[] stringToByteArray(String s, String encoding)
	{
		if (encoding != null)
		{
			try
			{
				return s.getBytes(encoding);
			}
			catch (UnsupportedEncodingException e)
			{
				// let if fall through to use the default character encoding
			}
		}

		return s.getBytes();
	}
	
	public static String dumpByteArray(byte[] buffer)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("{ ");
		int len = buffer.length;
		for (int i=0; i<len; i++)
		{
			if (i > 0) sb.append(", ");
			sb.append(buffer[i] & 0xff);
		}
		sb.append(" }");
		return sb.toString();
	}
}
