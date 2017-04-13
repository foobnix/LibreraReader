package com.mobi.m2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class PDBHeader
{
	private byte[]	name				= { 0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0 };
	private	byte[]	attributes			= { 0, 0 };
	private byte[]	version				= { 0, 0 };
	private	byte[]	creationDate		= { 0, 0, 0, 0 };
	private	byte[]	modificationDate	= { 0, 0, 0, 0 };
	private	byte[]	lastBackupDate		= { 0, 0, 0, 0 };
	private	byte[]	modificationNumber	= { 0, 0, 0, 0 };
	private	byte[]	appInfoID			= { 0, 0, 0, 0 };
	private	byte[]	sortInfoID			= { 0, 0, 0, 0 };
	private	byte[]	type				= { 0, 0, 0, 0 };
	private	byte[]	creator				= { 0, 0, 0, 0 };
	private	byte[]	uniqueIDSeed		= { 0, 0, 0, 0 };
	private	byte[]	nextRecordListID	= { 0, 0, 0, 0 };
	private	byte[]	numRecords			= { 0, 0 };
    public List<RecordInfo> recordInfoList;
	private	byte[]	gapToData			= { 0, 0 };

	public PDBHeader(InputStream in)
		throws IOException
	{
		MobiCommon.logMessage("*** PDBHeader ***");
		StreamUtils.readByteArray(in, name);
		StreamUtils.readByteArray(in, attributes);
		StreamUtils.readByteArray(in, version);
		StreamUtils.readByteArray(in, creationDate);
		StreamUtils.readByteArray(in, modificationDate);
		StreamUtils.readByteArray(in, lastBackupDate);
		StreamUtils.readByteArray(in, modificationNumber);
		StreamUtils.readByteArray(in, appInfoID);
		StreamUtils.readByteArray(in, sortInfoID);
		StreamUtils.readByteArray(in, type);
		StreamUtils.readByteArray(in, creator);
		StreamUtils.readByteArray(in, uniqueIDSeed);
		StreamUtils.readByteArray(in, nextRecordListID);
		StreamUtils.readByteArray(in, numRecords);

		int recordCount = StreamUtils.byteArrayToInt(numRecords);
		MobiCommon.logMessage("numRecords: " + recordCount);
		recordInfoList = new LinkedList<RecordInfo>();
		for (int i=0; i<recordCount; i++)
		{
			recordInfoList.add(new RecordInfo(in));
		}

		StreamUtils.readByteArray(in, gapToData);
	}

	public long getMobiHeaderSize()
	{
		return (recordInfoList.size() > 1) ? (recordInfoList.get(1)
				.getRecordDataOffset() - recordInfoList.get(0)
				.getRecordDataOffset()) : 0;
	}
	
	public long getOffsetAfterMobiHeader()
	{
		return (recordInfoList.size() > 1) ? recordInfoList.get(1)
				.getRecordDataOffset() : 0;
	}
	
	public void adjustOffsetsAfterMobiHeader(int newMobiHeaderSize)
	{
		if (recordInfoList.size() < 2) return;

		int delta	= (int)(newMobiHeaderSize - getMobiHeaderSize());
		int len		= recordInfoList.size();
		for (int i=1; i<len; i++)
		{
			RecordInfo	rec			= recordInfoList.get(i);
			long		oldOffset	= rec.getRecordDataOffset();
			rec.setRecordDataOffset(oldOffset + delta);
		}
	}
	
	public void write(OutputStream out) throws IOException
	{
		out.write(name);
		out.write(attributes);
		out.write(version);
		out.write(creationDate);
		out.write(modificationDate);
		out.write(lastBackupDate);
		out.write(modificationNumber);
		out.write(appInfoID);
		out.write(sortInfoID);
		out.write(type);
		out.write(creator);
		out.write(uniqueIDSeed);
		out.write(nextRecordListID);
		out.write(numRecords);
		for (RecordInfo rec : recordInfoList) rec.write(out);
		out.write(gapToData);	
	}
	
	public String getName()
	{
		return StreamUtils.byteArrayToString(name);
	}
	
	public int getAttributes()
	{
		return StreamUtils.byteArrayToInt(attributes);
	}
	
	public int getVersion()
	{
		return StreamUtils.byteArrayToInt(version);
	}
	
	public long getCreationDate()
	{
		return StreamUtils.byteArrayToLong(creationDate);
	}

    public int getNumberOfRecord() {
        return StreamUtils.byteArrayToInt(numRecords);
    }
	
	public long getModificationDate()
	{
		return StreamUtils.byteArrayToLong(modificationDate);
	}
	
	public long getLastBackupDate()
	{
		return StreamUtils.byteArrayToLong(lastBackupDate);
	}
	
	public long getModificationNumber()
	{
		return StreamUtils.byteArrayToLong(modificationNumber);
	}
	
	public long getAppInfoID()
	{
		return StreamUtils.byteArrayToLong(appInfoID);
	}
	
	public long getSortInfoID()
	{
		return StreamUtils.byteArrayToLong(sortInfoID);
	}
	
	public long getType()
	{
		return StreamUtils.byteArrayToLong(type);
	}
	
	public long getCreator()
	{
		return StreamUtils.byteArrayToLong(creator);
	}
	
	public long getUniqueIDSeed()
	{
		return StreamUtils.byteArrayToLong(uniqueIDSeed);
	}
}
