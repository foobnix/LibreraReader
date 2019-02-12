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

package net.lingala.zip4j.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;

import net.lingala.zip4j.core.HeaderWriter;
import net.lingala.zip4j.crypto.AESEncrpyter;
import net.lingala.zip4j.crypto.IEncrypter;
import net.lingala.zip4j.crypto.StandardEncrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.EndCentralDirRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jConstants;
import net.lingala.zip4j.util.Zip4jUtil;

public class CipherOutputStream extends BaseOutputStream {
	
	protected OutputStream outputStream;
	private File sourceFile;
	protected FileHeader fileHeader;
	protected LocalFileHeader localFileHeader;
	private IEncrypter encrypter;
	protected ZipParameters zipParameters;
	protected ZipModel zipModel;
	private long totalBytesWritten;
	protected CRC32 crc;
	private long bytesWrittenForThisFile;
	private byte[] pendingBuffer;
	private int pendingBufferLength;
	private long totalBytesRead;
	
	public CipherOutputStream(OutputStream outputStream, ZipModel zipModel) {
		this.outputStream = outputStream;
		initZipModel(zipModel);
		crc = new CRC32();
		this.totalBytesWritten = 0;
		this.bytesWrittenForThisFile = 0;
		this.pendingBuffer = new byte[InternalZipConstants.AES_BLOCK_SIZE];
		this.pendingBufferLength = 0;
		this.totalBytesRead = 0;
	}
	
	public void putNextEntry(File file, ZipParameters zipParameters) throws ZipException {
		if (!zipParameters.isSourceExternalStream() && file == null) {
			throw new ZipException("input file is null");
		}
		
		if (!zipParameters.isSourceExternalStream() && !Zip4jUtil.checkFileExists(file)) {
			throw new ZipException("input file does not exist");
		}
		
		try {
			sourceFile = file;
			
			this.zipParameters = (ZipParameters)zipParameters.clone();
			
			if (!zipParameters.isSourceExternalStream()) {
				if (sourceFile.isDirectory()) {
					this.zipParameters.setEncryptFiles(false);
					this.zipParameters.setEncryptionMethod(-1);
					this.zipParameters.setCompressionMethod(Zip4jConstants.COMP_STORE);
				}
			} else {
				if (!Zip4jUtil.isStringNotNullAndNotEmpty(this.zipParameters.getFileNameInZip())) {
					throw new ZipException("file name is empty for external stream");
				}
				if (this.zipParameters.getFileNameInZip().endsWith("/") || 
						this.zipParameters.getFileNameInZip().endsWith("\\")) {
					this.zipParameters.setEncryptFiles(false);
					this.zipParameters.setEncryptionMethod(-1);
					this.zipParameters.setCompressionMethod(Zip4jConstants.COMP_STORE);
				}
			}
			
			createFileHeader();
			createLocalFileHeader();
			
			if (zipModel.isSplitArchive()) {
				if (zipModel.getCentralDirectory() == null || 
						zipModel.getCentralDirectory().getFileHeaders() == null || 
						zipModel.getCentralDirectory().getFileHeaders().size() == 0) {
					byte[] intByte = new byte[4];
					Raw.writeIntLittleEndian(intByte, 0, (int)InternalZipConstants.SPLITSIG);
					outputStream.write(intByte);
					totalBytesWritten += 4;
				}
			}
			
			if (this.outputStream instanceof SplitOutputStream) {
				if (totalBytesWritten == 4) {
					fileHeader.setOffsetLocalHeader(4);
				} else {
					fileHeader.setOffsetLocalHeader(((SplitOutputStream)outputStream).getFilePointer());
				}
			} else {
				if (totalBytesWritten == 4) {
					fileHeader.setOffsetLocalHeader(4);
				} else {
					fileHeader.setOffsetLocalHeader(totalBytesWritten);
				}
			}
			
			HeaderWriter headerWriter = new HeaderWriter();
			totalBytesWritten += headerWriter.writeLocalFileHeader(zipModel, localFileHeader, outputStream);
			
			if (this.zipParameters.isEncryptFiles()) {
				initEncrypter();
				if (encrypter != null) {
					if (zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_STANDARD) {
						byte[] headerBytes = ((StandardEncrypter)encrypter).getHeaderBytes();
						outputStream.write(headerBytes);
						totalBytesWritten += headerBytes.length;
						bytesWrittenForThisFile += headerBytes.length;
					} else if (zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
						byte[] saltBytes = ((AESEncrpyter)encrypter).getSaltBytes();
						byte[] passwordVerifier = ((AESEncrpyter)encrypter).getDerivedPasswordVerifier();
						outputStream.write(saltBytes);
						outputStream.write(passwordVerifier);
						totalBytesWritten += saltBytes.length + passwordVerifier.length;
						bytesWrittenForThisFile += saltBytes.length + passwordVerifier.length;
					}
				}
			} 
			
			crc.reset();
		} catch (CloneNotSupportedException e) {
			throw new ZipException(e);
		} catch (ZipException e) {
			throw e;
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	private void initEncrypter() throws ZipException {
		if (!zipParameters.isEncryptFiles()) {
			encrypter = null;
			return;
		}
		
		switch (zipParameters.getEncryptionMethod()) {
		case Zip4jConstants.ENC_METHOD_STANDARD:
			// Since we do not know the crc here, we use the modification time for encrypting.
			encrypter = new StandardEncrypter(zipParameters.getPassword(), (localFileHeader.getLastModFileTime() & 0x0000ffff) << 16);
			break;
		case Zip4jConstants.ENC_METHOD_AES:
			encrypter = new AESEncrpyter(zipParameters.getPassword(), zipParameters.getAesKeyStrength());
			break;
		default:
			throw new ZipException("invalid encprytion method");
		}
	}
	
	private void initZipModel(ZipModel zipModel) {
		if (zipModel == null) {
			this.zipModel = new ZipModel();
		} else {
			this.zipModel = zipModel;
		}
		
		if (this.zipModel.getEndCentralDirRecord() == null)
			this.zipModel.setEndCentralDirRecord(new EndCentralDirRecord());
		
		if (this.zipModel.getCentralDirectory() == null)
			this.zipModel.setCentralDirectory(new CentralDirectory());
		
		if (this.zipModel.getCentralDirectory().getFileHeaders() == null)
			this.zipModel.getCentralDirectory().setFileHeaders(new ArrayList());
		
		if (this.zipModel.getLocalFileHeaderList() == null)
			this.zipModel.setLocalFileHeaderList(new ArrayList());
		
		if (this.outputStream instanceof SplitOutputStream) {
			if (((SplitOutputStream)outputStream).isSplitZipFile()) {
				this.zipModel.setSplitArchive(true);
				this.zipModel.setSplitLength(((SplitOutputStream)outputStream).getSplitLength());
			}
		}
		
		this.zipModel.getEndCentralDirRecord().setSignature(InternalZipConstants.ENDSIG);
	}
	
	public void write(int bval) throws IOException {
	    byte[] b = new byte[1];
	    b[0] = (byte) bval;
	    write(b, 0, 1);
	}
	
	public void write(byte[] b) throws IOException {
		if (b == null)
			throw new NullPointerException();
		
		if (b.length == 0) return;
		
		write(b, 0, b.length);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		if (len == 0) return;
		
		if (zipParameters.isEncryptFiles() && 
				zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
			if (pendingBufferLength != 0) {
				if (len >= (InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength)) {
					System.arraycopy(b, off, pendingBuffer, pendingBufferLength,
									(InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength));
					encryptAndWrite(pendingBuffer, 0, pendingBuffer.length);
					off = (InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength);
					len = len - off;
					pendingBufferLength = 0;
				} else {
					System.arraycopy(b, off, pendingBuffer, pendingBufferLength,
							len);
					pendingBufferLength += len;
					return;
				}
			}
			if (len != 0 && len % 16 != 0) {
				System.arraycopy(b, (len + off) - (len % 16), pendingBuffer, 0, len % 16);
				pendingBufferLength = len % 16;
				len = len - pendingBufferLength; 
			}
		}
		if (len != 0)
			encryptAndWrite(b, off, len);
	}
	
	private void encryptAndWrite(byte[] b, int off, int len) throws IOException {
		if (encrypter != null) {
			try {
				encrypter.encryptData(b, off, len);
			} catch (ZipException e) {
				throw new IOException(e.getMessage());
			}
		}
		outputStream.write(b, off, len);
		totalBytesWritten += len;
		bytesWrittenForThisFile += len;
	}
	
	public void closeEntry() throws IOException, ZipException {
		
		if (this.pendingBufferLength != 0) {
			encryptAndWrite(pendingBuffer, 0, pendingBufferLength);
			pendingBufferLength = 0;
		}
		
		if (this.zipParameters.isEncryptFiles() && 
				this.zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
			if (encrypter instanceof AESEncrpyter) {
				outputStream.write(((AESEncrpyter)encrypter).getFinalMac());
				bytesWrittenForThisFile += 10;
				totalBytesWritten += 10;
			} else {
				throw new ZipException("invalid encrypter for AES encrypted file");
			}
		}
		fileHeader.setCompressedSize(bytesWrittenForThisFile);
		localFileHeader.setCompressedSize(bytesWrittenForThisFile);
		
		if (zipParameters.isSourceExternalStream()) {
			fileHeader.setUncompressedSize(totalBytesRead);
			if (localFileHeader.getUncompressedSize() != totalBytesRead) {
				localFileHeader.setUncompressedSize(totalBytesRead);
			}
		}
		
		long crc32 = crc.getValue();
		if (fileHeader.isEncrypted()) {
			if (fileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
				crc32 = 0;
			}
		}
		
		if (zipParameters.isEncryptFiles() && 
				zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
			fileHeader.setCrc32(0);
			localFileHeader.setCrc32(0);
		} else {
			fileHeader.setCrc32(crc32);
			localFileHeader.setCrc32(crc32);
		}
		
		zipModel.getLocalFileHeaderList().add(localFileHeader);
		zipModel.getCentralDirectory().getFileHeaders().add(fileHeader);
		
		HeaderWriter headerWriter = new HeaderWriter();
		totalBytesWritten += headerWriter.writeExtendedLocalHeader(localFileHeader, outputStream);
		
		crc.reset();
		bytesWrittenForThisFile = 0;
		encrypter = null;
		totalBytesRead = 0;
	}
	
	public void finish() throws IOException, ZipException {
		zipModel.getEndCentralDirRecord().setOffsetOfStartOfCentralDir(totalBytesWritten);
		
		HeaderWriter headerWriter = new HeaderWriter();
		headerWriter.finalizeZipFile(zipModel, outputStream);
	}
	
	public void close() throws IOException {
		if (outputStream != null)
			outputStream.close();
	}
	
	private void createFileHeader() throws ZipException {
		this.fileHeader = new FileHeader();
		fileHeader.setSignature((int)InternalZipConstants.CENSIG);
		fileHeader.setVersionMadeBy(20);
		fileHeader.setVersionNeededToExtract(20);
		if (zipParameters.isEncryptFiles() && 
				zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
			fileHeader.setCompressionMethod(Zip4jConstants.ENC_METHOD_AES);
			fileHeader.setAesExtraDataRecord(generateAESExtraDataRecord(zipParameters));
		} else {
			fileHeader.setCompressionMethod(zipParameters.getCompressionMethod());
		}
		if (zipParameters.isEncryptFiles()) {
			fileHeader.setEncrypted(true);
			fileHeader.setEncryptionMethod(zipParameters.getEncryptionMethod());
		}
		String fileName = null;
		if (zipParameters.isSourceExternalStream()) {
			fileHeader.setLastModFileTime((int) Zip4jUtil.javaToDosTime(System.currentTimeMillis()));
			if (!Zip4jUtil.isStringNotNullAndNotEmpty(zipParameters.getFileNameInZip())) {
				throw new ZipException("fileNameInZip is null or empty");
			}
			fileName = zipParameters.getFileNameInZip();
		} else {
			fileHeader.setLastModFileTime((int) Zip4jUtil.javaToDosTime((Zip4jUtil.getLastModifiedFileTime(
					sourceFile, zipParameters.getTimeZone()))));
			fileHeader.setUncompressedSize(sourceFile.length());
			fileName = Zip4jUtil.getRelativeFileName(
					sourceFile.getAbsolutePath(), zipParameters.getRootFolderInZip(), zipParameters.getDefaultFolderPath());
			
		}
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
			throw new ZipException("fileName is null or empty. unable to create file header");
		}
		
		fileHeader.setFileName(fileName);
		
		if (Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getFileNameCharset())) {
			fileHeader.setFileNameLength(Zip4jUtil.getEncodedStringLength(fileName, 
					zipModel.getFileNameCharset()));
		} else {
			fileHeader.setFileNameLength(Zip4jUtil.getEncodedStringLength(fileName));
		}
		
		if (outputStream instanceof SplitOutputStream) {
			fileHeader.setDiskNumberStart(((SplitOutputStream)outputStream).getCurrSplitFileCounter());
		} else {
			fileHeader.setDiskNumberStart(0);
		}
		
		int fileAttrs = 0;
		if (!zipParameters.isSourceExternalStream())
			fileAttrs = getFileAttributes(sourceFile);
		byte[] externalFileAttrs = {(byte)fileAttrs, 0, 0, 0};
		fileHeader.setExternalFileAttr(externalFileAttrs);
		
		if (zipParameters.isSourceExternalStream()) {
			fileHeader.setDirectory(fileName.endsWith("/") || fileName.endsWith("\\"));
		} else {
			fileHeader.setDirectory(this.sourceFile.isDirectory());
		}
		if (fileHeader.isDirectory()) {
			fileHeader.setCompressedSize(0);
			fileHeader.setUncompressedSize(0);
		} else {
			if (!zipParameters.isSourceExternalStream()) {
				long fileSize = Zip4jUtil.getFileLengh(sourceFile);
				if (zipParameters.getCompressionMethod() == Zip4jConstants.COMP_STORE) {
					if (zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_STANDARD) {
						fileHeader.setCompressedSize(fileSize
								+ InternalZipConstants.STD_DEC_HDR_SIZE);
					} else if (zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
						int saltLength = 0;
						switch (zipParameters.getAesKeyStrength()) {
						case Zip4jConstants.AES_STRENGTH_128:
							saltLength = 8;
							break;
						case Zip4jConstants.AES_STRENGTH_256:
							saltLength = 16;
							break;
						default:
							throw new ZipException("invalid aes key strength, cannot determine key sizes");
						}
						fileHeader.setCompressedSize(fileSize + saltLength
								+ InternalZipConstants.AES_AUTH_LENGTH + 2); //2 is password verifier
					} else {
						fileHeader.setCompressedSize(0);
					}
				} else {
					fileHeader.setCompressedSize(0);
				}
				fileHeader.setUncompressedSize(fileSize);
			}
		}
		if (zipParameters.isEncryptFiles() && 
				zipParameters.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_STANDARD) {
			fileHeader.setCrc32(zipParameters.getSourceFileCRC());
		}
		byte[] shortByte = new byte[2]; 
		shortByte[0] = Raw.bitArrayToByte(generateGeneralPurposeBitArray(
				fileHeader.isEncrypted(), zipParameters.getCompressionMethod()));
		boolean isFileNameCharsetSet = Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getFileNameCharset());
	    if ((isFileNameCharsetSet &&
	            zipModel.getFileNameCharset().equalsIgnoreCase(InternalZipConstants.CHARSET_UTF8)) ||
	        (!isFileNameCharsetSet &&
	            Zip4jUtil.detectCharSet(fileHeader.getFileName()).equals(InternalZipConstants.CHARSET_UTF8))) {
	        shortByte[1] = 8;
	    } else {
	        shortByte[1] = 0;
	    }
		fileHeader.setGeneralPurposeFlag(shortByte);
	}
	
	private void createLocalFileHeader() throws ZipException {
		if (fileHeader == null) {
			throw new ZipException("file header is null, cannot create local file header");
		}
		this.localFileHeader = new LocalFileHeader();
		localFileHeader.setSignature((int)InternalZipConstants.LOCSIG);
		localFileHeader.setVersionNeededToExtract(fileHeader.getVersionNeededToExtract());
		localFileHeader.setCompressionMethod(fileHeader.getCompressionMethod());
		localFileHeader.setLastModFileTime(fileHeader.getLastModFileTime());
		localFileHeader.setUncompressedSize(fileHeader.getUncompressedSize());
		localFileHeader.setFileNameLength(fileHeader.getFileNameLength());
		localFileHeader.setFileName(fileHeader.getFileName());
		localFileHeader.setEncrypted(fileHeader.isEncrypted());
		localFileHeader.setEncryptionMethod(fileHeader.getEncryptionMethod());
		localFileHeader.setAesExtraDataRecord(fileHeader.getAesExtraDataRecord());
		localFileHeader.setCrc32(fileHeader.getCrc32());
		localFileHeader.setCompressedSize(fileHeader.getCompressedSize());
		localFileHeader.setGeneralPurposeFlag((byte[])fileHeader.getGeneralPurposeFlag().clone());
	}
	
	/**
	 * Checks the file attributes and returns an integer
	 * @param file
	 * @return
	 * @throws ZipException
	 */
	private int getFileAttributes(File file) throws ZipException {
		if (file == null) {
			throw new ZipException("input file is null, cannot get file attributes");
		}
		
		if (!file.exists()) {
			return 0;
		}
		
		if (file.isDirectory()) {
			if (file.isHidden()) {
				return InternalZipConstants.FOLDER_MODE_HIDDEN;
			} else {
				return InternalZipConstants.FOLDER_MODE_NONE;
			}
		} else {
			if (!file.canWrite() && file.isHidden()) {
				return InternalZipConstants.FILE_MODE_READ_ONLY_HIDDEN;
			} else if (!file.canWrite()) {
				return InternalZipConstants.FILE_MODE_READ_ONLY;
			} else if (file.isHidden()) {
				return InternalZipConstants.FILE_MODE_HIDDEN;
			} else {
				return InternalZipConstants.FILE_MODE_NONE;
			}
		}
	}
	
	private int[] generateGeneralPurposeBitArray(boolean isEncrpyted, int compressionMethod) {
		
		int[] generalPurposeBits = new int[8];
		if (isEncrpyted) {
			generalPurposeBits[0] = 1;
		} else {
			generalPurposeBits[0] = 0;
		}
		
		if (compressionMethod == Zip4jConstants.COMP_DEFLATE) {
			// Have to set flags for deflate
		} else {
			generalPurposeBits[1] = 0;
			generalPurposeBits[2] = 0;
		}

		generalPurposeBits[3] = 1;
		
		return generalPurposeBits;
	}
	
	private AESExtraDataRecord generateAESExtraDataRecord(ZipParameters parameters) throws ZipException {
		
		if (parameters == null) {
			throw new ZipException("zip parameters are null, cannot generate AES Extra Data record");
		}
		
		AESExtraDataRecord aesDataRecord = new AESExtraDataRecord();
		aesDataRecord.setSignature(InternalZipConstants.AESSIG);
		aesDataRecord.setDataSize(7);
		aesDataRecord.setVendorID("AE");
		// Always set the version number to 2 as we do not store CRC for any AES encrypted files
		// only MAC is stored and as per the specification, if version number is 2, then MAC is read
		// and CRC is ignored
		aesDataRecord.setVersionNumber(2); 
		if (parameters.getAesKeyStrength() == Zip4jConstants.AES_STRENGTH_128) {
			aesDataRecord.setAesStrength(Zip4jConstants.AES_STRENGTH_128);
		} else if (parameters.getAesKeyStrength() == Zip4jConstants.AES_STRENGTH_256) {
			aesDataRecord.setAesStrength(Zip4jConstants.AES_STRENGTH_256);
		} else {
			throw new ZipException("invalid AES key strength, cannot generate AES Extra data record");
		}
		aesDataRecord.setCompressionMethod(parameters.getCompressionMethod());
		
		return aesDataRecord;
	}
	
	public void decrementCompressedFileSize(int value) {
		if (value <= 0) return;
		
		if (value <= this.bytesWrittenForThisFile) {
			this.bytesWrittenForThisFile -= value;
		}
	}
	
	protected void updateTotalBytesRead(int toUpdate) {
		if (toUpdate > 0) {
			totalBytesRead += toUpdate;
		}
	}
	
	public void setSourceFile(File sourceFile) {
		this.sourceFile = sourceFile;
	}

	public File getSourceFile() {
		return sourceFile;
	}
}
