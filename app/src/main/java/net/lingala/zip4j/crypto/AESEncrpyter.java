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

import java.util.Random;

import net.lingala.zip4j.crypto.PBKDF2.MacBasedPRF;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Engine;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Parameters;
import net.lingala.zip4j.crypto.engine.AESEngine;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jConstants;

public class AESEncrpyter implements IEncrypter {
	
	private char[] password;
	private int keyStrength;
	private AESEngine aesEngine;
	private MacBasedPRF mac;
	
	private int KEY_LENGTH;
	private int MAC_LENGTH;
	private int SALT_LENGTH;
	private final int PASSWORD_VERIFIER_LENGTH = 2;
	
	private byte[] aesKey;
	private byte[] macKey;
	private byte[] derivedPasswordVerifier;
	private byte[] saltBytes;
	
	private boolean finished;
	
	private int nonce = 1;
	private int loopCount = 0;
	
	private byte[] iv;
	private byte[] counterBlock;
	
	public AESEncrpyter(char[] password, int keyStrength) throws ZipException {
		if (password == null || password.length == 0) {
			throw new ZipException("input password is empty or null in AES encrypter constructor");
		}
		if (keyStrength != Zip4jConstants.AES_STRENGTH_128 && 
				keyStrength != Zip4jConstants.AES_STRENGTH_256) {
			throw new ZipException("Invalid key strength in AES encrypter constructor");
		}
		
		this.password = password;
		this.keyStrength = keyStrength;
		this.finished = false;
		counterBlock = new byte[InternalZipConstants.AES_BLOCK_SIZE];
		iv = new byte[InternalZipConstants.AES_BLOCK_SIZE];
		init();
	}
	
	private void init() throws ZipException {
		switch (keyStrength) {
		case Zip4jConstants.AES_STRENGTH_128:
			KEY_LENGTH = 16;
			MAC_LENGTH = 16;
			SALT_LENGTH = 8;
			break;
		case Zip4jConstants.AES_STRENGTH_256:
			KEY_LENGTH = 32;
			MAC_LENGTH = 32;
			SALT_LENGTH = 16;
			break;
		default:
			throw new ZipException("invalid aes key strength, cannot determine key sizes");
		}
		
		saltBytes = generateSalt(SALT_LENGTH);
		byte[] keyBytes = deriveKey(saltBytes, password);
		
		if (keyBytes == null || keyBytes.length != (KEY_LENGTH + MAC_LENGTH + PASSWORD_VERIFIER_LENGTH)) {
			throw new ZipException("invalid key generated, cannot decrypt file");
		}
		
		aesKey = new byte[KEY_LENGTH];
		macKey = new byte[MAC_LENGTH];
		derivedPasswordVerifier = new byte[PASSWORD_VERIFIER_LENGTH];
		
		System.arraycopy(keyBytes, 0, aesKey, 0, KEY_LENGTH);
		System.arraycopy(keyBytes, KEY_LENGTH, macKey, 0, MAC_LENGTH);
		System.arraycopy(keyBytes, KEY_LENGTH + MAC_LENGTH, derivedPasswordVerifier, 0, PASSWORD_VERIFIER_LENGTH);
		
		aesEngine = new AESEngine(aesKey);
		mac = new MacBasedPRF("HmacSHA1");
		mac.init(macKey);
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
	
	public int encryptData(byte[] buff) throws ZipException {
		
		if (buff == null) {
			throw new ZipException("input bytes are null, cannot perform AES encrpytion");
		}
		return encryptData(buff, 0, buff.length);
	}
	
	public int encryptData(byte[] buff, int start, int len) throws ZipException {
		
		if (finished) {
			// A non 16 byte block has already been passed to encrypter
			// non 16 byte block should be the last block of compressed data in AES encryption
			// any more encryption will lead to corruption of data
			throw new ZipException("AES Encrypter is in finished state (A non 16 byte block has already been passed to encrypter)");
		}
		
		if (len%16!=0) {
			this.finished = true;
		}
		
		for (int j = start; j < (start + len); j += InternalZipConstants.AES_BLOCK_SIZE) {
			loopCount = (j + InternalZipConstants.AES_BLOCK_SIZE <= (start + len)) ? 
					InternalZipConstants.AES_BLOCK_SIZE : ((start + len) - j);
			
			Raw.prepareBuffAESIVBytes(iv, nonce, InternalZipConstants.AES_BLOCK_SIZE);
			aesEngine.processBlock(iv, counterBlock);
			
			for (int k = 0; k < loopCount; k++) {
				buff[j + k] = (byte)(buff[j + k] ^ counterBlock[k]);
			}
			
			mac.update(buff, j, loopCount);
			nonce++;
		}
		
		return len;
	}
	
	private static byte[] generateSalt(int size) throws ZipException {
		
		if (size != 8 && size != 16) {
			throw new ZipException("invalid salt size, cannot generate salt");
		}
		
		int rounds = 0;
		
		if (size == 8)
			rounds = 2;
		if (size == 16)
			rounds = 4;
		
		byte[] salt = new byte[size];
		for( int j = 0; j < rounds; j++ ) {
			Random rand = new Random();
			int i = rand.nextInt();
			salt[0+j*4] = (byte)(i>>24);
			salt[1+j*4] = (byte)(i>>16);
			salt[2+j*4] = (byte)(i>>8);
			salt[3+j*4] = (byte)i;
		}
		return salt;
	}
	
	public byte[] getFinalMac() {
		byte[] rawMacBytes = mac.doFinal();
		byte[] macBytes = new byte[10];
		System.arraycopy(rawMacBytes, 0, macBytes, 0, 10);
		return macBytes;
	}

	public byte[] getDerivedPasswordVerifier() {
		return derivedPasswordVerifier;
	}

	public void setDerivedPasswordVerifier(byte[] derivedPasswordVerifier) {
		this.derivedPasswordVerifier = derivedPasswordVerifier;
	}

	public byte[] getSaltBytes() {
		return saltBytes;
	}

	public void setSaltBytes(byte[] saltBytes) {
		this.saltBytes = saltBytes;
	}
	
	public int getSaltLength() {
		return SALT_LENGTH;
	}
	
	public int getPasswordVeriifierLength() {
		return PASSWORD_VERIFIER_LENGTH;
	}
}
