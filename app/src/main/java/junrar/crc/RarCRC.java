/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 29.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *  
 * the unrar licence applies to all junrar source and binary distributions 
 * you are not allowed to use this source to re-create the RAR compression algorithm
 * 
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;" 
 */
package junrar.crc;


/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class RarCRC {
	
	private final static int crcTab[];
    static {
		crcTab = new int[256];
		for (int i = 0; i < 256; i++) {
			int c = i;
			for (int j = 0; j < 8; j++){
				if ((c & 1) !=0) {
					c >>>= 1;
					c ^= 0xEDB88320;
				}
                else{
					c >>>= 1;
				}
			}
			crcTab[i] = c;
		}
    }

	private RarCRC() {
	}

	public static int checkCrc(int startCrc, byte[] data, int offset,
            int count) {
		int size = Math.min(data.length-offset,count);
		// #if defined(LITTLE_ENDIAN) && defined(PRESENT_INT32) &&
		// defined(ALLOW_NOT_ALIGNED_INT)
		/*
		for (int i = 0; (0 < size) && i < data.length - 8
				&& ((data[i + 8] & 7) != 0); i++) {
			startCrc = crcTab[(short) (startCrc ^ data[i]) & 0x00FF] ^ (startCrc >>> 8);
			size--;
		}
		
		for (int i = 0; size >= 8; i += 8) {
			startCrc ^= data[i + 0] << 24;
			startCrc ^= data[i + 1] << 16;
			startCrc ^= data[i + 2] << 8;
			startCrc ^= data[i + 3];

			startCrc = crcTab[(short) startCrc & 0x00FF] ^ (startCrc >>> 8);
			startCrc = crcTab[(short) startCrc & 0x00FF] ^ (startCrc >>> 8);
			startCrc = crcTab[(short) startCrc & 0x00FF] ^ (startCrc >>> 8);
			startCrc = crcTab[(short) startCrc & 0x00FF] ^ (startCrc >>> 8);

			startCrc ^= data[i + 4] << 24;
			startCrc ^= data[i + 5] << 16;
			startCrc ^= data[i + 6] << 8;
			startCrc ^= data[i + 7];
			startCrc = crcTab[(short) startCrc & 0x00FF] ^ (startCrc >>> 8);
			startCrc = crcTab[(short) startCrc & 0x00FF] ^ (startCrc >>> 8);
			startCrc = crcTab[(short) startCrc & 0x00FF] ^ (startCrc >>> 8);
			startCrc = crcTab[(short) startCrc & 0x00FF] ^ (startCrc >>> 8);
			size -= 8;
		}*/
		
		for (int i = 0; i < size; i++)
		{
/*
			// (byte)(StartCRC^Data[I])
			int pos = 0; // pos=0x00000000
			pos |= startCrc; // pos=ffffffff
			
			pos ^= data[i]; // data[0]=0x73=115dec --> pos=140
			System.out.println(Integer.toHexString(pos));
			
			// Only last 8 bit because CRCtab has length 256
			pos = pos & 0x000000FF;
			System.out.println("pos:"+pos);
			//startCrc >>>= 8;
			
			
			//StartCRC>>8
			int temp =0;
			temp|=startCrc;
			temp >>>= 8;
			System.out.println("temp:"+Integer.toHexString(temp));
			
			
			startCrc = (crcTab[pos]^temp);
			System.out.println("--"+Integer.toHexString(startCrc));*/
			
			startCrc=(crcTab[((int)((int)startCrc ^
                    (int)data[offset+i]))&0xff]^(startCrc>>>8));
			
			//System.out.println(Integer.toHexString(startCrc));
			
			// Original code:
			//StartCRC=CRCTab[(byte)(StartCRC^Data[I])]^(StartCRC>>8);
		}
		return (startCrc);
	}

	public static short checkOldCrc(short startCrc, byte[] data, int count) {
        int n = Math.min(data.length, count);
		for (int i = 0; i < n; i++) {
			startCrc = (short) ((short) (startCrc + (short) (data[i]&0x00ff)) & -1);
			startCrc = (short) (((startCrc << 1) | (startCrc >>> 15)) & -1);
		}
		return (startCrc);
	}

//	public static void main(String[] args)
//	{
//		RarCRC rc = new RarCRC();
//		//byte[] data = { 0x72, 0x21, 0x1A, 0x07, 0x00};
//		
//		byte[] data = {0x73 ,0x00 ,0x00 ,0x0D ,0x00 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00};
//		
//		int crc = 0x90CF;
//		
//
//		int result = rc.checkCrc(0xFFFFffff, data,0,data.length);
//		System.out.println("3: "+Integer.toHexString(~result&0xffff));
//		
//	}
	
}
