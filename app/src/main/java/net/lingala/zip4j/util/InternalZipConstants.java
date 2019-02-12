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

public interface InternalZipConstants {
	
	/*
     * Header signatures
     */
	// Whenever a new Signature is added here, make sure to add it
	// in Zip4jUtil.getAllHeaderSignatures()
    static long LOCSIG = 0x04034b50L;	// "PK\003\004"
    static long EXTSIG = 0x08074b50L;	// "PK\007\008"
    static long CENSIG = 0x02014b50L;	// "PK\001\002"
    static long ENDSIG = 0x06054b50L;	// "PK\005\006"
    static long DIGSIG = 0x05054b50L;
    static long ARCEXTDATREC = 0x08064b50L;
    static long SPLITSIG = 0x08074b50L;
    static long ZIP64ENDCENDIRLOC = 0x07064b50L;
    static long ZIP64ENDCENDIRREC = 0x06064b50;
    static int EXTRAFIELDZIP64LENGTH = 0x0001;
    static int AESSIG = 0x9901;

    /*
     * Header sizes in bytes (including signatures)
     */
    static final int LOCHDR = 30;	// LOC header size
    static final int EXTHDR = 16;	// EXT header size
    static final int CENHDR = 46;	// CEN header size
    static final int ENDHDR = 22;	// END header size

    /*
     * Local file (LOC) header field offsets
     */
    static final int LOCVER = 4;	// version needed to extract
    static final int LOCFLG = 6;	// general purpose bit flag
    static final int LOCHOW = 8;	// compression method
    static final int LOCTIM = 10;	// modification time
    static final int LOCCRC = 14;	// uncompressed file crc-32 value
    static final int LOCSIZ = 18;	// compressed size
    static final int LOCLEN = 22;	// uncompressed size
    static final int LOCNAM = 26;	// filename length
    static final int LOCEXT = 28;	// extra field length

    /*
     * Extra local (EXT) header field offsets
     */
    static final int EXTCRC = 4;	// uncompressed file crc-32 value
    static final int EXTSIZ = 8;	// compressed size
    static final int EXTLEN = 12;	// uncompressed size

    /*
     * Central directory (CEN) header field offsets
     */
    static final int CENVEM = 4;	// version made by
    static final int CENVER = 6;	// version needed to extract
    static final int CENFLG = 8;	// encrypt, decrypt flags
    static final int CENHOW = 10;	// compression method
    static final int CENTIM = 12;	// modification time
    static final int CENCRC = 16;	// uncompressed file crc-32 value
    static final int CENSIZ = 20;	// compressed size
    static final int CENLEN = 24;	// uncompressed size
    static final int CENNAM = 28;	// filename length
    static final int CENEXT = 30;	// extra field length
    static final int CENCOM = 32;	// comment length
    static final int CENDSK = 34;	// disk number start
    static final int CENATT = 36;	// internal file attributes
    static final int CENATX = 38;	// external file attributes
    static final int CENOFF = 42;	// LOC header offset

    /*
     * End of central directory (END) header field offsets
     */
    static final int ENDSUB = 8;	// number of entries on this disk
    static final int ENDTOT = 10;	// total number of entries
    static final int ENDSIZ = 12;	// central directory size in bytes
    static final int ENDOFF = 16;	// offset of first CEN header
    static final int ENDCOM = 20;	// zip file comment length
    
    static final int STD_DEC_HDR_SIZE = 12;
    
    //AES Constants
    static final int AES_AUTH_LENGTH = 10;
    static final int AES_BLOCK_SIZE = 16;
    
    static final int MIN_SPLIT_LENGTH = 65536;
    
    static final long ZIP_64_LIMIT = 4294967295L;
	
	public static String OFFSET_CENTRAL_DIR = "offsetCentralDir";
	
	public static final String VERSION = "1.3.3";
	
	public static final int MODE_ZIP = 1;
	
	public static final int MODE_UNZIP = 2;
	
	public static final String WRITE_MODE = "rw";
	
	public static final String READ_MODE = "r";
	
	public static final int BUFF_SIZE = 1024 * 4;
	
	public static final int FILE_MODE_NONE = 0;
	
	public static final int FILE_MODE_READ_ONLY = 1;
	
	public static final int FILE_MODE_HIDDEN = 2;
	
	public static final int FILE_MODE_ARCHIVE = 32;
	
	public static final int FILE_MODE_READ_ONLY_HIDDEN = 3;
	
	public static final int FILE_MODE_READ_ONLY_ARCHIVE = 33;
	
	public static final int FILE_MODE_HIDDEN_ARCHIVE = 34;
	
	public static final int FILE_MODE_READ_ONLY_HIDDEN_ARCHIVE = 35;
	
	public static final int FILE_MODE_SYSTEM = 38;
	
	public static final int FOLDER_MODE_NONE = 16;
	
	public static final int FOLDER_MODE_HIDDEN = 18;

	public static final int FOLDER_MODE_ARCHIVE = 48;
	
	public static final int FOLDER_MODE_HIDDEN_ARCHIVE = 50;
	
	// Update local file header constants
	// This value holds the number of bytes to skip from
	// the offset of start of local header
	public static final int UPDATE_LFH_CRC = 14;
	
	public static final int UPDATE_LFH_COMP_SIZE = 18;
	
	public static final int UPDATE_LFH_UNCOMP_SIZE = 22;
	
	public static final int LIST_TYPE_FILE = 1;
	
	public static final int LIST_TYPE_STRING = 2;
	
	public static final int UFT8_NAMES_FLAG = 1 << 11;
	
	public static final String CHARSET_UTF8 = "UTF8";
	
	public static final String CHARSET_CP850 = "Cp850";
	
	public static final String CHARSET_COMMENTS_DEFAULT = "windows-1254";
	
	public static final String CHARSET_DEFAULT = System.getProperty("file.encoding");
	
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
	public static final String ZIP_FILE_SEPARATOR = "/";
	
	public static final String THREAD_NAME = "Zip4j";
	
	public static final int MAX_ALLOWED_ZIP_COMMENT_LENGTH = 0xFFFF;
}
