/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 21.06.2007
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
package junrar.unpack;

import java.io.IOException;
import java.util.Arrays;

import junrar.exception.RarException;
import junrar.unpack.decode.Compress;
import junrar.unpack.vm.BitInput;


/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public abstract class Unpack15 extends BitInput
{

	protected int readBorder;
	
	protected boolean suspended;

	protected boolean unpAllBuf;

	protected ComprDataIO unpIO;

	protected boolean unpSomeRead;

	protected int readTop;

	protected long destUnpSize;

	protected byte[] window;

	protected int[] oldDist = new int[4];

	protected int unpPtr, wrPtr;

	protected int oldDistPtr;

	protected int[] ChSet = new int[256], ChSetA = new int[256],
			ChSetB = new int[256], ChSetC = new int[256];

	protected int[] Place = new int[256], PlaceA = new int[256],
			PlaceB = new int[256], PlaceC = new int[256];

	protected int[] NToPl = new int[256], NToPlB = new int[256],
			NToPlC = new int[256];

	protected int FlagBuf, AvrPlc, AvrPlcB, AvrLn1, AvrLn2, AvrLn3;

	protected int Buf60, NumHuf, StMode, LCount, FlagsCnt;

	protected int Nhfb, Nlzb, MaxDist3;

	protected int lastDist, lastLength;

	private static final int STARTL1 = 2;

	private static int DecL1[] = { 0x8000, 0xa000, 0xc000, 0xd000, 0xe000,
			0xea00, 0xee00, 0xf000, 0xf200, 0xf200, 0xffff };

	private static int PosL1[] = { 0, 0, 0, 2, 3, 5, 7, 11, 16, 20, 24, 32, 32 };

	private static final int STARTL2 = 3;

	private static int DecL2[] = { 0xa000, 0xc000, 0xd000, 0xe000, 0xea00,
			0xee00, 0xf000, 0xf200, 0xf240, 0xffff };

	private static int PosL2[] = { 0, 0, 0, 0, 5, 7, 9, 13, 18, 22, 26, 34, 36 };

	private static final int STARTHF0 = 4;

	private static int DecHf0[] = { 0x8000, 0xc000, 0xe000, 0xf200, 0xf200,
			0xf200, 0xf200, 0xf200, 0xffff };

	private static int PosHf0[] = { 0, 0, 0, 0, 0, 8, 16, 24, 33, 33, 33, 33,
			33 };

	private static final int STARTHF1 = 5;

	private static int DecHf1[] = { 0x2000, 0xc000, 0xe000, 0xf000, 0xf200,
			0xf200, 0xf7e0, 0xffff };

	private static int PosHf1[] = { 0, 0, 0, 0, 0, 0, 4, 44, 60, 76, 80, 80,
			127 };

	private static final int STARTHF2 = 5;

	private static int DecHf2[] = { 0x1000, 0x2400, 0x8000, 0xc000, 0xfa00,
			0xffff, 0xffff, 0xffff };

	private static int PosHf2[] = { 0, 0, 0, 0, 0, 0, 2, 7, 53, 117, 233, 0, 0 };

	private static final int STARTHF3 = 6;

	private static int DecHf3[] = { 0x800, 0x2400, 0xee00, 0xfe80, 0xffff,
			0xffff, 0xffff };

	private static int PosHf3[] = { 0, 0, 0, 0, 0, 0, 0, 2, 16, 218, 251, 0, 0 };

	private static final int STARTHF4 = 8;

	private static int DecHf4[] = { 0xff00, 0xffff, 0xffff, 0xffff, 0xffff,
			0xffff };

	private static int PosHf4[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 255, 0, 0, 0 };

	static int ShortLen1[] = { 1, 3, 4, 4, 5, 6, 7, 8, 8, 4, 4, 5, 6, 6, 4, 0 };

	static int ShortXor1[] = { 0, 0xa0, 0xd0, 0xe0, 0xf0, 0xf8, 0xfc, 0xfe,
			0xff, 0xc0, 0x80, 0x90, 0x98, 0x9c, 0xb0 };

	static int ShortLen2[] = { 2, 3, 3, 3, 4, 4, 5, 6, 6, 4, 4, 5, 6, 6, 4, 0 };

	static int ShortXor2[] = { 0, 0x40, 0x60, 0xa0, 0xd0, 0xe0, 0xf0, 0xf8,
			0xfc, 0xc0, 0x80, 0x90, 0x98, 0x9c, 0xb0 };

	protected abstract void unpInitData(boolean solid);

	protected void unpack15(boolean solid) throws IOException, RarException
	{
		if (suspended) {
			unpPtr = wrPtr;
		} else {
			unpInitData(solid);
			oldUnpInitData(solid);
			unpReadBuf();
			if (!solid) {
				initHuff();
				unpPtr = 0;
			} else {
				unpPtr = wrPtr;
			}
			--destUnpSize;
		}
		if (destUnpSize >= 0) {
			getFlagsBuf();
			FlagsCnt = 8;
		}

		while (destUnpSize >= 0) {
			unpPtr &= Compress.MAXWINMASK;

			if (inAddr > readTop - 30 && !unpReadBuf()) {
				break;
			}
			if (((wrPtr - unpPtr) & Compress.MAXWINMASK) < 270
					&& wrPtr != unpPtr) {
				oldUnpWriteBuf();
				if (suspended) {
					return;
				}
			}
			if (StMode != 0) {
				huffDecode();
				continue;
			}

			if (--FlagsCnt < 0) {
				getFlagsBuf();
				FlagsCnt = 7;
			}

			if ((FlagBuf & 0x80) != 0) {
				FlagBuf <<= 1;
				if (Nlzb > Nhfb) {
					longLZ();
				} else {
					huffDecode();
				}
			} else {
				FlagBuf <<= 1;
				if (--FlagsCnt < 0) {
					getFlagsBuf();
					FlagsCnt = 7;
				}
				if ((FlagBuf & 0x80) != 0) {
					FlagBuf <<= 1;
					if (Nlzb > Nhfb) {
						huffDecode();
					} else {
						longLZ();
					}
				} else {
					FlagBuf <<= 1;
					shortLZ();
				}
			}
		}
		oldUnpWriteBuf();
	}



	protected boolean unpReadBuf() throws IOException, RarException
	{
		  int dataSize=readTop-inAddr;
		  if (dataSize<0){
		    return(false);
		  }
		  if (inAddr>BitInput.MAX_SIZE/2) {
		    if (dataSize>0){
		      //memmove(InBuf,InBuf+InAddr,DataSize);
//		    	for (int i = 0; i < dataSize; i++) {
//					inBuf[i] = inBuf[inAddr + i];
//				}
                System.arraycopy(inBuf, inAddr, inBuf, 0, dataSize);
		    }
		    inAddr=0;
		    readTop=dataSize;
		  }
		  else{
		    dataSize=readTop;
		  }
		  //int readCode=UnpIO->UnpRead(InBuf+DataSize,(BitInput::MAX_SIZE-DataSize)&~0xf);
		  int readCode=unpIO.unpRead(inBuf, dataSize, (BitInput.MAX_SIZE-dataSize)&~0xf);
		  if (readCode>0){
		    readTop+=readCode;
		  }
		  readBorder=readTop-30;
		  return(readCode!=-1);
	}

	private int getShortLen1(int pos)
	{
		return pos == 1 ? Buf60 + 3 : ShortLen1[pos];
	}

	private int getShortLen2(int pos)
	{
		return pos == 3 ? Buf60 + 3 : ShortLen2[pos];
	}

	protected void shortLZ()
	{
		int Length, SaveLength;
		int LastDistance;
		int Distance;
		int DistancePlace;
		NumHuf = 0;

		int BitField = fgetbits();
		if (LCount == 2) {
			faddbits(1);
			if (BitField >= 0x8000) {
				oldCopyString(lastDist, lastLength);
				return;
			}
			BitField <<= 1;
			LCount = 0;
		}
		BitField >>>= 8;
		if (AvrLn1 < 37) {
			for (Length = 0;; Length++) {
				if (((BitField ^ ShortXor1[Length]) & (~(0xff >>> getShortLen1(Length)))) == 0) {
					break;
				}
			}
			faddbits(getShortLen1(Length));
		} else {
			for (Length = 0;; Length++) {
				if (((BitField ^ ShortXor2[Length]) & (~(0xff >> getShortLen2(Length)))) == 0) {
					break;
				}
			}
			faddbits(getShortLen2(Length));
		}

		if (Length >= 9) {
			if (Length == 9) {
				LCount++;
				oldCopyString(lastDist, lastLength);
				return;
			}
			if (Length == 14) {
				LCount = 0;
				Length = decodeNum(fgetbits(), STARTL2, DecL2, PosL2) + 5;
				Distance = (fgetbits() >> 1) | 0x8000;
				faddbits(15);
				lastLength = Length;
				lastDist = Distance;
				oldCopyString(Distance, Length);
				return;
			}

			LCount = 0;
			SaveLength = Length;
			Distance = oldDist[(oldDistPtr - (Length - 9)) & 3];
			Length = decodeNum(fgetbits(), STARTL1, DecL1, PosL1) + 2;
			if (Length == 0x101 && SaveLength == 10) {
				Buf60 ^= 1;
				return;
			}
			if (Distance > 256)
				Length++;
			if (Distance >= MaxDist3)
				Length++;

			oldDist[oldDistPtr++] = Distance;
			oldDistPtr = oldDistPtr & 3;
			lastLength = Length;
			lastDist = Distance;
			oldCopyString(Distance, Length);
			return;
		}

		LCount = 0;
		AvrLn1 += Length;
		AvrLn1 -= AvrLn1 >> 4;

		DistancePlace = decodeNum(fgetbits(), STARTHF2, DecHf2, PosHf2) & 0xff;
		Distance = ChSetA[DistancePlace];
		if (--DistancePlace != -1) {
			PlaceA[Distance]--;
			LastDistance = ChSetA[DistancePlace];
			PlaceA[LastDistance]++;
			ChSetA[DistancePlace + 1] = LastDistance;
			ChSetA[DistancePlace] = Distance;
		}
		Length += 2;
		oldDist[oldDistPtr++] = ++Distance;
		oldDistPtr = oldDistPtr & 3;
		lastLength = Length;
		lastDist = Distance;
		oldCopyString(Distance, Length);
	}

	protected void longLZ()
	{
		int Length;
		int Distance;
		int DistancePlace, NewDistancePlace;
		int OldAvr2, OldAvr3;

		NumHuf = 0;
		Nlzb += 16;
		if (Nlzb > 0xff) {
			Nlzb = 0x90;
			Nhfb >>>= 1;
		}
		OldAvr2 = AvrLn2;

		int BitField = fgetbits();
		if (AvrLn2 >= 122) {
			Length = decodeNum(BitField, STARTL2, DecL2, PosL2);
		} else {
			if (AvrLn2 >= 64) {
				Length = decodeNum(BitField, STARTL1, DecL1, PosL1);
			} else {
				if (BitField < 0x100) {
					Length = BitField;
					faddbits(16);
				} else {
					for (Length = 0; ((BitField << Length) & 0x8000) == 0; Length++) {
						;
					}
					faddbits(Length + 1);
				}
			}
		}
		AvrLn2 += Length;
		AvrLn2 -= AvrLn2 >>> 5;

		BitField = fgetbits();
		if (AvrPlcB > 0x28ff) {
			DistancePlace = decodeNum(BitField, STARTHF2, DecHf2, PosHf2);
		} else {
			if (AvrPlcB > 0x6ff) {
				DistancePlace = decodeNum(BitField, STARTHF1, DecHf1, PosHf1);
			} else {
				DistancePlace = decodeNum(BitField, STARTHF0, DecHf0, PosHf0);
			}
		}
		AvrPlcB += DistancePlace;
		AvrPlcB -= AvrPlcB >> 8;
		while (true) {
			Distance = ChSetB[DistancePlace & 0xff];
			NewDistancePlace = NToPlB[Distance++ & 0xff]++;
			if ((Distance & 0xff) == 0) {
				corrHuff(ChSetB, NToPlB);
			} else {
				break;
			}
		}

		ChSetB[DistancePlace] = ChSetB[NewDistancePlace];
		ChSetB[NewDistancePlace] = Distance;

		Distance = ((Distance & 0xff00) | (fgetbits() >>> 8)) >>> 1;
		faddbits(7);

		OldAvr3 = AvrLn3;
		if (Length != 1 && Length != 4) {
			if (Length == 0 && Distance <= MaxDist3) {
				AvrLn3++;
				AvrLn3 -= AvrLn3 >> 8;
			} else {
				if (AvrLn3 > 0) {
					AvrLn3--;
				}
			}
		}
		Length += 3;
		if (Distance >= MaxDist3) {
			Length++;
		}
		if (Distance <= 256) {
			Length += 8;
		}
		if (OldAvr3 > 0xb0 || AvrPlc >= 0x2a00 && OldAvr2 < 0x40) {
			MaxDist3 = 0x7f00;
		} else {
			MaxDist3 = 0x2001;
		}
		oldDist[oldDistPtr++] = Distance;
		oldDistPtr = oldDistPtr & 3;
		lastLength = Length;
		lastDist = Distance;
		oldCopyString(Distance, Length);
	}

	protected void huffDecode()
	{
		int CurByte, NewBytePlace;
		int Length;
		int Distance;
		int BytePlace;

		int BitField = fgetbits();

		if (AvrPlc > 0x75ff) {
			BytePlace = decodeNum(BitField, STARTHF4, DecHf4, PosHf4);
		} else {
			if (AvrPlc > 0x5dff) {
				BytePlace = decodeNum(BitField, STARTHF3, DecHf3, PosHf3);
			} else {
				if (AvrPlc > 0x35ff) {
					BytePlace = decodeNum(BitField, STARTHF2, DecHf2, PosHf2);
				} else {
					if (AvrPlc > 0x0dff) {
						BytePlace = decodeNum(BitField, STARTHF1, DecHf1,
								PosHf1);
					} else {
						BytePlace = decodeNum(BitField, STARTHF0, DecHf0,
								PosHf0);
					}
				}
			}
		}
		BytePlace &= 0xff;
		if (StMode != 0) {
			if (BytePlace == 0 && BitField > 0xfff) {
				BytePlace = 0x100;
			}
			if (--BytePlace == -1) {
				BitField = fgetbits();
				faddbits(1);
				if ((BitField & 0x8000) != 0) {
					NumHuf = StMode = 0;
					return;
				} else {
					Length = (BitField & 0x4000) != 0 ? 4 : 3;
					faddbits(1);
					Distance = decodeNum(fgetbits(), STARTHF2, DecHf2, PosHf2);
					Distance = (Distance << 5) | (fgetbits() >>> 11);
					faddbits(5);
					oldCopyString(Distance, Length);
					return;
				}
			}
		} else {
			if (NumHuf++ >= 16 && FlagsCnt == 0) {
				StMode = 1;
			}
		}
		AvrPlc += BytePlace;
		AvrPlc -= AvrPlc >>> 8;
		Nhfb += 16;
		if (Nhfb > 0xff) {
			Nhfb = 0x90;
			Nlzb >>>= 1;
		}

		window[unpPtr++] = (byte) (ChSet[BytePlace] >>> 8);
		--destUnpSize;

		while (true) {
			CurByte = ChSet[BytePlace];
			NewBytePlace = NToPl[CurByte++ & 0xff]++;
			if ((CurByte & 0xff) > 0xa1) {
				corrHuff(ChSet, NToPl);
			} else {
				break;
			}
		}

		ChSet[BytePlace] = ChSet[NewBytePlace];
		ChSet[NewBytePlace] = CurByte;
	}

	protected void getFlagsBuf()
	{
		int Flags, NewFlagsPlace;
		int FlagsPlace = decodeNum(fgetbits(), STARTHF2, DecHf2, PosHf2);

		while (true) {
			Flags = ChSetC[FlagsPlace];
			FlagBuf = Flags >>> 8;
			NewFlagsPlace = NToPlC[Flags++ & 0xff]++;
			if ((Flags & 0xff) != 0) {
				break;
			}
			corrHuff(ChSetC, NToPlC);
		}

		ChSetC[FlagsPlace] = ChSetC[NewFlagsPlace];
		ChSetC[NewFlagsPlace] = Flags;
	}

	protected void oldUnpInitData(boolean Solid)
	{
		if (!Solid ) {
			AvrPlcB = AvrLn1 = AvrLn2 = AvrLn3 = NumHuf = Buf60 = 0;
			AvrPlc = 0x3500;
			MaxDist3 = 0x2001;
			Nhfb = Nlzb = 0x80;
		}
		FlagsCnt = 0;
		FlagBuf = 0;
		StMode = 0;
		LCount = 0;
		readTop = 0;
	}

	protected void initHuff()
	{
		for (int I = 0; I < 256; I++) {
			Place[I] = PlaceA[I] = PlaceB[I] = I;
			PlaceC[I] = (~I + 1) & 0xff;
			ChSet[I] = ChSetB[I] = I << 8;
			ChSetA[I] = I;
			ChSetC[I] = ((~I + 1) & 0xff) << 8;
		}

		Arrays.fill(NToPl, 0);// memset(NToPl,0,sizeof(NToPl));
		Arrays.fill(NToPlB, 0); // memset(NToPlB,0,sizeof(NToPlB));
		Arrays.fill(NToPlC, 0); // memset(NToPlC,0,sizeof(NToPlC));
		corrHuff(ChSetB, NToPlB);
	}

	protected void corrHuff(int[] CharSet, int[] NumToPlace)
	{
		int I, J, pos = 0;
		for (I = 7; I >= 0; I--) {
			for (J = 0; J < 32; J++, pos++) {
				CharSet[pos] = ((CharSet[pos] & ~0xff) | I);// *CharSet=(*CharSet
				// & ~0xff) | I;
			}
		}
		Arrays.fill(NumToPlace, 0);// memset(NumToPlace,0,sizeof(NToPl));
		for (I = 6; I >= 0; I--) {
			NumToPlace[I] = (7 - I) * 32;
		}
	}

	protected void oldCopyString(int Distance, int Length)
	{
		destUnpSize -= Length;
		while ((Length--) != 0) {
			window[unpPtr] = window[(unpPtr - Distance) & Compress.MAXWINMASK];
			unpPtr = (unpPtr + 1) & Compress.MAXWINMASK;
		}
	}

	protected int decodeNum(int Num, int StartPos, int[] DecTab, int[] PosTab)
	{
		int I;
		for (Num &= 0xfff0, I = 0; DecTab[I] <= Num; I++) {
			StartPos++;
		}
		faddbits(StartPos);
		return (((Num - (I != 0 ? DecTab[I - 1] : 0)) >>> (16 - StartPos)) + PosTab[StartPos]);
	}

	protected void oldUnpWriteBuf() throws IOException
	{
		if (unpPtr != wrPtr) {
			unpSomeRead = true;
		}
		if (unpPtr < wrPtr) {
			unpIO.unpWrite(window, wrPtr, -wrPtr & Compress.MAXWINMASK);
			unpIO.unpWrite(window, 0, unpPtr);
			unpAllBuf = true;
		} else {
			unpIO.unpWrite(window, wrPtr, unpPtr - wrPtr);
		}
		wrPtr = unpPtr;
	}


}
