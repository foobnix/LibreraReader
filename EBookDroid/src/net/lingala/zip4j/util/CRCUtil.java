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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;

public class CRCUtil {
	
	private static final int BUF_SIZE = 1 << 14; //16384
	
	public static long computeFileCRC(String inputFile) throws ZipException {
		return computeFileCRC(inputFile, null);
	}
	
	/**
	 * Calculates CRC of a file
	 * @param inputFile - file for which crc has to be calculated
	 * @return crc of the file
	 * @throws ZipException
	 */
	public static long computeFileCRC(String inputFile, ProgressMonitor progressMonitor) throws ZipException {
		
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(inputFile)) {
			throw new ZipException("input file is null or empty, cannot calculate CRC for the file");
		}
		InputStream inputStream = null;
		try {
			Zip4jUtil.checkFileReadAccess(inputFile);
			
			inputStream = new FileInputStream(new File(inputFile));
			
			byte[] buff = new byte[BUF_SIZE];
			int readLen = -2;
			CRC32 crc32 = new CRC32();
			
			while ((readLen = inputStream.read(buff)) != -1) {
				crc32.update(buff, 0, readLen);
				if (progressMonitor != null) {
					progressMonitor.updateWorkCompleted(readLen);
					if (progressMonitor.isCancelAllTasks()) {
						progressMonitor.setResult(ProgressMonitor.RESULT_CANCELLED);
						progressMonitor.setState(ProgressMonitor.STATE_READY);
						return 0;
					}
				}
			}
			
			return crc32.getValue();
		} catch (IOException e) {
			throw new ZipException(e);
		} catch (Exception e) {
			throw new ZipException(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					throw new ZipException("error while closing the file after calculating crc");
				}
			}
		}
	}
	
}
