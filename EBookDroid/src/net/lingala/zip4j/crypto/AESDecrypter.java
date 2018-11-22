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

package net.lingala.zip4j.crypto;

import java.util.Arrays;

import net.lingala.zip4j.crypto.PBKDF2.MacBasedPRF;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Engine;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Parameters;
import net.lingala.zip4j.crypto.engine.AESEngine;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.exception.ZipExceptionConstants;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jConstants;

public class AESDecrypter implements IDecrypter {
	
	private LocalFileHeader localFileHeader;
	private AESEngine aesEngine;
	private MacBasedPRF mac;
	
	private final int PASSWORD_VERIFIER_LENGTH = 2;
	private int KEY_LENGTH;
	private int MAC_LENGTH;
	private int SALT_LENGTH;
	
	private byte[] aesKey;
	private byte[] macKey;
	private byte[] derivedPasswordVerifier;
	private byte[] storedMac;
	
	private int nonce = 1;
	private byte[] iv;
	private byte[] counterBlock;
	private int loopCount = 0;
	
	public AESDecrypter(LocalFileHeader localFileHeader, 
			byte[] salt, byte[] passwordVerifier) throws ZipException {
		
		if (localFileHeader == null) {
			throw new ZipException("one of the input parameters is null in AESDecryptor Constructor");
		}
		
		this.localFileHeader = localFileHeader;
		this.storedMac = null;
		iv = new byte[InternalZipConstants.AES_BLOCK_SIZE];
		counterBlock = new byte[InternalZipConstants.AES_BLOCK_SIZE];
		init(salt, passwordVerifier);
	}
	
	private void init(byte[] salt, byte[] passwordVerifier) throws ZipException {
		if (localFileHeader == null) {
			throw new ZipException("invalid file header in init method of AESDecryptor");
		}
		
		AESExtraDataRecord aesExtraDataRecord = localFileHeader.getAesExtraDataRecord();
		if (aesExtraDataRecord == null) {
			throw new ZipException("invalid aes extra data record - in init method of AESDecryptor");
		}
		
		switch (aesExtraDataRecord.getAesStrength()) {
		case Zip4jConstants.AES_STRENGTH_128:
			KEY_LENGTH = 16;
			MAC_LENGTH = 16;
			SALT_LENGTH = 8;
			break;
		case Zip4jConstants.AES_STRENGTH_192:
			KEY_LENGTH = 24;
			MAC_LENGTH = 24;
			SALT_LENGTH = 12;
			break;
		case Zip4jConstants.AES_STRENGTH_256:
			KEY_LENGTH = 32;
			MAC_LENGTH = 32;
			SALT_LENGTH = 16;
			break;
		default:
			throw new ZipException("invalid aes key strength for file: " + localFileHeader.getFileName());
		}
		
		if (localFileHeader.getPassword() == null || localFileHeader.getPassword().length <= 0) {
			throw new ZipException("empty or null password provided for AES Decryptor");
		}
		
		byte[] derivedKey = deriveKey(salt, localFileHeader.getPassword());
		if (derivedKey == null || 
				derivedKey.length != (KEY_LENGTH + MAC_LENGTH + PASSWORD_VERIFIER_LENGTH)) {
			throw new ZipException("invalid derived key");
		}
		
		aesKey = new byte[KEY_LENGTH];
		macKey = new byte[MAC_LENGTH];
		derivedPasswordVerifier = new byte[PASSWORD_VERIFIER_LENGTH];
		
		System.arraycopy(derivedKey, 0, aesKey, 0, KEY_LENGTH);
		System.arraycopy(derivedKey, KEY_LENGTH, macKey, 0, MAC_LENGTH);
		System.arraycopy(derivedKey, KEY_LENGTH + MAC_LENGTH, derivedPasswordVerifier, 0, PASSWORD_VERIFIER_LENGTH);
		
		if (derivedPasswordVerifier == null) {
			throw new ZipException("invalid derived password verifier for AES");
		}
		
		if (!Arrays.equals(passwordVerifier, derivedPasswordVerifier)) {
			throw new ZipException("Wrong Password for file: " + localFileHeader.getFileName(), ZipExceptionConstants.WRONG_PASSWORD);
		}
		
		aesEngine = new AESEngine(aesKey);
		mac = new MacBasedPRF("HmacSHA1");
		mac.init(macKey);
	}
	
	public int decryptData(byte[] buff, int start, int len) throws ZipException {
		
		if (aesEngine == null) {
			throw new ZipException("AES not initialized properly");
		}
		
		try {
			
			for (int j = start; j < (start + len); j += InternalZipConstants.AES_BLOCK_SIZE) {
				loopCount = (j + InternalZipConstants.AES_BLOCK_SIZE <= (start + len)) ? 
						InternalZipConstants.AES_BLOCK_SIZE : ((start + len) - j);
				
				mac.update(buff, j, loopCount);
				Raw.prepareBuffAESIVBytes(iv, nonce, InternalZipConstants.AES_BLOCK_SIZE);
				aesEngine.processBlock(iv, counterBlock);
				
				for (int k = 0; k < loopCount; k++) {
					buff[j + k] = (byte)(buff[j + k] ^ counterBlock[k]);
				}
				
				nonce++;
			}
			
			return len;
			
		} catch (ZipException e) {
			throw e;
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	public int decryptData(byte[] buff) throws ZipException {
		return decryptData(buff, 0, buff.length);
	}
	
	private byte[] deriveKey(byte[] salt, char[] password) throws ZipException {
		try {
			PBKDF2Parameters p = new PBKDF2Parameters("HmacSHA1", "ISO-8859-1",
	                    salt, 1000);
	        PBKDF2Engine e = new PBKDF2Engine(p);
	        byte[] derivedKey = e.deriveKey(password, KEY_LENGTH + MAC_LENGTH + PASSWORD_VERIFIER_LENGTH);
			return derivedKey;
		} catch (Exception e) {
			throw new ZipException(e);
		}
	}
	
	public int getPasswordVerifierLength() {
		return PASSWORD_VERIFIER_LENGTH;
	}
	
	public int getSaltLength() {
		return SALT_LENGTH;
	}
	
	public byte[] getCalculatedAuthenticationBytes() {
		return mac.doFinal();
	}
	
	public void setStoredMac(byte[] storedMac) {
		this.storedMac = storedMac;
	}

	public byte[] getStoredMac() {
		return storedMac;
	}

//	public byte[] getStoredMac() throws ZipException {
//		if (raf == null) {
//			throw new ZipException("attempting to read MAC on closed file handle");
//		}
//		
//		try {
//			byte[] storedMacBytes = new byte[InternalZipConstants.AES_AUTH_LENGTH];
//			int bytesRead = raf.read(storedMacBytes);
//			if (bytesRead != InternalZipConstants.AES_AUTH_LENGTH) {
//				if (zipModel.isSplitArchive()) {
////					unzipEngine.startNextSplitFile();
//					if (bytesRead == -1) bytesRead = 0;
//					int newlyRead = raf.read(storedMacBytes, bytesRead, InternalZipConstants.AES_AUTH_LENGTH - bytesRead);
//					bytesRead += newlyRead;
//					if (bytesRead != InternalZipConstants.AES_AUTH_LENGTH) {
//						throw new ZipException("invalid number of bytes read for stored MAC after starting split file");
//					}
//				} else {
//					throw new ZipException("invalid number of bytes read for stored MAC");
//				}
//			}
//			return storedMacBytes;
//		} catch (IOException e) {
//			throw new ZipException(e);
//		} catch (Exception e) {
//			throw new ZipException(e);
//		}
//		
//	}
}
