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

import java.io.FileOutputStream;

import net.lingala.zip4j.crypto.IDecrypter;
import net.lingala.zip4j.unzip.UnzipEngine;

public class UnzipEngineParameters {
	
	private ZipModel zipModel;
	
	private FileHeader fileHeader;
	
	private LocalFileHeader localFileHeader;
	
	private IDecrypter iDecryptor;
	
	private FileOutputStream outputStream;
	
	private UnzipEngine unzipEngine;

	public ZipModel getZipModel() {
		return zipModel;
	}

	public void setZipModel(ZipModel zipModel) {
		this.zipModel = zipModel;
	}

	public FileHeader getFileHeader() {
		return fileHeader;
	}

	public void setFileHeader(FileHeader fileHeader) {
		this.fileHeader = fileHeader;
	}

	public LocalFileHeader getLocalFileHeader() {
		return localFileHeader;
	}

	public void setLocalFileHeader(LocalFileHeader localFileHeader) {
		this.localFileHeader = localFileHeader;
	}

	public IDecrypter getIDecryptor() {
		return iDecryptor;
	}

	public void setIDecryptor(IDecrypter decrypter) {
		iDecryptor = decrypter;
	}

	public FileOutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(FileOutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public UnzipEngine getUnzipEngine() {
		return unzipEngine;
	}

	public void setUnzipEngine(UnzipEngine unzipEngine) {
		this.unzipEngine = unzipEngine;
	}
	
	
	
}
