package com.mobi.m2;

import java.io.*;
import java.util.*;

public class MobiHeader
{
	private byte[]	compression			= { 0, 0 };
	private byte[]	unused0				= { 0, 0 };
	private byte[]	textLength			= { 0, 0, 0, 0 };
	private byte[]	recordCount			= { 0, 0 };
	private byte[]	recordSize			= { 0, 0 };
	private byte[]	encryptionType		= { 0, 0 };
	private byte[]	unused1				= { 0, 0 };
	private byte[]	identifier			= { 0, 0, 0, 0 };
	private byte[]	headerLength		= { 0, 0, 0, 0 };	// from offset 0x10
	private byte[]	mobiType			= { 0, 0, 0, 0 };
	private byte[]	textEncoding		= { 0, 0, 0, 0 };
	private byte[]	uniqueID			= { 0, 0, 0, 0 };
	private byte[]	fileVersion			= { 0, 0, 0, 0 };
	private byte[]	orthographicIndex	= { 0, 0, 0, 0 };
	private byte[]	inflectionIndex		= { 0, 0, 0, 0 };
	private byte[]	indexNames			= { 0, 0, 0, 0 };
	private byte[]	indexKeys			= { 0, 0, 0, 0 };
	private byte[]	extraIndex0			= { 0, 0, 0, 0 };
	private byte[]	extraIndex1			= { 0, 0, 0, 0 };
	private byte[]	extraIndex2			= { 0, 0, 0, 0 };
	private byte[]	extraIndex3			= { 0, 0, 0, 0 };
	private byte[]	extraIndex4			= { 0, 0, 0, 0 };
	private byte[]	extraIndex5			= { 0, 0, 0, 0 };
	private byte[]	firstNonBookIndex	= { 0, 0, 0, 0 };
	private byte[]	fullNameOffset		= { 0, 0, 0, 0 };
	private byte[]	fullNameLength		= { 0, 0, 0, 0 };
	private byte[]	locale				= { 0, 0, 0, 0 };
	private byte[]	inputLanguage		= { 0, 0, 0, 0 };
	private byte[]	outputLanguage		= { 0, 0, 0, 0 };
	private byte[]	minVersion			= { 0, 0, 0, 0 };
	private byte[]	firstImageIndex		= { 0, 0, 0, 0 };
	private byte[]	huffmanRecordOffset	= { 0, 0, 0, 0 };
	private byte[]	huffmanRecordCount	= { 0, 0, 0, 0 };
	private byte[]	huffmanTableOffset	= { 0, 0, 0, 0 };
	private byte[]	huffmanTableLength	= { 0, 0, 0, 0 };
	private byte[]	exthFlags			= { 0, 0, 0, 0 };
	private	byte[]	restOfMobiHeader	= null;
	private EXTHHeader exthHeader		= null;
	private byte[]	remainder			= null;
	// end of useful data


	private byte[]	fullName			= null;
	private	String	characterEncoding	= null;

	public MobiHeader(InputStream in, long mobiHeaderSize) throws IOException
	{
		MobiCommon.logMessage("*** MobiHeader ***");
		MobiCommon.logMessage("compression");
		StreamUtils.readByteArray(in, compression);
		StreamUtils.readByteArray(in, unused0);
		StreamUtils.readByteArray(in, textLength);
		StreamUtils.readByteArray(in, recordCount);
		StreamUtils.readByteArray(in, recordSize);
		MobiCommon.logMessage("encryptionType");
		StreamUtils.readByteArray(in, encryptionType);
		StreamUtils.readByteArray(in, unused1);

		StreamUtils.readByteArray(in, identifier);
		if (MobiCommon.debug)
		{
			MobiCommon.logMessage("identifier: "
					+ StreamUtils.byteArrayToString(identifier));
		}
		if ((identifier[0] != 77)
			||
			(identifier[1] != 79)
			||
			(identifier[2] != 66)
			||
			(identifier[3] != 73))
		{
			throw new IOException("Did not get expected MOBI identifier");
		}

		// this value will determine the size of restOfMobiHeader[]
		//
		StreamUtils.readByteArray(in, headerLength);
		int headLen = StreamUtils.byteArrayToInt(headerLength);
		restOfMobiHeader = new byte[headLen + 16 - 132];
		if (MobiCommon.debug)
		{
			MobiCommon.logMessage("headerLength: " + headLen);
		}

		StreamUtils.readByteArray(in, mobiType);
		if (MobiCommon.debug)
		{
			MobiCommon.logMessage("mobiType: "
					+ StreamUtils.byteArrayToInt(mobiType));
		}

		StreamUtils.readByteArray(in, textEncoding);
		switch (StreamUtils.byteArrayToInt(textEncoding))
		{
			case 1252:
				characterEncoding = "Cp1252";
				break;
			case 65001:
				characterEncoding = "UTF-8";
				break;
			default:
				characterEncoding = null;
				break;
		}
		MobiCommon.logMessage("text encoding: " + characterEncoding);

		StreamUtils.readByteArray(in, uniqueID);
		StreamUtils.readByteArray(in, fileVersion);
		StreamUtils.readByteArray(in, orthographicIndex);
		StreamUtils.readByteArray(in, inflectionIndex);
		StreamUtils.readByteArray(in, indexNames);
		StreamUtils.readByteArray(in, indexKeys);
		StreamUtils.readByteArray(in, extraIndex0);
		StreamUtils.readByteArray(in, extraIndex1);
		StreamUtils.readByteArray(in, extraIndex2);
		StreamUtils.readByteArray(in, extraIndex3);
		StreamUtils.readByteArray(in, extraIndex4);
		StreamUtils.readByteArray(in, extraIndex5);
		StreamUtils.readByteArray(in, firstNonBookIndex);
		StreamUtils.readByteArray(in, fullNameOffset);
		if (MobiCommon.debug)
		{
			MobiCommon.logMessage("full name offset: "
					+ StreamUtils.byteArrayToInt(fullNameOffset));
		}

		StreamUtils.readByteArray(in, fullNameLength);
		int fullNameLen = StreamUtils.byteArrayToInt(fullNameLength);
		MobiCommon.logMessage("full name length: " + fullNameLen);
		StreamUtils.readByteArray(in, locale);
		StreamUtils.readByteArray(in, inputLanguage);
		StreamUtils.readByteArray(in, outputLanguage);
		StreamUtils.readByteArray(in, minVersion);
		StreamUtils.readByteArray(in, firstImageIndex);
		StreamUtils.readByteArray(in, huffmanRecordOffset);
		StreamUtils.readByteArray(in, huffmanRecordCount);
		StreamUtils.readByteArray(in, huffmanTableOffset);
		StreamUtils.readByteArray(in, huffmanTableLength);
		StreamUtils.readByteArray(in, exthFlags);
		if (MobiCommon.debug)
		{
			MobiCommon.logMessage("exthFlags: "
					+ StreamUtils.byteArrayToInt(exthFlags));
		}
		boolean exthExists = ((StreamUtils.byteArrayToInt(exthFlags) & 0x40)
							  != 0);
		MobiCommon.logMessage("exthExists: " + exthExists);
		StreamUtils.readByteArray(in, restOfMobiHeader);

		if (exthExists)
		{
			exthHeader = new EXTHHeader(in);
		}

		int currentOffset = 132 + restOfMobiHeader.length + exthHeaderSize();

		remainder = new byte[(int)(mobiHeaderSize - currentOffset)];
		StreamUtils.readByteArray(in, remainder);

		int fullNameIndexInRemainder
				= StreamUtils.byteArrayToInt(fullNameOffset) - currentOffset;
		fullName = new byte[fullNameLen];
		MobiCommon.logMessage("fullNameIndexInRemainder: "
							   + fullNameIndexInRemainder);
		MobiCommon.logMessage("fullNameLen: " + fullNameLen);

		if ((fullNameIndexInRemainder >= 0)
			&&
			(fullNameIndexInRemainder < remainder.length)
			&&
			((fullNameIndexInRemainder + fullNameLen) <= remainder.length)
			&&
			(fullNameLen > 0))
		{
			System.arraycopy(remainder,
							 fullNameIndexInRemainder,
							 fullName,
							 0,
							 fullNameLen);
		}
		if (MobiCommon.debug)
		{
			MobiCommon.logMessage("full name: "
					+ StreamUtils.byteArrayToString(fullName));
		}
	}
	
	public String getCharacterEncoding()
	{
		return characterEncoding;
	}
	
	public String getFullName()
	{
		return StreamUtils.byteArrayToString(fullName, characterEncoding);
	}

	public void setFullName(String s)
	{
		byte[] fullBytes = StreamUtils.stringToByteArray(s, characterEncoding);
		int len		= fullBytes.length;
		StreamUtils.intToByteArray(len, fullNameLength);

		// the string must be terminated by 2 null bytes
		// then this must end in a 4-byte boundary
		//
		int padding	= (len + 2) % 4;
		if (padding != 0) padding = 4 - padding;
		padding += 2;

		byte[] buffer = new byte[len + padding];
		System.arraycopy(fullBytes, 0, buffer, 0, len);
		for (int i=len; i<buffer.length; i++) buffer[i] = 0;

		fullName = buffer;
	}
	
	public int getLocale()
	{
		return StreamUtils.byteArrayToInt(locale);
	}

	public void setLocale(int localeInt)
	{
		StreamUtils.intToByteArray(localeInt, locale);
	}

	public int getInputLanguage()
	{
		return StreamUtils.byteArrayToInt(inputLanguage);
	}

	public void setInputLanguage(int input)
	{
		StreamUtils.intToByteArray(input, inputLanguage);
	}

	public int getOutputLanguage()
	{
		return StreamUtils.byteArrayToInt(outputLanguage);
	}

	public void setOutputLanguage(int output)
	{
		StreamUtils.intToByteArray(output, outputLanguage);
	}

	public List<EXTHRecord> getEXTHRecords()
	{
		return (exthHeader == null) ? (new LinkedList<EXTHRecord>())
				: exthHeader.getRecordList();
	}

	public void setEXTHRecords(List<EXTHRecord> list)
	{
		int flag = StreamUtils.byteArrayToInt(exthFlags) & 0xffffbf;
		if ((list == null) || (list.size() == 0))
		{
			exthHeader = null;
			StreamUtils.intToByteArray(flag, exthFlags);
		}
		else
		{
			if (exthHeader == null)
				exthHeader = new EXTHHeader(list);
			else
				exthHeader.setRecordList(list);

			StreamUtils.intToByteArray(flag | 0x40, exthFlags);
		}
	}
	
	public void pack()
	{
		if (!MobiCommon.safeMode)
		{
			// dump existing remainder, set to fullName
			remainder = new byte[fullName.length];
			System.arraycopy(fullName, 0, remainder, 0, remainder.length);

			// adjust fullNameOffset
			StreamUtils.intToByteArray(132 + restOfMobiHeader.length
					+ exthHeaderSize(), fullNameOffset);
		}
	}
	
	public int size()
	{
		return 132 + restOfMobiHeader.length + exthHeaderSize() + remainder.length;
	}
	
	public String getCompression()
	{
		int comp = StreamUtils.byteArrayToInt(compression);
		switch (comp)
		{
			case 1:
				return "None";
			case 2:
				return "PalmDOC";
			case 17480:
				return "HUFF/CDIC";
			default:
				return "Unknown (" + comp + ")";
		}
	}
	
	public long getTextLength()
	{
		return StreamUtils.byteArrayToLong(textLength);
	}
	
	public int getRecordCount()
	{
		return StreamUtils.byteArrayToInt(recordCount);
	}
	
	public int getRecordSize()
	{
		return StreamUtils.byteArrayToInt(recordSize);
	}
	
	public String getEncryptionType()
	{
		int enc = StreamUtils.byteArrayToInt(encryptionType);
		switch (enc)
		{
			case 0: return "None";
			case 1: return "Old Mobipocket";
			case 2: return "Mobipocket";
			default: return "Unknown (" + enc + ")";
		}
	}
	
	public long getHeaderLength()
	{
		return StreamUtils.byteArrayToLong(headerLength);
	}
	
	public String getMobiType()
	{
		long type = StreamUtils.byteArrayToLong(mobiType);
		if (type == 2)
			return "Mobipocket Book";
		else if (type == 3)
			return "PalmDoc Book";
		else if (type == 4)
			return "Audio";
		else if (type == 257)
			return "News";
		else if (type == 258)
			return "News Feed";
		else if (type == 259)
			return "News Magazine";
		else if (type == 513)
			return "PICS";
		else if (type == 514)
			return "WORD";
		else if (type == 515)
			return "XLS";
		else if (type == 516)
			return "PPT";
		else if (type == 517)
			return "TEXT";
		else if (type == 518)
			return "HTML";
		else
			return "Unknown (" + type + ")";
	}
	
	public long getUniqueID()
	{
		return StreamUtils.byteArrayToLong(uniqueID);
	}
	
	public long getFileVersion()
	{
		return StreamUtils.byteArrayToLong(fileVersion);
	}
	
	public long getOrthographicIndex()
	{
		return StreamUtils.byteArrayToLong(orthographicIndex);
	}
	
	public long getInflectionIndex()
	{
		return StreamUtils.byteArrayToLong(inflectionIndex);
	}
	
	public long getIndexNames()
	{
		return StreamUtils.byteArrayToLong(indexNames);
	}
	
	public long getIndexKeys()
	{
		return StreamUtils.byteArrayToLong(indexKeys);
	}
	
	public long getExtraIndex0()
	{
		return StreamUtils.byteArrayToLong(extraIndex0);
	}
	
	public long getExtraIndex1()
	{
		return StreamUtils.byteArrayToLong(extraIndex1);
	}
	
	public long getExtraIndex2()
	{
		return StreamUtils.byteArrayToLong(extraIndex2);
	}
	
	public long getExtraIndex3()
	{
		return StreamUtils.byteArrayToLong(extraIndex3);
	}
	
	public long getExtraIndex4()
	{
		return StreamUtils.byteArrayToLong(extraIndex4);
	}
	
	public long getExtraIndex5()
	{
		return StreamUtils.byteArrayToLong(extraIndex5);
	}
	
	public long getFirstNonBookIndex()
	{
		return StreamUtils.byteArrayToLong(firstNonBookIndex);
	}
	
	public long getFullNameOffset()
	{
		return StreamUtils.byteArrayToLong(fullNameOffset);
	}
	
	public long getFullNameLength()
	{
		return StreamUtils.byteArrayToLong(fullNameLength);
	}
	
	public long getMinVersion()
	{
		return StreamUtils.byteArrayToLong(minVersion);
	}
	
	public long getHuffmanRecordOffset()
	{
		return StreamUtils.byteArrayToLong(huffmanRecordOffset);
	}
	
	public long getHuffmanRecordCount()
	{
		return StreamUtils.byteArrayToLong(huffmanRecordCount);
	}
	
	public long getHuffmanTableOffset()
	{
		return StreamUtils.byteArrayToLong(huffmanTableOffset);
	}
	
	public long getHuffmanTableLength()
	{
		return StreamUtils.byteArrayToLong(huffmanTableLength);
	}
	
	private int exthHeaderSize()
	{
		return (exthHeader == null)?0:exthHeader.size();
	}
	
	public void write(OutputStream out) throws IOException
	{
		out.write(compression);
		out.write(unused0);
		out.write(textLength);
		out.write(recordCount);
		out.write(recordSize);
		out.write(encryptionType);
		out.write(unused1);
		out.write(identifier);
		out.write(headerLength);
		out.write(mobiType);
		out.write(textEncoding);
		out.write(uniqueID);
		out.write(fileVersion);
		out.write(orthographicIndex);
		out.write(inflectionIndex);
		out.write(indexNames);
		out.write(indexKeys);
		out.write(extraIndex0);
		out.write(extraIndex1);
		out.write(extraIndex2);
		out.write(extraIndex3);
		out.write(extraIndex4);
		out.write(extraIndex5);
		out.write(firstNonBookIndex);
		out.write(fullNameOffset);
		out.write(fullNameLength);
		out.write(locale);
		out.write(inputLanguage);
		out.write(outputLanguage);
		out.write(minVersion);
		out.write(firstImageIndex);
		out.write(huffmanRecordOffset);
		out.write(huffmanRecordCount);
		out.write(huffmanTableOffset);
		out.write(huffmanTableLength);
		out.write(exthFlags);
		out.write(restOfMobiHeader);
		if (exthHeader != null) exthHeader.write(out);
		out.write(remainder);
	}

    public EXTHHeader getExthHeader() {
        return exthHeader;
    }

    public void setExthHeader(EXTHHeader exthHeader) {
        this.exthHeader = exthHeader;
    }
}
