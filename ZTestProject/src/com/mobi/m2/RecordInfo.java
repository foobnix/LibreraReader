package com.mobi.m2;

import java.io.*;

public class RecordInfo
{
	private	byte[]	recordDataOffset	= { 0, 0, 0, 0 };
	private	byte	recordAttributes	= 0;
	private	byte[]	uniqueID			= { 0, 0, 0 };

	public RecordInfo(InputStream in)
		throws IOException
	{
		StreamUtils.readByteArray(in, recordDataOffset);
		recordAttributes = StreamUtils.readByte(in);
		StreamUtils.readByteArray(in, uniqueID);

		if (MobiCommon.debug)
		{
			MobiCommon.logMessage("RecordInfo uniqueID: "
							   + StreamUtils.byteArrayToInt(uniqueID));
		}
	}

	public long getRecordDataOffset()
	{
		return StreamUtils.byteArrayToLong(recordDataOffset);
	}
	
	public void setRecordDataOffset(long newOffset)
	{
		StreamUtils.longToByteArray(newOffset, recordDataOffset);
	}
	
	public void write(OutputStream out) throws IOException
	{
		out.write(recordDataOffset);
		out.write(recordAttributes);
		out.write(uniqueID);
	}
}
