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

package net.lingala.zip4j.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import net.lingala.zip4j.core.HeaderReader;
import net.lingala.zip4j.core.HeaderWriter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.SplitOutputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.Zip64EndCentralDirLocator;
import net.lingala.zip4j.model.Zip64EndCentralDirRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;

public class ArchiveMaintainer {
	
	public ArchiveMaintainer() {
	}
	
	public HashMap removeZipFile(final ZipModel zipModel, 
			final FileHeader fileHeader, final ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
		
		if (runInThread) {
			Thread thread = new Thread(InternalZipConstants.THREAD_NAME) {
				public void run() {
					try {
						initRemoveZipFile(zipModel, fileHeader, progressMonitor);
						progressMonitor.endProgressMonitorSuccess();
					} catch (ZipException e) {
					}
				}
			};
			thread.start();
			return null;
		} else {
			HashMap retMap = initRemoveZipFile(zipModel, fileHeader, progressMonitor);
			progressMonitor.endProgressMonitorSuccess();
			return retMap;
		}
		
	}
	
	public HashMap initRemoveZipFile(ZipModel zipModel, 
			FileHeader fileHeader, ProgressMonitor progressMonitor) throws ZipException {
		
		if (fileHeader == null || zipModel == null) {
			throw new ZipException("input parameters is null in maintain zip file, cannot remove file from archive");
		}
		
		OutputStream outputStream = null;
		File zipFile = null;
		RandomAccessFile inputStream = null;
		boolean successFlag = false;
		String tmpZipFileName = null;
		HashMap retMap = new HashMap();
		
		try {
			int indexOfFileHeader = Zip4jUtil.getIndexOfFileHeader(zipModel, fileHeader);
			
			if (indexOfFileHeader < 0) {
				throw new ZipException("file header not found in zip model, cannot remove file");
			}
			
			if (zipModel.isSplitArchive()) {
				throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
			}
			
			long currTime = System.currentTimeMillis();
			tmpZipFileName = zipModel.getZipFile() + currTime%1000;
			File tmpFile = new File(tmpZipFileName);
			
			while (tmpFile.exists()) {
				currTime = System.currentTimeMillis();
				tmpZipFileName = zipModel.getZipFile() + currTime%1000;
				tmpFile = new File(tmpZipFileName);
			}
			
			try {
				outputStream = new SplitOutputStream(new File(tmpZipFileName));
			} catch (FileNotFoundException e1) {
				throw new ZipException(e1);
			}
			
			zipFile = new File(zipModel.getZipFile());
			
			inputStream = createFileHandler(zipModel, InternalZipConstants.READ_MODE);
			
			HeaderReader headerReader = new HeaderReader(inputStream);
			LocalFileHeader localFileHeader = headerReader.readLocalFileHeader(fileHeader);
			if (localFileHeader == null) {
				throw new ZipException("invalid local file header, cannot remove file from archive");
			}
			
			long offsetLocalFileHeader = fileHeader.getOffsetLocalHeader();
			
			if (fileHeader.getZip64ExtendedInfo() != null && 
					fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {
				offsetLocalFileHeader = fileHeader.getZip64ExtendedInfo().getOffsetLocalHeader();
			}
			
			long offsetEndOfCompressedFile = -1;
			
			long offsetStartCentralDir = zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir();
			if (zipModel.isZip64Format()) {
				if (zipModel.getZip64EndCentralDirRecord() != null) {
					offsetStartCentralDir = zipModel.getZip64EndCentralDirRecord().getOffsetStartCenDirWRTStartDiskNo();
				}
			}
			
			ArrayList fileHeaderList = zipModel.getCentralDirectory().getFileHeaders();
			
			if (indexOfFileHeader == fileHeaderList.size() - 1) {
				offsetEndOfCompressedFile = offsetStartCentralDir - 1;
			} else {
				FileHeader nextFileHeader = (FileHeader)fileHeaderList.get(indexOfFileHeader + 1);
				if (nextFileHeader != null) {
					offsetEndOfCompressedFile = nextFileHeader.getOffsetLocalHeader() - 1;
					if (nextFileHeader.getZip64ExtendedInfo() != null && 
							nextFileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {
						offsetEndOfCompressedFile = nextFileHeader.getZip64ExtendedInfo().getOffsetLocalHeader() - 1;
					}
				}
			}
			
			if (offsetLocalFileHeader < 0 || offsetEndOfCompressedFile < 0) {
				throw new ZipException("invalid offset for start and end of local file, cannot remove file");
			}
			
			if(indexOfFileHeader == 0) {
				if (zipModel.getCentralDirectory().getFileHeaders().size() > 1) {
					// if this is the only file and it is deleted then no need to do this
					copyFile(inputStream, outputStream, offsetEndOfCompressedFile + 1, offsetStartCentralDir, progressMonitor);
				}	
			} else if (indexOfFileHeader == fileHeaderList.size() - 1) {
				copyFile(inputStream, outputStream, 0, offsetLocalFileHeader, progressMonitor);
			} else {
				copyFile(inputStream, outputStream, 0, offsetLocalFileHeader, progressMonitor);
				copyFile(inputStream, outputStream, offsetEndOfCompressedFile + 1, offsetStartCentralDir, progressMonitor);
			}
			
			if (progressMonitor.isCancelAllTasks()) {
				progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
				progressMonitor.setState(ProgressMonitor.STATE_READY);
				return null;
			}
			
			zipModel.getEndCentralDirRecord().setOffsetOfStartOfCentralDir(((SplitOutputStream)outputStream).getFilePointer());
			zipModel.getEndCentralDirRecord().setTotNoOfEntriesInCentralDir(
					zipModel.getEndCentralDirRecord().getTotNoOfEntriesInCentralDir() - 1);
			zipModel.getEndCentralDirRecord().setTotNoOfEntriesInCentralDirOnThisDisk(
					zipModel.getEndCentralDirRecord().getTotNoOfEntriesInCentralDirOnThisDisk() - 1);
			
			zipModel.getCentralDirectory().getFileHeaders().remove(indexOfFileHeader);
			
			for (int i = indexOfFileHeader; i < zipModel.getCentralDirectory().getFileHeaders().size(); i++) {
				long offsetLocalHdr = ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getOffsetLocalHeader();
				if (((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getZip64ExtendedInfo() != null && 
						((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getZip64ExtendedInfo().getOffsetLocalHeader() != -1) {
					offsetLocalHdr = ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getZip64ExtendedInfo().getOffsetLocalHeader();
				}
				
				((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).setOffsetLocalHeader(
						offsetLocalHdr - (offsetEndOfCompressedFile - offsetLocalFileHeader) - 1);
			}
			
			HeaderWriter headerWriter = new HeaderWriter();
			headerWriter.finalizeZipFile(zipModel, outputStream);
			
			successFlag = true;
			
			retMap.put(InternalZipConstants.OFFSET_CENTRAL_DIR, 
					Long.toString(zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir()));
			
		} catch (ZipException e) {
			progressMonitor.endProgressMonitorError(e);
			throw e;
		} catch (Exception e) {
			progressMonitor.endProgressMonitorError(e);
			throw new ZipException(e);
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
				if (outputStream != null)
					outputStream.close();
			} catch (IOException e) {
				throw new ZipException("cannot close input stream or output stream when trying to delete a file from zip file");
			}
			
			if (successFlag) {
				restoreFileName(zipFile, tmpZipFileName);
			} else {
				File newZipFile = new File(tmpZipFileName);
				newZipFile.delete();				
			}
		}
		
		return retMap;
	}
	
	private void restoreFileName(File zipFile, String tmpZipFileName) throws ZipException {
		if (zipFile.delete())
		{
			File newZipFile = new File(tmpZipFileName);
			if (!newZipFile.renameTo(zipFile)) {
				throw new ZipException("cannot rename modified zip file");
			}
		} else {
			throw new ZipException("cannot delete old zip file");
		}
	}
	
	private void copyFile(RandomAccessFile inputStream, 
			OutputStream outputStream, long start, long end, ProgressMonitor progressMonitor) throws ZipException {
		
		if (inputStream == null || outputStream == null) {
			throw new ZipException("input or output stream is null, cannot copy file");
		}
		
		if (start < 0) {
			throw new ZipException("starting offset is negative, cannot copy file");
		}
		
		if (end < 0) {
			throw new ZipException("end offset is negative, cannot copy file");
		}
		
		if (start > end) {
			throw new ZipException("start offset is greater than end offset, cannot copy file");
		}
		
		if (start == end) {
			return;
		}
		
		if (progressMonitor.isCancelAllTasks()) {
			progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
			progressMonitor.setState(ProgressMonitor.STATE_READY);
			return;
		}
		
		try {
			inputStream.seek(start);
			
			int readLen = -2;
			byte[] buff;
			long bytesRead = 0;
			long bytesToRead = end - start;
			
			if ((end - start) < InternalZipConstants.BUFF_SIZE) {
				buff = new byte[(int)(end - start)];
			} else {
				buff = new byte[InternalZipConstants.BUFF_SIZE];
			}
			
			while ((readLen = inputStream.read(buff)) != -1) {
				outputStream.write(buff, 0, readLen);
				
				progressMonitor.updateWorkCompleted(readLen);
				if (progressMonitor.isCancelAllTasks()) {
					progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
					return;
				}
				
				bytesRead += readLen;
				
				if(bytesRead == bytesToRead) {
					break;
				} else if (bytesRead + buff.length > bytesToRead) {
					buff = new byte[(int)(bytesToRead - bytesRead)];
				}
			}
			
		} catch (IOException e) {
			throw new ZipException(e);
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	private RandomAccessFile createFileHandler(ZipModel zipModel, String mode) throws ZipException {
		if (zipModel == null || !Zip4jUtil.isStringNotNullAndNotEmpty(zipModel.getZipFile())) {
			throw new ZipException("input parameter is null in getFilePointer, cannot create file handler to remove file");
		}
		
		try {
			return new RandomAccessFile(new File(zipModel.getZipFile()), mode);
		} catch (FileNotFoundException e) {
			throw new ZipException(e);
		}
	}
	
	/**
	 * Merges split Zip files into a single Zip file
	 * @param zipModel
	 * @throws ZipException
	 */
	public void mergeSplitZipFiles(final ZipModel zipModel, final File outputZipFile, 
			final ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
		if (runInThread) {
			Thread thread = new Thread(InternalZipConstants.THREAD_NAME) {
				public void run() {
					try {
						initMergeSplitZipFile(zipModel, outputZipFile, progressMonitor);
					} catch (ZipException e) {
					}
				}
			};
			thread.start();
		} else {
			initMergeSplitZipFile(zipModel, outputZipFile, progressMonitor);
		}
	}
	
	private void initMergeSplitZipFile(ZipModel zipModel, File outputZipFile, 
			ProgressMonitor progressMonitor) throws ZipException {
		if (zipModel == null) {
			ZipException e = new ZipException("one of the input parameters is null, cannot merge split zip file");
			progressMonitor.endProgressMonitorError(e);
			throw e;
		}
		
		if (!zipModel.isSplitArchive()) {
			ZipException e = new ZipException("archive not a split zip file");
			progressMonitor.endProgressMonitorError(e);
			throw e;
		}
		
		OutputStream outputStream = null;
		RandomAccessFile inputStream = null;
		ArrayList fileSizeList = new ArrayList();
		long totBytesWritten = 0;
		boolean splitSigRemoved = false;
		try {
			
			int totNoOfSplitFiles = zipModel.getEndCentralDirRecord().getNoOfThisDisk();
			
			if (totNoOfSplitFiles <= 0) {
				throw new ZipException("corrupt zip model, archive not a split zip file");
			}
			
			outputStream = prepareOutputStreamForMerge(outputZipFile);
			for (int i = 0; i <= totNoOfSplitFiles; i++) {
				inputStream = createSplitZipFileHandler(zipModel, i);
				
				int start = 0;
				Long end = new Long(inputStream.length());
				
				if (i == 0) {
					if (zipModel.getCentralDirectory() != null && 
							zipModel.getCentralDirectory().getFileHeaders() != null && 
							zipModel.getCentralDirectory().getFileHeaders().size() > 0) {
						byte[] buff = new byte[4];
						inputStream.seek(0);
						inputStream.read(buff);
						if (Raw.readIntLittleEndian(buff, 0) == InternalZipConstants.SPLITSIG) {
							start = 4;
							splitSigRemoved = true;
						}
					}
				}
				
				if (i == totNoOfSplitFiles) {
					end = new Long(zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir());
				}
				
				copyFile(inputStream, outputStream, start, end.longValue(), progressMonitor);
				totBytesWritten += (end.longValue() - start);
				if (progressMonitor.isCancelAllTasks()) {
					progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
					progressMonitor.setState(ProgressMonitor.STATE_READY);
					return;
				}
				
				fileSizeList.add(end);
				
				try {
					inputStream.close();
				} catch (IOException e) {
					//ignore
				}
			}
			
			ZipModel newZipModel = (ZipModel)zipModel.clone();
			newZipModel.getEndCentralDirRecord().setOffsetOfStartOfCentralDir(totBytesWritten);
			
			updateSplitZipModel(newZipModel, fileSizeList, splitSigRemoved);
			
			HeaderWriter headerWriter = new HeaderWriter();
			headerWriter.finalizeZipFileWithoutValidations(newZipModel, outputStream);
			
			progressMonitor.endProgressMonitorSuccess();
			
		} catch (IOException e) {
			progressMonitor.endProgressMonitorError(e);
			throw new ZipException(e);
		} catch (Exception e) {
			progressMonitor.endProgressMonitorError(e);
			throw new ZipException(e);
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					//ignore
				}
			}
			
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	/**
	 * Creates an input stream for the split part of the zip file
	 * @return Zip4jInputStream
	 * @throws ZipException
	 */
	
	private RandomAccessFile createSplitZipFileHandler(ZipModel zipModel, int partNumber) throws ZipException {
		if (zipModel == null) {
			throw new ZipException("zip model is null, cannot create split file handler");
		}
		
		if (partNumber < 0) {
			throw new ZipException("invlaid part number, cannot create split file handler");
		}
		
		try {
			String curZipFile = zipModel.getZipFile();
			String partFile = null;
			if (partNumber == zipModel.getEndCentralDirRecord().getNoOfThisDisk()) {
				partFile = zipModel.getZipFile();
			} else {
				if (partNumber >= 9) {
					partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z" + (partNumber+ 1);
				} else{
					partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z0" + (partNumber+ 1);
				}
			}
			File tmpFile = new File(partFile);
			
			if (!Zip4jUtil.checkFileExists(tmpFile)) {
				throw new ZipException("split file does not exist: " + partFile);
			}
			
			return new RandomAccessFile(tmpFile, InternalZipConstants.READ_MODE);
		} catch (FileNotFoundException e) {
			throw new ZipException(e);
		} catch (Exception e) {
			throw new ZipException(e);
		}
		
	}
	
	private OutputStream prepareOutputStreamForMerge(File outFile) throws ZipException {
		if (outFile == null) {
			throw new ZipException("outFile is null, cannot create outputstream");
		}
		
		try {
			return new FileOutputStream(outFile);
		} catch (FileNotFoundException e) {
			throw new ZipException(e);
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	private void updateSplitZipModel(ZipModel zipModel, ArrayList fileSizeList, boolean splitSigRemoved) throws ZipException {
		if (zipModel == null) {
			throw new ZipException("zip model is null, cannot update split zip model");
		}
		
		zipModel.setSplitArchive(false);
		updateSplitFileHeader(zipModel, fileSizeList, splitSigRemoved);
		updateSplitEndCentralDirectory(zipModel);
		if (zipModel.isZip64Format()) {
			updateSplitZip64EndCentralDirLocator(zipModel, fileSizeList);
			updateSplitZip64EndCentralDirRec(zipModel, fileSizeList);
		}
	}
	
	private void updateSplitFileHeader(ZipModel zipModel, ArrayList fileSizeList, boolean splitSigRemoved) throws ZipException {
		try {
			
			if (zipModel.getCentralDirectory()== null) {
				throw new ZipException("corrupt zip model - getCentralDirectory, cannot update split zip model");
			}
			
			int fileHeaderCount = zipModel.getCentralDirectory().getFileHeaders().size();
			int splitSigOverhead = 0;
			if (splitSigRemoved)
				splitSigOverhead = 4;
			
			for (int i = 0; i < fileHeaderCount; i++) {
				long offsetLHToAdd = 0;
				
				for (int j = 0; j < ((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getDiskNumberStart(); j++) {
					offsetLHToAdd += ((Long)fileSizeList.get(j)).longValue();
				}
				((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).setOffsetLocalHeader(
						((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).getOffsetLocalHeader() +
						offsetLHToAdd - splitSigOverhead);
				((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).setDiskNumberStart(0);
			}
			
		} catch (ZipException e) {
			throw e;
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	private void updateSplitEndCentralDirectory(ZipModel zipModel) throws ZipException {
		try {
			if (zipModel == null) {
				throw new ZipException("zip model is null - cannot update end of central directory for split zip model");
			}
			
			if (zipModel.getCentralDirectory()== null) {
				throw new ZipException("corrupt zip model - getCentralDirectory, cannot update split zip model");
			}
			
			zipModel.getEndCentralDirRecord().setNoOfThisDisk(0);
			zipModel.getEndCentralDirRecord().setNoOfThisDiskStartOfCentralDir(0);
			zipModel.getEndCentralDirRecord().setTotNoOfEntriesInCentralDir(
					zipModel.getCentralDirectory().getFileHeaders().size());
			zipModel.getEndCentralDirRecord().setTotNoOfEntriesInCentralDirOnThisDisk(
					zipModel.getCentralDirectory().getFileHeaders().size());
			
		} catch (ZipException e) {
			throw e;
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	private void updateSplitZip64EndCentralDirLocator(ZipModel zipModel, ArrayList fileSizeList) throws ZipException {
		if (zipModel == null) {
			throw new ZipException("zip model is null, cannot update split Zip64 end of central directory locator");
		}
		
		if (zipModel.getZip64EndCentralDirLocator() == null) {
			return;
		}
		
		zipModel.getZip64EndCentralDirLocator().setNoOfDiskStartOfZip64EndOfCentralDirRec(0);
		long offsetZip64EndCentralDirRec = 0;
		
		for (int i = 0; i < fileSizeList.size(); i++) {
			offsetZip64EndCentralDirRec += ((Long)fileSizeList.get(i)).longValue();
		}
		zipModel.getZip64EndCentralDirLocator().setOffsetZip64EndOfCentralDirRec(
				((Zip64EndCentralDirLocator)zipModel.getZip64EndCentralDirLocator()).getOffsetZip64EndOfCentralDirRec() + 
				offsetZip64EndCentralDirRec);
		zipModel.getZip64EndCentralDirLocator().setTotNumberOfDiscs(1);
	}
	
	private void updateSplitZip64EndCentralDirRec(ZipModel zipModel, ArrayList fileSizeList) throws ZipException {
		if (zipModel == null) {
			throw new ZipException("zip model is null, cannot update split Zip64 end of central directory record");
		}
		
		if (zipModel.getZip64EndCentralDirRecord() == null) {
			return;
		}
		
		zipModel.getZip64EndCentralDirRecord().setNoOfThisDisk(0);
		zipModel.getZip64EndCentralDirRecord().setNoOfThisDiskStartOfCentralDir(0);
		zipModel.getZip64EndCentralDirRecord().setTotNoOfEntriesInCentralDirOnThisDisk(
				zipModel.getEndCentralDirRecord().getTotNoOfEntriesInCentralDir());
		
		long offsetStartCenDirWRTStartDiskNo = 0;
		
		for (int i = 0; i < fileSizeList.size(); i++) {
			offsetStartCenDirWRTStartDiskNo += ((Long)fileSizeList.get(i)).longValue();
		}
		
		zipModel.getZip64EndCentralDirRecord().setOffsetStartCenDirWRTStartDiskNo(
				((Zip64EndCentralDirRecord)zipModel.getZip64EndCentralDirRecord()).getOffsetStartCenDirWRTStartDiskNo() + 
				offsetStartCenDirWRTStartDiskNo);
	}
	
	public void setComment(ZipModel zipModel, String comment) throws ZipException {
		if (comment == null) {
			throw new ZipException("comment is null, cannot update Zip file with comment");
		}
		
		if (zipModel == null) {
			throw new ZipException("zipModel is null, cannot update Zip file with comment");
		}
		
		String encodedComment = comment;
		byte[] commentBytes = comment.getBytes();
		int commentLength = comment.length();
		
		if (Zip4jUtil.isSupportedCharset(InternalZipConstants.CHARSET_COMMENTS_DEFAULT)) {
			try {
				encodedComment = new String(comment.getBytes(InternalZipConstants.CHARSET_COMMENTS_DEFAULT), InternalZipConstants.CHARSET_COMMENTS_DEFAULT);
				commentBytes = encodedComment.getBytes(InternalZipConstants.CHARSET_COMMENTS_DEFAULT);
				commentLength = encodedComment.length();
			} catch (UnsupportedEncodingException e) {
				encodedComment = comment;
				commentBytes = comment.getBytes();
				commentLength = comment.length();
			}
		}
		
		if (commentLength > InternalZipConstants.MAX_ALLOWED_ZIP_COMMENT_LENGTH) {
			throw new ZipException("comment length exceeds maximum length");
		}
		
		zipModel.getEndCentralDirRecord().setComment(encodedComment);
		zipModel.getEndCentralDirRecord().setCommentBytes(commentBytes);
		zipModel.getEndCentralDirRecord().setCommentLength(commentLength);
		
		SplitOutputStream outputStream = null;
		
		try {
			HeaderWriter headerWriter = new HeaderWriter();
			outputStream = new SplitOutputStream(zipModel.getZipFile());
			
			if (zipModel.isZip64Format()) {
				outputStream.seek(zipModel.getZip64EndCentralDirRecord().getOffsetStartCenDirWRTStartDiskNo());
			} else {
				outputStream.seek(zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir());
			}
			
			headerWriter.finalizeZipFileWithoutValidations(zipModel, outputStream);
		} catch (FileNotFoundException e) {
			throw new ZipException(e);
		} catch (IOException e) {
			throw new ZipException(e);
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
	}
	
	public void initProgressMonitorForRemoveOp(ZipModel zipModel, 
			FileHeader fileHeader, ProgressMonitor progressMonitor) throws ZipException {
		if (zipModel == null || fileHeader == null || progressMonitor == null) {
			throw new ZipException("one of the input parameters is null, cannot calculate total work");
		}
		
		progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_REMOVE);
		progressMonitor.setFileName(fileHeader.getFileName());
		progressMonitor.setTotalWork(calculateTotalWorkForRemoveOp(zipModel, fileHeader));
		progressMonitor.setState(ProgressMonitor.STATE_BUSY);
	}
	
	private long calculateTotalWorkForRemoveOp(ZipModel zipModel, FileHeader fileHeader) throws ZipException {
		return Zip4jUtil.getFileLengh(new File(zipModel.getZipFile())) - fileHeader.getCompressedSize();
	}
	
	public void initProgressMonitorForMergeOp(ZipModel zipModel, ProgressMonitor progressMonitor) throws ZipException {
		if (zipModel == null) {
			throw new ZipException("zip model is null, cannot calculate total work for merge op");
		}
		
		progressMonitor.setCurrentOperation(ProgressMonitor.OPERATION_MERGE);
		progressMonitor.setFileName(zipModel.getZipFile());
		progressMonitor.setTotalWork(calculateTotalWorkForMergeOp(zipModel));
		progressMonitor.setState(ProgressMonitor.STATE_BUSY);
	}
	
	private long calculateTotalWorkForMergeOp(ZipModel zipModel) throws ZipException {
		long totSize = 0;
		if (zipModel.isSplitArchive()) {
			int totNoOfSplitFiles = zipModel.getEndCentralDirRecord().getNoOfThisDisk();
			String partFile = null;
			String curZipFile = zipModel.getZipFile();
			int partNumber = 0;
			for (int i = 0; i <= totNoOfSplitFiles; i++) {
				if (partNumber == zipModel.getEndCentralDirRecord().getNoOfThisDisk()) {
					partFile = zipModel.getZipFile();
				} else {
					if (partNumber >= 9) {
						partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z" + (partNumber+ 1);
					} else{
						partFile = curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z0" + (partNumber+ 1);
					}
				}
				
				totSize += Zip4jUtil.getFileLengh(new File(partFile)); 
			}
			
		}
		return totSize;
	}
}
