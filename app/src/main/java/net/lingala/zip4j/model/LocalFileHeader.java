/*
* Copyright 2010 Srikanth Reddy Lingala  
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License. 
* You may obtain a copy of the License at 
* 
* http://www.apache.org/licenses/LICENSE-2.0 
* 
* Unless required by applicable law or agreed to in writing, 
* software distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*/

package net.lingala.zip4j.model;

import java.util.ArrayList;

public class LocalFileHeader {
	
	private int signature;
	
	private int versionNeededToExtract;
	
	private byte[] generalPurposeFlag;
	
	private int compressionMethod;
	
	private int lastModFileTime;
	
	private long crc32;
	
	private byte[] crcBuff;
	
	private long compressedSize;
	
	private long uncompressedSize;
	
	private int fileNameLength;
	
	private int extraFieldLength;
	
	private String fileName;
	
	private byte[] extraField;
	
	private long offsetStartOfData;
	
	private boolean isEncrypted;
	
	private int encryptionMethod;
	
	private char[] password;
	
	private ArrayList extraDataRecords;
	
	private Zip64ExtendedInfo zip64ExtendedInfo;
	
	private AESExtraDataRecord aesExtraDataRecord;
	
	private boolean dataDescriptorExists;
	
	private boolean writeComprSizeInZip64ExtraRecord;
	
	private boolean fileNameUTF8Encoded;
	
	public LocalFileHeader() {
		encryptionMethod = -1;
		writeComprSizeInZip64ExtraRecord = false;
		crc32 = 0;
		uncompressedSize = 0;
	}
	
	public int getSignature() {
		return signature;
	}

	public void setSignature(int signature) {
		this.signature = signature;
	}

	public int getVersionNeededToExtract() {
		return versionNeededToExtract;
	}

	public void setVersionNeededToExtract(int versionNeededToExtract) {
		this.versionNeededToExtract = versionNeededToExtract;
	}

	public byte[] getGeneralPurposeFlag() {
		return generalPurposeFlag;
	}

	public void setGeneralPurposeFlag(byte[] generalPurposeFlag) {
		this.generalPurposeFlag = generalPurposeFlag;
	}

	public int getCompressionMethod() {
		return compressionMethod;
	}

	public void setCompressionMethod(int compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	public int getLastModFileTime() {
		return lastModFileTime;
	}

	public void setLastModFileTime(int lastModFileTime) {
		this.lastModFileTime = lastModFileTime;
	}

	public long getCrc32() {
		return crc32;
	}

	public void setCrc32(long crc32) {
		this.crc32 = crc32;
	}

	public long getCompressedSize() {
		return compressedSize;
	}

	public void setCompressedSize(long compressedSize) {
		this.compressedSize = compressedSize;
	}

	public long getUncompressedSize() {
		return uncompressedSize;
	}

	public void setUncompressedSize(long uncompressedSize) {
		this.uncompressedSize = uncompressedSize;
	}

	public int getFileNameLength() {
		return fileNameLength;
	}

	public void setFileNameLength(int fileNameLength) {
		this.fileNameLength = fileNameLength;
	}

	public int getExtraFieldLength() {
		return extraFieldLength;
	}

	public void setExtraFieldLength(int extraFieldLength) {
		this.extraFieldLength = extraFieldLength;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getExtraField() {
		return extraField;
	}

	public void setExtraField(byte[] extraField) {
		this.extraField = extraField;
	}

	public long getOffsetStartOfData() {
		return offsetStartOfData;
	}

	public void setOffsetStartOfData(long offsetStartOfData) {
		this.offsetStartOfData = offsetStartOfData;
	}

	public boolean isEncrypted() {
		return isEncrypted;
	}

	public void setEncrypted(boolean isEncrypted) {
		this.isEncrypted = isEncrypted;
	}

	public int getEncryptionMethod() {
		return encryptionMethod;
	}

	public void setEncryptionMethod(int encryptionMethod) {
		this.encryptionMethod = encryptionMethod;
	}

	public byte[] getCrcBuff() {
		return crcBuff;
	}

	public void setCrcBuff(byte[] crcBuff) {
		this.crcBuff = crcBuff;
	}
	
	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public ArrayList getExtraDataRecords() {
		return extraDataRecords;
	}

	public void setExtraDataRecords(ArrayList extraDataRecords) {
		this.extraDataRecords = extraDataRecords;
	}

	public boolean isDataDescriptorExists() {
		return dataDescriptorExists;
	}

	public void setDataDescriptorExists(boolean dataDescriptorExists) {
		this.dataDescriptorExists = dataDescriptorExists;
	}

	public Zip64ExtendedInfo getZip64ExtendedInfo() {
		return zip64ExtendedInfo;
	}

	public void setZip64ExtendedInfo(Zip64ExtendedInfo zip64ExtendedInfo) {
		this.zip64ExtendedInfo = zip64ExtendedInfo;
	}

	public AESExtraDataRecord getAesExtraDataRecord() {
		return aesExtraDataRecord;
	}

	public void setAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord) {
		this.aesExtraDataRecord = aesExtraDataRecord;
	}

	public boolean isWriteComprSizeInZip64ExtraRecord() {
		return writeComprSizeInZip64ExtraRecord;
	}

	public void setWriteComprSizeInZip64ExtraRecord(
			boolean writeComprSizeInZip64ExtraRecord) {
		this.writeComprSizeInZip64ExtraRecord = writeComprSizeInZip64ExtraRecord;
	}

	public boolean isFileNameUTF8Encoded() {
		return fileNameUTF8Encoded;
	}

	public void setFileNameUTF8Encoded(boolean fileNameUTF8Encoded) {
		this.fileNameUTF8Encoded = fileNameUTF8Encoded;
	}
	
}
