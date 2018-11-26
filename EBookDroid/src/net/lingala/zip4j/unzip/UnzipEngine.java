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

package net.lingala.zip4j.unzip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.zip.CRC32;

import net.lingala.zip4j.core.HeaderReader;
import net.lingala.zip4j.crypto.AESDecrypter;
import net.lingala.zip4j.crypto.IDecrypter;
import net.lingala.zip4j.crypto.StandardDecrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.InflaterInputStream;
import net.lingala.zip4j.io.PartInputStream;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jConstants;
import net.lingala.zip4j.util.Zip4jUtil;

public class UnzipEngine {
	
	private ZipModel zipModel;
	private FileHeader fileHeader;
	private int currSplitFileCounter = 0;
	private LocalFileHeader localFileHeader;
	private IDecrypter decrypter;
	private CRC32 crc;
	
	public UnzipEngine(ZipModel zipModel, FileHeader fileHeader) throws ZipException {
		if (zipModel == null || fileHeader == null) {
			throw new ZipException("Invalid parameters passed to StoreUnzip. One or more of the parameters were null");
		}
		
		this.zipModel = zipModel;
		this.fileHeader = fileHeader;
		this.crc = new CRC32();
	}
	
	public void unzipFile(ProgressMonitor progressMonitor, 
			String outPath, String newFileName, UnzipParameters unzipParameters) throws ZipException {
		if (zipModel == null || fileHeader == null || !Zip4jUtil.isStringNotNullAndNotEmpty(outPath)) {
			throw new ZipException("Invalid parameters passed during unzipping file. One or more of the parameters were null");
		}
		InputStream is = null;
		OutputStream os = null;
		try {
			byte[] buff = new byte[InternalZipConstants.BUFF_SIZE];
			int readLength = -1;
			
			is = getInputStream();
			os = getOutputStream(outPath, newFileName);
			
			while ((readLength = is.read(buff)) != -1) {
				os.write(buff, 0, readLength);
				progressMonitor.updateWorkCompleted(readLength);
				if (progressMonitor.isCancelAllTasks()) {
					progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
					progressMonitor.setState(ProgressMonitor.STATE_READY);
					return;
				}
			}
			
			closeStreams(is, os);
			
			UnzipUtil.applyFileAttributes(fileHeader, new File(getOutputFileNameWithPath(outPath, newFileName)), unzipParameters);
			
		} catch (IOException e) {
			throw new ZipException(e);
		} catch (Exception e) {
			throw new ZipException(e);
		} finally {
			closeStreams(is, os);
		}
	}
	
	public ZipInputStream getInputStream() throws ZipException {
		if (fileHeader == null) {
			throw new ZipException("file header is null, cannot get inputstream");
		}
		
		RandomAccessFile raf = null;
		try {
			raf = createFileHandler(InternalZipConstants.READ_MODE);
			String errMsg = "local header and file header do not match";
			//checkSplitFile();
			
			if (!checkLocalHeader())
				throw new ZipException(errMsg);
			
			init(raf);
			
			long comprSize = localFileHeader.getCompressedSize();
			long offsetStartOfData = localFileHeader.getOffsetStartOfData();
			
			if (localFileHeader.isEncrypted()) {
				if (localFileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
					if (decrypter instanceof AESDecrypter) {
						comprSize -= (((AESDecrypter)decrypter).getSaltLength() + 
								((AESDecrypter)decrypter).getPasswordVerifierLength() + 10);
						offsetStartOfData += (((AESDecrypter)decrypter).getSaltLength() + 
								((AESDecrypter)decrypter).getPasswordVerifierLength());
					} else {
						throw new ZipException("invalid decryptor when trying to calculate " +
								"compressed size for AES encrypted file: " + fileHeader.getFileName());
					}
				} else if (localFileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_STANDARD) {
					comprSize -= InternalZipConstants.STD_DEC_HDR_SIZE;
					offsetStartOfData += InternalZipConstants.STD_DEC_HDR_SIZE;
				}
			}
			
			int compressionMethod = fileHeader.getCompressionMethod();
			if (fileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
				if (fileHeader.getAesExtraDataRecord() != null) {
					compressionMethod = fileHeader.getAesExtraDataRecord().getCompressionMethod();
				} else {
					throw new ZipException("AESExtraDataRecord does not exist for AES encrypted file: " + fileHeader.getFileName());
				}
			}
			raf.seek(offsetStartOfData);
			switch (compressionMethod) {
			case Zip4jConstants.COMP_STORE:
				return new ZipInputStream(new PartInputStream(raf, offsetStartOfData, comprSize, this));
			case Zip4jConstants.COMP_DEFLATE:
				return new ZipInputStream(new InflaterInputStream(raf, offsetStartOfData, comprSize, this));
			default:
				throw new ZipException("compression type not supported");
			}
		} catch (ZipException e) {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e1) {
					//ignore
				}
			}
			throw e;
		} catch (Exception e) {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e1) {
				}
			}
			throw new ZipException(e);
		}
		
	}
	
	private void init(RandomAccessFile raf) throws ZipException {
		
		if (localFileHeader == null) {
			throw new ZipException("local file header is null, cannot initialize input stream");
		}
		
		try {
			initDecrypter(raf);
		} catch (ZipException e) {
			throw e;
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	private void initDecrypter(RandomAccessFile raf) throws ZipException {
		if (localFileHeader == null) {
			throw new ZipException("local file header is null, cannot init decrypter");
		}
		
		if (localFileHeader.isEncrypted()) {
			if (localFileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_STANDARD) {
				decrypter = new StandardDecrypter(fileHeader, getStandardDecrypterHeaderBytes(raf));
			} else if (localFileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
				decrypter = new AESDecrypter(localFileHeader, getAESSalt(raf), getAESPasswordVerifier(raf));
			} else {
				throw new ZipException("unsupported encryption method");
			}
		}
	}
	
	private byte[] getStandardDecrypterHeaderBytes(RandomAccessFile raf) throws ZipException {
		try {
			byte[] headerBytes = new byte[InternalZipConstants.STD_DEC_HDR_SIZE];
			raf.seek(localFileHeader.getOffsetStartOfData());
			raf.read(headerBytes, 0, 12);
			return headerBytes;
		} catch (IOException e) {
			throw new ZipException(e);
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	private byte[] getAESSalt(RandomAccessFile raf) throws ZipException {
		if (localFileHeader.getAesExtraDataRecord() == null)
			return null;
		
		try {
			AESExtraDataRecord aesExtraDataRecord = localFileHeader.getAesExtraDataRecord();
			byte[] saltBytes = new byte[calculateAESSaltLength(aesExtraDataRecord)]; 			
			raf.seek(localFileHeader.getOffsetStartOfData());
			raf.read(saltBytes);
			return saltBytes;
		} catch (IOException e) {
			throw new ZipException(e);
		}
	}
	
	private byte[] getAESPasswordVerifier(RandomAccessFile raf) throws ZipException {
		try {
			byte[] pvBytes = new byte[2];
			raf.read(pvBytes);
			return pvBytes;
		} catch (IOException e) {
			throw new ZipException(e);
		}
	}
	
	private int calculateAESSaltLength(AESExtraDataRecord aesExtraDataRecord) throws ZipException {
		if (aesExtraDataRecord == null) {
			throw new ZipException("unable to determine salt length: AESExtraDataRecord is null");
		}
		switch (aesExtraDataRecord.getAesStrength()) {
		case Zip4jConstants.AES_STRENGTH_128:
			return 8;
		case Zip4jConstants.AES_STRENGTH_192:
			return 12;
		case Zip4jConstants.AES_STRENGTH_256:
			return 16;
		default:
			throw new ZipException("unable to determine salt length: invalid aes key strength");
		}
	}
	
	public void checkCRC() throws ZipException {
		if (fileHeader != null) {
			if (fileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
				if (decrypter != null && decrypter instanceof AESDecrypter) {
					byte[] tmpMacBytes = ((AESDecrypter)decrypter).getCalculatedAuthenticationBytes();
					byte[] storedMac = ((AESDecrypter)decrypter).getStoredMac();
					byte[] calculatedMac = new byte[InternalZipConstants.AES_AUTH_LENGTH]; 
					
					if (calculatedMac == null || storedMac == null) {
						throw new ZipException("CRC (MAC) check failed for " + fileHeader.getFileName());
					}
					
					System.arraycopy(tmpMacBytes, 0, calculatedMac, 0, InternalZipConstants.AES_AUTH_LENGTH);
					
					if (!Arrays.equals(calculatedMac, storedMac)) {
						throw new ZipException("invalid CRC (MAC) for file: " + fileHeader.getFileName());
					}
				}
			} else {
				long calculatedCRC = crc.getValue() & 0xffffffffL;
				if (calculatedCRC != fileHeader.getCrc32()) {
					String errMsg = "invalid CRC for file: " + fileHeader.getFileName();
					if (localFileHeader.isEncrypted() && 
							localFileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_STANDARD) {
						errMsg += " - Wrong Password?";
					}
					throw new ZipException(errMsg);
				}
			}
		}
	}
	
//	private void checkCRC() throws ZipException {
//		if (fileHeader != null) {
//			if (fileHeader.getEncryptionMethod() == Zip4jConstants.ENC_METHOD_AES) {
//				if (decrypter != null && decrypter instanceof AESDecrypter) {
//					byte[] tmpMacBytes = ((AESDecrypter)decrypter).getCalculatedAuthenticationBytes();
//					byte[] actualMacBytes = ((AESDecrypter)decrypter).getStoredMac();
//					if (tmpMacBytes == null || actualMacBytes == null) {
//						throw new ZipException("null mac value for AES encrypted file: " + fileHeader.getFileName());
//					}
//					byte[] calcMacBytes = new byte[10];
//					System.arraycopy(tmpMacBytes, 0, calcMacBytes, 0, 10);
//					if (!Arrays.equals(calcMacBytes, actualMacBytes)) {
//						throw new ZipException("invalid CRC(mac) for file: " + fileHeader.getFileName());
//					}
//				} else {
//					throw new ZipException("invalid decryptor...cannot calculate mac value for file: " 
//							+ fileHeader.getFileName());
//				}
//			} else if (unzipEngine != null) {
//				long calculatedCRC = unzipEngine.getCRC();
//				long actualCRC = fileHeader.getCrc32();
//				if (calculatedCRC != actualCRC) {
//					throw new ZipException("invalid CRC for file: " + fileHeader.getFileName());
//				}
//			}
//		}
//	}
	
	private boolean checkLocalHeader() throws ZipException {
		RandomAccessFile rafForLH = null;
		try {
			rafForLH = checkSplitFile();
			
			if (rafForLH == null) {
				rafForLH = new RandomAccessFile(new File(this.zipModel.getZipFile()), InternalZipConstants.READ_MODE);
			}
			
			HeaderReader headerReader = new HeaderReader(rafForLH);
			this.localFileHeader = headerReader.readLocalFileHeader(fileHeader);
			
			if (localFileHeader == null) {
				throw new ZipException("error reading local file header. Is this a valid zip file?");
			}
			
			//TODO Add more comparision later
			if (localFileHeader.getCompressionMethod() != fileHeader.getCompressionMethod()) {
				return false;
			}
			
			return true;
		} catch (FileNotFoundException e) {
			throw new ZipException(e);
		} finally {
			if (rafForLH != null) {
				try {
					rafForLH.close();
				} catch (IOException e) {
					// Ignore this
				} catch (Exception e) {
					//Ignore this
				}
			}
		}
	}
	
	private RandomAccessFile checkSplitFile() throws ZipException {
		if (zipModel.isSplitArchive()) {
			int diskNumberStartOfFile = fileHeader.getDiskNumberStart();
			currSplitFileCounter = diskNumberStartOfFile + 1;
			String curZipFile = zipModel.getZipFile();
			String partFile = null;
			if (diskNumberStartOfFile == zipModel.getEndCentralDirRecord().getNoOfThisDisk()) {
				partFile = zipModel.getZipFile();
			} else {
				if (diskNumberStartOfFile >= 9) {
					partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z" + (diskNumberStartOfFile+ 1);
				} else{
					partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z0" + (diskNumberStartOfFile+ 1);
				}
			}
			
			try {
				RandomAccessFile raf = new RandomAccessFile(partFile, InternalZipConstants.READ_MODE);
				
				if (currSplitFileCounter == 1) {
					byte[] splitSig = new byte[4];
					raf.read(splitSig);
					if (Raw.readIntLittleEndian(splitSig, 0) != InternalZipConstants.SPLITSIG) {
						throw new ZipException("invalid first part split file signature");
					}
				}
				return raf;
			} catch (FileNotFoundException e) {
				throw new ZipException(e);
			} catch (IOException e) {
				throw new ZipException(e);
			}
		}
		return null;
	}
	
	private RandomAccessFile createFileHandler(String mode) throws ZipException {
		if (this.zipModel == null || !Zip4jUtil.isStringNotNullAndNotEmpty(this.zipModel.getZipFile())) {
			throw new ZipException("input parameter is null in getFilePointer");
		}
		
		try {
			RandomAccessFile raf = null;
			if (zipModel.isSplitArchive()) {
				raf = checkSplitFile();
			} else {
				raf = new RandomAccessFile(new File(this.zipModel.getZipFile()), mode);
			}
			return raf;
		} catch (FileNotFoundException e) {
			throw new ZipException(e);
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	private FileOutputStream getOutputStream(String outPath, String newFileName) throws ZipException {
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(outPath)) {
			throw new ZipException("invalid output path");
		}
		
		try {
			File file = new File(getOutputFileNameWithPath(outPath, newFileName));
			
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			
			if (file.exists()) {
				file.delete();
			}
			
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			return fileOutputStream;
		} catch (FileNotFoundException e) {
			throw new ZipException(e);
		}
	}
	
	private String getOutputFileNameWithPath(String outPath, String newFileName) throws ZipException {
		String fileName = null;
		if (Zip4jUtil.isStringNotNullAndNotEmpty(newFileName)) {
			fileName = newFileName;
		} else {
			fileName = fileHeader.getFileName();
		}
		return outPath + System.getProperty("file.separator") + fileName;
	}
	
	public RandomAccessFile startNextSplitFile() throws IOException, FileNotFoundException {
		String currZipFile = zipModel.getZipFile();
		String partFile = null;
		if (currSplitFileCounter == zipModel.getEndCentralDirRecord().getNoOfThisDisk()) {
			partFile = zipModel.getZipFile();
		} else {
			if (currSplitFileCounter >= 9) {
				partFile = currZipFile.substring(0, currZipFile.lastIndexOf(".")) + ".z" + (currSplitFileCounter + 1);
			} else {
				partFile = currZipFile.substring(0, currZipFile.lastIndexOf(".")) + ".z0" + (currSplitFileCounter + 1);
			}
		}
		currSplitFileCounter++;
		try {
			if(!Zip4jUtil.checkFileExists(partFile)) {
				throw new IOException("zip split file does not exist: " + partFile);
			}
		} catch (ZipException e) {
			throw new IOException(e.getMessage());
		}
		return new RandomAccessFile(partFile, InternalZipConstants.READ_MODE);
	}
	
	private void closeStreams(InputStream is, OutputStream os) throws ZipException {
		try {
			if (is != null) {
				is.close();
				is = null;
			}
		} catch (IOException e) {
			if (e != null && Zip4jUtil.isStringNotNullAndNotEmpty(e.getMessage())) {
				if (e.getMessage().indexOf(" - Wrong Password?") >= 0) {
					throw new ZipException(e.getMessage());
				}
			}
		} finally {
			try {
				if (os != null) {
					os.close();
					os = null;
				}
			} catch (IOException e) {
				//do nothing
			}
		}
	}
	
	public void updateCRC(int b) {
		crc.update(b);
	}
	
	public void updateCRC(byte[] buff, int offset, int len) {
		if (buff != null) {
			crc.update(buff, offset, len);
		}
	}

	public FileHeader getFileHeader() {
		return fileHeader;
	}

	public IDecrypter getDecrypter() {
		return decrypter;
	}

	public ZipModel getZipModel() {
		return zipModel;
	}

	public LocalFileHeader getLocalFileHeader() {
		return localFileHeader;
	}
}
