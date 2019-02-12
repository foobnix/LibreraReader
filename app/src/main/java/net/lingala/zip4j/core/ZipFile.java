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

package net.lingala.zip4j.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.exception.ZipExceptionConstants;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.unzip.Unzip;
import net.lingala.zip4j.util.ArchiveMaintainer;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Zip4jUtil;
import net.lingala.zip4j.zip.ZipEngine;

/**
 * Base class to handle zip files. Some of the operations supported
 * in this class are:<br>
 * <ul>
 * 		<li>Create Zip File</li>
 * 		<li>Add files to zip file</li>
 * 		<li>Add folder to zip file</li>
 * 		<li>Extract files from zip files</li>
 * 		<li>Remove files from zip file</li>
 * </ul>
 *
 */

public class ZipFile {
	
	private String file;
	private int mode;
	private ZipModel zipModel;
	private boolean isEncrypted;
	private ProgressMonitor progressMonitor;
	private boolean runInThread;
	private String fileNameCharset;
	
	/**
	 * Creates a new Zip File Object with the given zip file path.
	 * If the zip file does not exist, it is not created at this point. 
	 * @param zipFile
	 * @throws ZipException
	 */
	public ZipFile(String zipFile) throws ZipException {
		this(new File(zipFile));
	}
	
	/**
	 * Creates a new Zip File Object with the input file.
	 * If the zip file does not exist, it is not created at this point.
	 * @param zipFile
	 * @throws ZipException
	 */
	public ZipFile(File zipFile) throws ZipException {
		if (zipFile == null) {
			throw new ZipException("Input zip file parameter is not null", 
					ZipExceptionConstants.inputZipParamIsNull);
		}
		
		this.file = zipFile.getPath();
		this.mode = InternalZipConstants.MODE_UNZIP;
		this.progressMonitor = new ProgressMonitor();
		this.runInThread = false;
	}
	
	/**
	 * Creates a zip file and adds the source file to the zip file. If the zip file
	 * exists then this method throws an exception. Parameters such as compression type, etc
	 * can be set in the input parameters
	 * @param sourceFile - File to be added to the zip file
	 * @param parameters - parameters to create the zip file
	 * @throws ZipException
	 */
	public void createZipFile(File sourceFile, ZipParameters parameters) throws ZipException {
		ArrayList sourceFileList = new ArrayList();
		sourceFileList.add(sourceFile);
		createZipFile(sourceFileList, parameters, false, -1);
	}
	
	/**
	 * Creates a zip file and adds the source file to the zip file. If the zip file
	 * exists then this method throws an exception. Parameters such as compression type, etc
	 * can be set in the input parameters. While the method addFile/addFiles also creates the 
	 * zip file if it does not exist, the main functionality of this method is to create a split
	 * zip file. To create a split zip file, set the splitArchive parameter to true with a valid
	 * splitLength. Split Length has to be more than 65536 bytes
	 * @param sourceFile - File to be added to the zip file
	 * @param parameters - parameters to create the zip file
	 * @param splitArchive - if archive has to be split or not
	 * @param splitLength - if archive has to be split, then length in bytes at which it has to be split
	 * @throws ZipException
	 */
	public void createZipFile(File sourceFile, ZipParameters parameters, 
			boolean splitArchive, long splitLength) throws ZipException {
		
		ArrayList sourceFileList = new ArrayList();
		sourceFileList.add(sourceFile);
		createZipFile(sourceFileList, parameters, splitArchive, splitLength);
	}
	
	/**
	 * Creates a zip file and adds the list of source file(s) to the zip file. If the zip file
	 * exists then this method throws an exception. Parameters such as compression type, etc
	 * can be set in the input parameters
	 * @param sourceFileList - File to be added to the zip file
	 * @param parameters - parameters to create the zip file
	 * @throws ZipException
	 */
	public void createZipFile(ArrayList sourceFileList, 
			ZipParameters parameters) throws ZipException {
		createZipFile(sourceFileList, parameters, false, -1);
	}
	
	/**
	 * Creates a zip file and adds the list of source file(s) to the zip file. If the zip file
	 * exists then this method throws an exception. Parameters such as compression type, etc
	 * can be set in the input parameters. While the method addFile/addFiles also creates the 
	 * zip file if it does not exist, the main functionality of this method is to create a split
	 * zip file. To create a split zip file, set the splitArchive parameter to true with a valid
	 * splitLength. Split Length has to be more than 65536 bytes
	 * @param sourceFileList - File to be added to the zip file
	 * @param parameters - zip parameters for this file list
	 * @param splitArchive - if archive has to be split or not
	 * @param splitLength - if archive has to be split, then length in bytes at which it has to be split
	 * @throws ZipException
	 */
	public void createZipFile(ArrayList sourceFileList, ZipParameters parameters, 
			boolean splitArchive, long splitLength) throws ZipException {
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(file)) {
			throw new ZipException("zip file path is empty");
		}
		
		if (Zip4jUtil.checkFileExists(file)) {
			throw new ZipException("zip file: " + file + " already exists. To add files to existing zip file use addFile method");
		}
		
		if (sourceFileList == null) {
			throw new ZipException("input file ArrayList is null, cannot create zip file");
		}
		
		if (!Zip4jUtil.checkArrayListTypes(sourceFileList, InternalZipConstants.LIST_TYPE_FILE)) {
			throw new ZipException("One or more elements in the input ArrayList is not of type File");
		}
		
		createNewZipModel();
		this.zipModel.setSplitArchive(splitArchive);
		this.zipModel.setSplitLength(splitLength);
		addFiles(sourceFileList, parameters);
	}
	
	/**
	 * Creates a zip file and adds the files/folders from the specified folder to the zip file.
	 * This method does the same functionality as in addFolder method except that this method
	 * can also create split zip files when adding a folder. To create a split zip file, set the 
	 * splitArchive parameter to true and specify the splitLength. Split length has to be more than
	 * or equal to 65536 bytes. Note that this method throws an exception if the zip file already 
	 * exists.
	 * @param folderToAdd
	 * @param parameters
	 * @param splitArchive
	 * @param splitLength
	 * @throws ZipException
	 */
	public void createZipFileFromFolder(String folderToAdd, ZipParameters parameters, 
			boolean splitArchive, long splitLength) throws ZipException {
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(folderToAdd)) {
			throw new ZipException("folderToAdd is empty or null, cannot create Zip File from folder");
		}
		
		createZipFileFromFolder(new File(folderToAdd), parameters, splitArchive, splitLength);
		
	}
	
	/**
	 * Creates a zip file and adds the files/folders from the specified folder to the zip file.
	 * This method does the same functionality as in addFolder method except that this method
	 * can also create split zip files when adding a folder. To create a split zip file, set the 
	 * splitArchive parameter to true and specify the splitLength. Split length has to be more than
	 * or equal to 65536 bytes. Note that this method throws an exception if the zip file already 
	 * exists.
	 * @param folderToAdd
	 * @param parameters
	 * @param splitArchive
	 * @param splitLength
	 * @throws ZipException
	 */
	public void createZipFileFromFolder(File folderToAdd, ZipParameters parameters, 
			boolean splitArchive, long splitLength) throws ZipException {
		
		if (folderToAdd == null) {
			throw new ZipException("folderToAdd is null, cannot create zip file from folder");
		}
		
		if (parameters == null) {
			throw new ZipException("input parameters are null, cannot create zip file from folder");
		}
		
		if (Zip4jUtil.checkFileExists(file)) {
			throw new ZipException("zip file: " + file + " already exists. To add files to existing zip file use addFolder method");
		}
		
		createNewZipModel();
		this.zipModel.setSplitArchive(splitArchive);
		if (splitArchive)
			this.zipModel.setSplitLength(splitLength);
		
		addFolder(folderToAdd, parameters, false);
	}
	
	/**
	 * Adds input source file to the zip file. If zip file does not exist, then 
	 * this method creates a new zip file. Parameters such as compression type, etc
	 * can be set in the input parameters.
	 * @param sourceFile - File to tbe added to the zip file
	 * @param parameters - zip parameters for this file
	 * @throws ZipException
	 */
	public void addFile(File sourceFile, ZipParameters parameters) throws ZipException {
		ArrayList sourceFileList = new ArrayList();
		sourceFileList.add(sourceFile);
		addFiles(sourceFileList, parameters);
	}
	
	/**
	 * Adds the list of input files to the zip file. If zip file does not exist, then 
	 * this method creates a new zip file. Parameters such as compression type, etc
	 * can be set in the input parameters.
	 * @param sourceFileList
	 * @param parameters
	 * @throws ZipException
	 */
	public void addFiles(ArrayList sourceFileList, ZipParameters parameters) throws ZipException {
		
		checkZipModel();
		
		if (this.zipModel == null) {
			throw new ZipException("internal error: zip model is null");
		}
		
		if (sourceFileList == null) {
			throw new ZipException("input file ArrayList is null, cannot add files");
		}
		
		if (!Zip4jUtil.checkArrayListTypes(sourceFileList, InternalZipConstants.LIST_TYPE_FILE)) {
			throw new ZipException("One or more elements in the input ArrayList is not of type File");
		}
		
		if (parameters == null) {
			throw new ZipException("input parameters are null, cannot add files to zip");
		}
		
		if (progressMonitor.getState() == ProgressMonitor.STATE_BUSY) {
			throw new ZipException("invalid operation - Zip4j is in busy state");
		}
		
		if (Zip4jUtil.checkFileExists(file)) {
			if (zipModel.isSplitArchive()) {
				throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
			}
		}
		
		ZipEngine zipEngine = new ZipEngine(zipModel);
		zipEngine.addFiles(sourceFileList, parameters, progressMonitor, runInThread);
	}
	
	/**
	 * Adds the folder in the given path to the zip file. If zip file does not exist, 
	 * then a new zip file is created. If input folder path is invalid then an exception
	 * is thrown. Zip parameters for the files in the folder to be added can be set in
	 * the input parameters
	 * @param path
	 * @param parameters
	 * @throws ZipException
	 */
	public void addFolder(String path, ZipParameters parameters) throws ZipException {
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(path)) {
			throw new ZipException("input path is null or empty, cannot add folder to zip file");
		}
		
		addFolder(new File(path), parameters);
	}
	
	/**
	 * Adds the folder in the given file object to the zip file. If zip file does not exist, 
	 * then a new zip file is created. If input folder is invalid then an exception
	 * is thrown. Zip parameters for the files in the folder to be added can be set in
	 * the input parameters
	 * @param path
	 * @param parameters
	 * @throws ZipException
	 */
	public void addFolder(File path, ZipParameters parameters) throws ZipException {
		if (path == null) {
			throw new ZipException("input path is null, cannot add folder to zip file");
		}
		
		if (parameters == null) {
			throw new ZipException("input parameters are null, cannot add folder to zip file");
		}
		
		addFolder(path, parameters, true);
	}
	
	/**
	 * Internal method to add a folder to the zip file.
	 * @param path
	 * @param parameters
	 * @param checkSplitArchive
	 * @throws ZipException
	 */
	private void addFolder(File path, ZipParameters parameters, 
			boolean checkSplitArchive) throws ZipException {
		
		checkZipModel();
		
		if (this.zipModel == null) {
			throw new ZipException("internal error: zip model is null");
		}
		
		if (checkSplitArchive) {
			if (this.zipModel.isSplitArchive()) {
				throw new ZipException("This is a split archive. Zip file format does not allow updating split/spanned files");
			}
		}
		
		ZipEngine zipEngine = new ZipEngine(zipModel);
		zipEngine.addFolderToZip(path, parameters, progressMonitor, runInThread);
		
	}
	
	/**
	 * Creates a new entry in the zip file and adds the content of the inputstream to the
	 * zip file. ZipParameters.isSourceExternalStream and ZipParameters.fileNameInZip have to be
	 * set before in the input parameters. If the file name ends with / or \, this method treats the
	 * content as a directory. Setting the flag ProgressMonitor.setRunInThread to true will have
	 * no effect for this method and hence this method cannot be used to add content to zip in
	 * thread mode
	 * @param inputStream
	 * @param parameters
	 * @throws ZipException
	 */
	public void addStream(InputStream inputStream, ZipParameters parameters) throws ZipException {
		if (inputStream == null) {
			throw new ZipException("inputstream is null, cannot add file to zip");
		}
		
		if (parameters == null) {
			throw new ZipException("zip parameters are null");
		}
		
		this.setRunInThread(false);
		
		checkZipModel();
		
		if (this.zipModel == null) {
			throw new ZipException("internal error: zip model is null");
		}
		
		if (Zip4jUtil.checkFileExists(file)) {
			if (zipModel.isSplitArchive()) {
				throw new ZipException("Zip file already exists. Zip file format does not allow updating split/spanned files");
			}
		}
		
		ZipEngine zipEngine = new ZipEngine(zipModel);
		zipEngine.addStreamToZip(inputStream, parameters);
	}
	
	/**
	 * Reads the zip header information for this zip file. If the zip file
	 * does not exist, then this method throws an exception.<br><br>
	 * <b>Note:</b> This method does not read local file header information
	 * @throws ZipException
	 */
	private void readZipInfo() throws ZipException {
		
		if (!Zip4jUtil.checkFileExists(file)) {
			throw new ZipException("zip file does not exist");
		}
		
		if (!Zip4jUtil.checkFileReadAccess(this.file)) {
			throw new ZipException("no read access for the input zip file");
		}
		
		if (this.mode != InternalZipConstants.MODE_UNZIP) {
			throw new ZipException("Invalid mode");
		}
		
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(new File(file), InternalZipConstants.READ_MODE);
			
			if (zipModel == null) {
				
				HeaderReader headerReader = new HeaderReader(raf);
				zipModel = headerReader.readAllHeaders(this.fileNameCharset);
				if (zipModel != null) {
					zipModel.setZipFile(file);
				}
			}
		} catch (FileNotFoundException e) {
			throw new ZipException(e);
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
	}
	
	/**
	 * Extracts all the files in the given zip file to the input destination path.
	 * If zip file does not exist or destination path is invalid then an 
	 * exception is thrown. 
	 * @param destPath
	 * @throws ZipException
	 */
	public void extractAll(String destPath) throws ZipException {
		extractAll(destPath, null);
		
	}
	
	/**
	 * Extracts all the files in the given zip file to the input destination path.
	 * If zip file does not exist or destination path is invalid then an 
	 * exception is thrown.
	 * @param destPath
	 * @param unzipParameters
	 * @throws ZipException
	 */
	public void extractAll(String destPath, 
			UnzipParameters unzipParameters) throws ZipException {
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(destPath)) {
			throw new ZipException("output path is null or invalid");
		}
		
		if (!Zip4jUtil.checkOutputFolder(destPath)) {
			throw new ZipException("invalid output path");
		}
		
		if (zipModel == null) {
			readZipInfo();
		}
		
		// Throw an exception if zipModel is still null
		if (zipModel == null) {
			throw new ZipException("Internal error occurred when extracting zip file");
		}
		
		if (progressMonitor.getState() == ProgressMonitor.STATE_BUSY) {
			throw new ZipException("invalid operation - Zip4j is in busy state");
		}
		
		Unzip unzip = new Unzip(zipModel);
		unzip.extractAll(unzipParameters, destPath, progressMonitor, runInThread);
		
	}
	
	/**
	 * Extracts a specific file from the zip file to the destination path.
	 * If destination path is invalid, then this method throws an exception.
	 * @param fileHeader
	 * @param destPath
	 * @throws ZipException
	 */
	public void extractFile(FileHeader fileHeader, String destPath) throws ZipException {
		extractFile(fileHeader, destPath, null);
	}
	
	/**
	 * Extracts a specific file from the zip file to the destination path.
	 * If destination path is invalid, then this method throws an exception.
	 * <br><br>
	 * If newFileName is not null or empty, newly created file name will be replaced by 
	 * the value in newFileName. If this value is null, then the file name will be the 
	 * value in FileHeader.getFileName
	 * @param fileHeader
	 * @param destPath
	 * @param unzipParameters
	 * @throws ZipException
	 */
	public void extractFile(FileHeader fileHeader, 
			String destPath, UnzipParameters unzipParameters) throws ZipException {
		extractFile(fileHeader, destPath, unzipParameters, null);
	}
	
	/**
	 * Extracts a specific file from the zip file to the destination path.
	 * If destination path is invalid, then this method throws an exception.
	 * @param fileHeader
	 * @param destPath
	 * @param unzipParameters
	 * @param newFileName
	 * @throws ZipException
	 */
	public void extractFile(FileHeader fileHeader, String destPath, 
			UnzipParameters unzipParameters, String newFileName) throws ZipException {
		
		if (fileHeader == null) {
			throw new ZipException("input file header is null, cannot extract file");
		}
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(destPath)) {
			throw new ZipException("destination path is empty or null, cannot extract file");
		}
		
		readZipInfo();
		
		if (progressMonitor.getState() == ProgressMonitor.STATE_BUSY) {
			throw new ZipException("invalid operation - Zip4j is in busy state");
		}
		
		fileHeader.extractFile(zipModel, destPath, unzipParameters, newFileName, progressMonitor, runInThread);
		
	}
	
	/**
	 * Extracts a specific file from the zip file to the destination path. 
	 * This method first finds the necessary file header from the input file name.
	 * <br><br>
	 * File name is relative file name in the zip file. For example if a zip file contains
	 * a file "a.txt", then to extract this file, input file name has to be "a.txt". Another
	 * example is if there is a file "b.txt" in a folder "abc" in the zip file, then the
	 * input file name has to be abc/b.txt
	 * <br><br>
	 * Throws an exception if file header could not be found for the given file name or if 
	 * the destination path is invalid
	 * @param fileName
	 * @param destPath
	 * @throws ZipException
	 */
	public void extractFile(String fileName, String destPath) throws ZipException {
		extractFile(fileName, destPath, null);
	}
	
	/**
	 * Extracts a specific file from the zip file to the destination path. 
	 * This method first finds the necessary file header from the input file name.
	 * <br><br>
	 * File name is relative file name in the zip file. For example if a zip file contains
	 * a file "a.txt", then to extract this file, input file name has to be "a.txt". Another
	 * example is if there is a file "b.txt" in a folder "abc" in the zip file, then the
	 * input file name has to be abc/b.txt
	 * <br><br>
	 * Throws an exception if file header could not be found for the given file name or if 
	 * the destination path is invalid
	 * @param fileName
	 * @param destPath
	 * @param unzipParameters
	 * @throws ZipException
	 */
	public void extractFile(String fileName, 
			String destPath, UnzipParameters unzipParameters) throws ZipException {
		extractFile(fileName, destPath, unzipParameters, null);
	}
	
	/**
	 * Extracts a specific file from the zip file to the destination path. 
	 * This method first finds the necessary file header from the input file name.
	 * <br><br>
	 * File name is relative file name in the zip file. For example if a zip file contains
	 * a file "a.txt", then to extract this file, input file name has to be "a.txt". Another
	 * example is if there is a file "b.txt" in a folder "abc" in the zip file, then the
	 * input file name has to be abc/b.txt
	 * <br><br>
	 * If newFileName is not null or empty, newly created file name will be replaced by 
	 * the value in newFileName. If this value is null, then the file name will be the 
	 * value in FileHeader.getFileName
	 * <br><br>
	 * Throws an exception if file header could not be found for the given file name or if 
	 * the destination path is invalid
	 * @param fileName
	 * @param destPath
	 * @param unzipParameters
	 * @param newFileName
	 * @throws ZipException
	 */
	public void extractFile(String fileName, String destPath, 
			UnzipParameters unzipParameters, String newFileName) throws ZipException {
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
			throw new ZipException("file to extract is null or empty, cannot extract file");
		}
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(destPath)) {
			throw new ZipException("destination string path is empty or null, cannot extract file");
		}
		
		readZipInfo();
		
		FileHeader fileHeader = Zip4jUtil.getFileHeader(zipModel, fileName);
		
		if (fileHeader == null) {
			throw new ZipException("file header not found for given file name, cannot extract file");
		}
		
		if (progressMonitor.getState() == ProgressMonitor.STATE_BUSY) {
			throw new ZipException("invalid operation - Zip4j is in busy state");
		}
		
		fileHeader.extractFile(zipModel, destPath, unzipParameters, newFileName, progressMonitor, runInThread);
		
	}
	
	/**
	 * Sets the password for the zip file.<br>
	 * <b>Note</b>: For security reasons, usage of this method is discouraged. Use 
	 * setPassword(char[]) instead. As strings are immutable, they cannot be wiped
	 * out from memory explicitly after usage. Therefore, usage of Strings to store 
	 * passwords is discouraged. More info here: 
	 * http://docs.oracle.com/javase/1.5.0/docs/guide/security/jce/JCERefGuide.html#PBEEx
	 * @param password
	 * @throws ZipException
	 */
	public void setPassword(String password) throws ZipException {
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(password)) {
			throw new NullPointerException();
		}
		setPassword(password.toCharArray());
	}
	
	/**
	 * Sets the password for the zip file
	 * @param password
	 * @throws ZipException
	 */
	public void setPassword(char[] password) throws ZipException {
		if (zipModel == null) {
			readZipInfo();
			if (zipModel == null) {
				throw new ZipException("Zip Model is null");
			}
		}
		
		if (zipModel.getCentralDirectory() == null || zipModel.getCentralDirectory().getFileHeaders() == null) {
			throw new ZipException("invalid zip file");
		}
		
		for (int i = 0; i < zipModel.getCentralDirectory().getFileHeaders().size(); i++) {
			if (zipModel.getCentralDirectory().getFileHeaders().get(i) != null) {
				if (((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).isEncrypted()) {
					((FileHeader)zipModel.getCentralDirectory().getFileHeaders().get(i)).setPassword(password);
				}
			}
		}
	}
	
	/**
	 * Returns the list of file headers in the zip file. Throws an exception if the 
	 * zip file does not exist
	 * @return list of file headers
	 * @throws ZipException
	 */
	public List getFileHeaders() throws ZipException {
		readZipInfo();
		if (zipModel == null || zipModel.getCentralDirectory() == null) {
			return null;
		}
		return zipModel.getCentralDirectory().getFileHeaders();
	}
	
	/**
	 * Returns FileHeader if a file header with the given fileHeader 
	 * string exists in the zip model: If not returns null
	 * @param fileName
	 * @return FileHeader
	 * @throws ZipException
	 */
	public FileHeader getFileHeader(String fileName) throws ZipException {
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
			throw new ZipException("input file name is emtpy or null, cannot get FileHeader");
		}
		
		readZipInfo();
		if (zipModel == null || zipModel.getCentralDirectory() == null) {
			return null;
		}
		
		return Zip4jUtil.getFileHeader(zipModel, fileName);
	}
	
	/**
	 * Checks to see if the zip file is encrypted
	 * @return true if encrypted, false if not
	 * @throws ZipException
	 */
	public boolean isEncrypted() throws ZipException {
		if (zipModel == null) {
			readZipInfo();
			if (zipModel == null) {
				throw new ZipException("Zip Model is null");
			}
		}
		
		if (zipModel.getCentralDirectory() == null || zipModel.getCentralDirectory().getFileHeaders() == null) {
			throw new ZipException("invalid zip file");
		}
		
		ArrayList fileHeaderList = zipModel.getCentralDirectory().getFileHeaders();
		for (int i = 0; i < fileHeaderList.size(); i++) {
			FileHeader fileHeader = (FileHeader)fileHeaderList.get(i);
			if (fileHeader != null) {
				if (fileHeader.isEncrypted()) {
					isEncrypted = true;
					break;
				}
			}
		}
		
		return isEncrypted;
	}
	
	/**
	 * Checks if the zip file is a split archive
	 * @return true if split archive, false if not
	 * @throws ZipException
	 */
	public boolean isSplitArchive() throws ZipException {

		if (zipModel == null) {
			readZipInfo();
			if (zipModel == null) {
				throw new ZipException("Zip Model is null");
			}
		}
		
		return zipModel.isSplitArchive();
	
	}
	
	/**
	 * Removes the file provided in the input paramters from the zip file.
	 * This method first finds the file header and then removes the file.
	 * If file does not exist, then this method throws an exception.
	 * If zip file is a split zip file, then this method throws an exception as
	 * zip specification does not allow for updating split zip archives.
	 * @param fileName
	 * @throws ZipException
	 */
	public void removeFile(String fileName) throws ZipException {
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
			throw new ZipException("file name is empty or null, cannot remove file");
		}
		
		if (zipModel == null) {
			if (Zip4jUtil.checkFileExists(file)) {
				readZipInfo();
			}
		}
		
		if (zipModel.isSplitArchive()) {
			throw new ZipException("Zip file format does not allow updating split/spanned files");
		}
		
		FileHeader fileHeader = Zip4jUtil.getFileHeader(zipModel, fileName);
		if (fileHeader == null) {
			throw new ZipException("could not find file header for file: " + fileName);
		}
		
		removeFile(fileHeader);
	}
	
	/**
	 * Removes the file provided in the input file header from the zip file.
	 * If zip file is a split zip file, then this method throws an exception as
	 * zip specification does not allow for updating split zip archives.
	 * @param fileHeader
	 * @throws ZipException
	 */
	public void removeFile(FileHeader fileHeader) throws ZipException {
		if (fileHeader == null) {
			throw new ZipException("file header is null, cannot remove file");
		}
		
		if (zipModel == null) {
			if (Zip4jUtil.checkFileExists(file)) {
				readZipInfo();
			}
		}
		
		if (zipModel.isSplitArchive()) {
			throw new ZipException("Zip file format does not allow updating split/spanned files");
		}
		
		ArchiveMaintainer archiveMaintainer = new ArchiveMaintainer();
		archiveMaintainer.initProgressMonitorForRemoveOp(zipModel, fileHeader, progressMonitor);
		archiveMaintainer.removeZipFile(zipModel, fileHeader, progressMonitor, runInThread);
	}
	
	/**
	 * Merges split zip files into a single zip file without the need to extract the
	 * files in the archive
	 * @param outputZipFile
	 * @throws ZipException
	 */
	public void mergeSplitFiles(File outputZipFile) throws ZipException {
		if (outputZipFile == null) {
			throw new ZipException("outputZipFile is null, cannot merge split files");
		}
		
		if (outputZipFile.exists()) {
			throw new ZipException("output Zip File already exists");
		}
		
		checkZipModel();
		
		if (this.zipModel == null) {
			throw new ZipException("zip model is null, corrupt zip file?");
		}
		
		ArchiveMaintainer archiveMaintainer = new ArchiveMaintainer();
		archiveMaintainer.initProgressMonitorForMergeOp(zipModel, progressMonitor);
		archiveMaintainer.mergeSplitZipFiles(zipModel, outputZipFile, progressMonitor, runInThread);
	}
	
	/**
	 * Sets comment for the Zip file
	 * @param comment
	 * @throws ZipException
	 */
	public void setComment(String comment) throws ZipException {
		if (comment == null) {
			throw new ZipException("input comment is null, cannot update zip file");
		}
		
		if (!Zip4jUtil.checkFileExists(file)) {
			throw new ZipException("zip file does not exist, cannot set comment for zip file");
		}
		
		readZipInfo();
		
		if (this.zipModel == null) {
			throw new ZipException("zipModel is null, cannot update zip file");
		}
		
		if (zipModel.getEndCentralDirRecord() == null) {
			throw new ZipException("end of central directory is null, cannot set comment");
		}
		
		ArchiveMaintainer archiveMaintainer = new ArchiveMaintainer();
		archiveMaintainer.setComment(zipModel, comment);
	}
	
	/**
	 * Returns the comment set for the Zip file
	 * @return String
	 * @throws ZipException
	 */
	public String getComment() throws ZipException {
		return getComment(null);
	}
	
	/**
	 * Returns the comment set for the Zip file in the input encoding
	 * @param encoding
	 * @return String
	 * @throws ZipException
	 */
	public String getComment(String encoding) throws ZipException {
		if (encoding == null) {
			if (Zip4jUtil.isSupportedCharset(InternalZipConstants.CHARSET_COMMENTS_DEFAULT)) {
				encoding = InternalZipConstants.CHARSET_COMMENTS_DEFAULT;
			} else {
				encoding = InternalZipConstants.CHARSET_DEFAULT;
			}
		}
		
		if (Zip4jUtil.checkFileExists(file)) {
			checkZipModel();
		} else {
			throw new ZipException("zip file does not exist, cannot read comment");
		}
		
		if (this.zipModel == null) {
			throw new ZipException("zip model is null, cannot read comment");
		}
		
		if (this.zipModel.getEndCentralDirRecord() == null) {
			throw new ZipException("end of central directory record is null, cannot read comment");
		}
		
		if (this.zipModel.getEndCentralDirRecord().getCommentBytes() == null || 
				this.zipModel.getEndCentralDirRecord().getCommentBytes().length <= 0) {
			return null;
		}
		
		try {
			return new String(this.zipModel.getEndCentralDirRecord().getCommentBytes(), encoding);
		} catch (UnsupportedEncodingException e) {
			throw new ZipException(e);
		}
	}
	
	/**
	 * Loads the zip model if zip model is null and if zip file exists.
	 * @throws ZipException
	 */
	private void checkZipModel() throws ZipException {
		if (this.zipModel == null) {
			if (Zip4jUtil.checkFileExists(file)) {
				readZipInfo();
			} else {
				createNewZipModel();
			}
		}
	}
	
	/**
	 * Creates a new instance of zip model
	 * @throws ZipException
	 */
	private void createNewZipModel() {
		zipModel = new ZipModel();
		zipModel.setZipFile(file);
		zipModel.setFileNameCharset(fileNameCharset);
	}
	
	/**
	 * Zip4j will encode all the file names with the input charset. This method throws
	 * an exception if the Charset is not supported
	 * @param charsetName
	 * @throws ZipException
	 */
	public void setFileNameCharset(String charsetName) throws ZipException {
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(charsetName)) {
			throw new ZipException("null or empty charset name");
		}
		
		if (!Zip4jUtil.isSupportedCharset(charsetName)) {
			throw new ZipException("unsupported charset: " + charsetName);
		}
		
		this.fileNameCharset = charsetName;
	}
	
	/**
	 * Returns an input stream for reading the contents of the Zip file corresponding
	 * to the input FileHeader. Throws an exception if the FileHeader does not exist
	 * in the ZipFile
	 * @param fileHeader
	 * @return ZipInputStream
	 * @throws ZipException
	 */
	public ZipInputStream getInputStream(FileHeader fileHeader) throws ZipException {
		if (fileHeader == null) {
			throw new ZipException("FileHeader is null, cannot get InputStream");
		}
		
		checkZipModel();
		
		if (zipModel == null) {
			throw new ZipException("zip model is null, cannot get inputstream");
		}
		
		Unzip unzip = new Unzip(zipModel);
		return unzip.getInputStream(fileHeader);
	}
	
	/**
	 * Checks to see if the input zip file is a valid zip file. This method
	 * will try to read zip headers. If headers are read successfully, this
	 * method returns true else false 
	 * @return boolean
	 * @since 1.2.3
	 */
	public boolean isValidZipFile() {
		try {
			readZipInfo();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Returns the full file path+names of all split zip files 
	 * in an ArrayList. For example: If a split zip file(abc.zip) has a 10 split parts
	 * this method returns an array list with path + "abc.z01", path + "abc.z02", etc.
	 * Returns null if the zip file does not exist
	 * @return ArrayList of Strings
	 * @throws ZipException
	 */
	public ArrayList getSplitZipFiles() throws ZipException {
		checkZipModel();
		return Zip4jUtil.getSplitZipFiles(zipModel);
	}
	
	public ProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	public boolean isRunInThread() {
		return runInThread;
	}

	public void setRunInThread(boolean runInThread) {
		this.runInThread = runInThread;
	}
	
	/**
	 * Returns the File object of the zip file 
	 * @return File
	 */
	public File getFile() {
		return new File(this.file);
	}
}
