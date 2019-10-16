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

import java.util.List;

public class ZipModel implements Cloneable {
	
	private List localFileHeaderList;
	
	private List dataDescriptorList;
	
	private ArchiveExtraDataRecord archiveExtraDataRecord;
	
	private CentralDirectory centralDirectory;
	
	private EndCentralDirRecord endCentralDirRecord;
	
	private Zip64EndCentralDirLocator zip64EndCentralDirLocator;
	
	private Zip64EndCentralDirRecord zip64EndCentralDirRecord;
	
	private boolean splitArchive;
	
	private long splitLength;
	
	private String zipFile;

	private boolean isZip64Format;
	
	private boolean isNestedZipFile;
	
	private long start;
	
	private long end;
	
	private String fileNameCharset;
	
	public ZipModel() {
		splitLength = -1;
	}
	
	public List getLocalFileHeaderList() {
		return localFileHeaderList;
	}

	public void setLocalFileHeaderList(List localFileHeaderList) {
		this.localFileHeaderList = localFileHeaderList;
	}

	public List getDataDescriptorList() {
		return dataDescriptorList;
	}

	public void setDataDescriptorList(List dataDescriptorList) {
		this.dataDescriptorList = dataDescriptorList;
	}

	public CentralDirectory getCentralDirectory() {
		return centralDirectory;
	}

	public void setCentralDirectory(CentralDirectory centralDirectory) {
		this.centralDirectory = centralDirectory;
	}

	public EndCentralDirRecord getEndCentralDirRecord() {
		return endCentralDirRecord;
	}

	public void setEndCentralDirRecord(EndCentralDirRecord endCentralDirRecord) {
		this.endCentralDirRecord = endCentralDirRecord;
	}

	public ArchiveExtraDataRecord getArchiveExtraDataRecord() {
		return archiveExtraDataRecord;
	}

	public void setArchiveExtraDataRecord(
			ArchiveExtraDataRecord archiveExtraDataRecord) {
		this.archiveExtraDataRecord = archiveExtraDataRecord;
	}

	public boolean isSplitArchive() {
		return splitArchive;
	}

	public void setSplitArchive(boolean splitArchive) {
		this.splitArchive = splitArchive;
	}

	public String getZipFile() {
		return zipFile;
	}

	public void setZipFile(String zipFile) {
		this.zipFile = zipFile;
	}

	public Zip64EndCentralDirLocator getZip64EndCentralDirLocator() {
		return zip64EndCentralDirLocator;
	}

	public void setZip64EndCentralDirLocator(
			Zip64EndCentralDirLocator zip64EndCentralDirLocator) {
		this.zip64EndCentralDirLocator = zip64EndCentralDirLocator;
	}

	public Zip64EndCentralDirRecord getZip64EndCentralDirRecord() {
		return zip64EndCentralDirRecord;
	}

	public void setZip64EndCentralDirRecord(
			Zip64EndCentralDirRecord zip64EndCentralDirRecord) {
		this.zip64EndCentralDirRecord = zip64EndCentralDirRecord;
	}

	public boolean isZip64Format() {
		return isZip64Format;
	}

	public void setZip64Format(boolean isZip64Format) {
		this.isZip64Format = isZip64Format;
	}

	public boolean isNestedZipFile() {
		return isNestedZipFile;
	}

	public void setNestedZipFile(boolean isNestedZipFile) {
		this.isNestedZipFile = isNestedZipFile;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public long getSplitLength() {
		return splitLength;
	}

	public void setSplitLength(long splitLength) {
		this.splitLength = splitLength;
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public String getFileNameCharset() {
		return fileNameCharset;
	}

	public void setFileNameCharset(String fileNameCharset) {
		this.fileNameCharset = fileNameCharset;
	}
	
}
