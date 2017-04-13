package com.mobi.m2;

import java.io.*;
import java.util.*;

public class EXTHHeader
{
	private byte[]	identifier		= { 69, 88, 84, 72 };
	private byte[]	headerLength	= { 0, 0, 0, 0 };
	private byte[]	recordCount		= { 0, 0, 0, 0 };
	private List<EXTHRecord> recordList = null;

	public EXTHHeader()
	{
		recordList = new LinkedList<EXTHRecord>();
	}

	public EXTHHeader(List<EXTHRecord> list)
	{
		setRecordList(list);
	}

	public EXTHHeader(InputStream in) throws IOException
	{
		MobiCommon.logMessage("*** EXTHHeader ***");

		StreamUtils.readByteArray(in, identifier);
		if ((identifier[0] != 69)
			||
			(identifier[1] != 88)
			||
			(identifier[2] != 84)
			||
			(identifier[3] != 72))
		{
			throw new IOException("Expected to find EXTH header identifier"
								  + " EXTH but got something else instead");
		}

		StreamUtils.readByteArray(in, headerLength);
		
		if (MobiCommon.debug)
		{
			MobiCommon.logMessage("EXTH header length: "
					+ StreamUtils.byteArrayToLong(headerLength));
		}

		StreamUtils.readByteArray(in, recordCount);
		int count = StreamUtils.byteArrayToInt(recordCount);
		MobiCommon.logMessage("EXTH record count: " + count);

		recordList = new LinkedList<EXTHRecord>();
		for (int i=0; i<count; i++)
		{
			recordList.add(new EXTHRecord(in));
		}

		int padding = paddingSize(dataSize());
		MobiCommon.logMessage("padding size: " + padding);
		for (int i=0; i<padding; i++) StreamUtils.readByte(in);
	}

	public int size()
	{
		int dataSize = dataSize();
		return 12 + dataSize + paddingSize(dataSize);
	}

	public void recomputeFields()
	{
		StreamUtils.intToByteArray(size(), headerLength);
		StreamUtils.intToByteArray(recordList.size(), recordCount);
	}

	public List<EXTHRecord> getRecordList()
	{
		LinkedList<EXTHRecord> list = new LinkedList<EXTHRecord>();
		for (EXTHRecord rec : recordList)
		{
			list.add(rec.copy());
		}

		return list;
	}

	public void setRecordList(List<EXTHRecord> list)
	{
		recordList = new LinkedList<EXTHRecord>();
		if (list != null)
		{
			for (EXTHRecord rec : list)
			{
				recordList.add(rec.copy());
			}
		}
		recomputeFields();
	}

	public void removeRecordsWithType(int type)
	{
		boolean changed = false;
		for (EXTHRecord rec : recordList)
		{
			if (rec.getRecordType() == type)
			{
				recordList.remove(rec);
				changed = true;
			}
		}

		if (changed) recomputeFields();
	}

	public boolean recordsWithTypeExist(int type)
	{
		for (EXTHRecord rec : recordList)
		{
			if (rec.getRecordType() == type) return true;
		}
		return false;
	}

	public void setAllRecordsWithTypeToString(int		type,
											  String	s,
											  String	encoding)
	{
		boolean changed = false;
		for (EXTHRecord rec : recordList)
		{
			if (rec.getRecordType() == type)
			{
				rec.setData(s, encoding);
				changed = true;
			}
		}

		if (changed) recomputeFields();
	}

	public void addRecord(int recType, String s, String encoding)
	{
		EXTHRecord rec = new EXTHRecord
						(recType, StreamUtils.stringToByteArray(s, encoding));
		recordList.add(rec);
		recomputeFields();
	}

	public void addRecord(int recType, byte[] buffer)
	{
		recordList.add(new EXTHRecord(recType, buffer));
		recomputeFields();
	}

	protected int dataSize()
	{
		int size = 0;
		for (EXTHRecord rec : recordList)
		{
			size += rec.size();
		}

		return size;
	}

	protected int paddingSize(int dataSize)
	{
		int paddingSize = dataSize % 4;
		if (paddingSize != 0) paddingSize = 4 - paddingSize;

		return paddingSize;
	}
	
	public void write(OutputStream out) throws IOException
	{
		out.write(identifier);
		out.write(headerLength);
		out.write(recordCount);
		for (EXTHRecord rec : recordList)
		{
			rec.write(out);
		}
		int padding = paddingSize(dataSize());
		for (int i=0; i<padding; i++) out.write(0);
	}
}
