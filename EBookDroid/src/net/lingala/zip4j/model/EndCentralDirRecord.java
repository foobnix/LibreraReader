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

public class EndCentralDirRecord {
	
	private long signature;
	
	private int noOfThisDisk;
	
	private int noOfThisDiskStartOfCentralDir;
	
	private int totNoOfEntriesInCentralDirOnThisDisk;
	
	private int totNoOfEntriesInCentralDir;
	
	private int sizeOfCentralDir;
	
	private long offsetOfStartOfCentralDir;
	
	private int commentLength;
	
	private String comment;
	
	private byte[] commentBytes;

	public long getSignature() {
		return signature;
	}

	public void setSignature(long signature) {
		this.signature = signature;
	}

	public int getNoOfThisDisk() {
		return noOfThisDisk;
	}

	public void setNoOfThisDisk(int noOfThisDisk) {
		this.noOfThisDisk = noOfThisDisk;
	}

	public int getNoOfThisDiskStartOfCentralDir() {
		return noOfThisDiskStartOfCentralDir;
	}

	public void setNoOfThisDiskStartOfCentralDir(int noOfThisDiskStartOfCentralDir) {
		this.noOfThisDiskStartOfCentralDir = noOfThisDiskStartOfCentralDir;
	}

	public int getTotNoOfEntriesInCentralDirOnThisDisk() {
		return totNoOfEntriesInCentralDirOnThisDisk;
	}

	public void setTotNoOfEntriesInCentralDirOnThisDisk(
			int totNoOfEntriesInCentralDirOnThisDisk) {
		this.totNoOfEntriesInCentralDirOnThisDisk = totNoOfEntriesInCentralDirOnThisDisk;
	}

	public int getTotNoOfEntriesInCentralDir() {
		return totNoOfEntriesInCentralDir;
	}

	public void setTotNoOfEntriesInCentralDir(int totNoOfEntrisInCentralDir) {
		this.totNoOfEntriesInCentralDir = totNoOfEntrisInCentralDir;
	}

	public int getSizeOfCentralDir() {
		return sizeOfCentralDir;
	}

	public void setSizeOfCentralDir(int sizeOfCentralDir) {
		this.sizeOfCentralDir = sizeOfCentralDir;
	}

	public long getOffsetOfStartOfCentralDir() {
		return offsetOfStartOfCentralDir;
	}

	public void setOffsetOfStartOfCentralDir(long offSetOfStartOfCentralDir) {
		this.offsetOfStartOfCentralDir = offSetOfStartOfCentralDir;
	}

	public int getCommentLength() {
		return commentLength;
	}

	public void setCommentLength(int commentLength) {
		this.commentLength = commentLength;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public byte[] getCommentBytes() {
		return commentBytes;
	}

	public void setCommentBytes(byte[] commentBytes) {
		this.commentBytes = commentBytes;
	}
	
}
