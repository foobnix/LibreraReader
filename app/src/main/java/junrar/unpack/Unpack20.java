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
import junrar.unpack.decode.AudioVariables;
import junrar.unpack.decode.BitDecode;
import junrar.unpack.decode.Compress;
import junrar.unpack.decode.Decode;
import junrar.unpack.decode.DistDecode;
import junrar.unpack.decode.LitDecode;
import junrar.unpack.decode.LowDistDecode;
import junrar.unpack.decode.MultDecode;
import junrar.unpack.decode.RepDecode;



/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public abstract class Unpack20 extends Unpack15
{

	protected MultDecode[] MD = new MultDecode[4];

	protected byte[] UnpOldTable20 = new byte[Compress.MC20 * 4];

	protected int UnpAudioBlock, UnpChannels, UnpCurChannel, UnpChannelDelta;

	protected AudioVariables[] AudV = new AudioVariables[4];

	protected LitDecode LD = new LitDecode();

	protected DistDecode DD = new DistDecode();

	protected LowDistDecode LDD = new LowDistDecode();

	protected RepDecode RD = new RepDecode();

	protected BitDecode BD = new BitDecode();

	public static final int[] LDecode = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12,
			14, 16, 20, 24, 28, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192,
			224 };

	public static final byte[] LBits = { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2,
			2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5 };

	public static final int[] DDecode = { 0, 1, 2, 3, 4, 6, 8, 12, 16, 24, 32,
			48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072,
			4096, 6144, 8192, 12288, 16384, 24576, 32768, 49152, 65536, 98304,
			131072, 196608, 262144, 327680, 393216, 458752, 524288, 589824,
			655360, 720896, 786432, 851968, 917504, 983040 };

	public static final int[] DBits = { 0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5,
			5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14,
			15, 15, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 };

	public static final int[] SDDecode = { 0, 4, 8, 16, 32, 64, 128, 192 };

	public static final int[] SDBits = { 2, 2, 3, 4, 5, 6, 6, 6 };

	protected void unpack20(boolean solid) throws IOException, RarException
	{

		int Bits;

		if (suspended) {
			unpPtr = wrPtr;
		} else {
			unpInitData(solid);
			if (!unpReadBuf()) {
				return;
			}
			if (!solid) {
				if (!ReadTables20()) {
					return;
				}
			}
			--destUnpSize;
		}

		while (destUnpSize >= 0) {
			unpPtr &= Compress.MAXWINMASK;

			if (inAddr > readTop - 30)
				if (!unpReadBuf())
					break;
			if (((wrPtr - unpPtr) & Compress.MAXWINMASK) < 270
					&& wrPtr != unpPtr) {
				oldUnpWriteBuf();
				if (suspended)
					return;
			}
			if (UnpAudioBlock != 0) {
				int AudioNumber = decodeNumber(MD[UnpCurChannel]);

				if (AudioNumber == 256) {
					if (!ReadTables20())
						break;
					continue;
				}
				window[unpPtr++] = DecodeAudio(AudioNumber);
				if (++UnpCurChannel == UnpChannels)
					UnpCurChannel = 0;
				--destUnpSize;
				continue;
			}

			int Number = decodeNumber(LD);
			if (Number < 256) {
				window[unpPtr++] = (byte) Number;
				--destUnpSize;
				continue;
			}
			if (Number > 269) {
				int Length = LDecode[Number -= 270] + 3;
				if ((Bits = LBits[Number]) > 0) {
					Length += getbits() >>> (16 - Bits);
					addbits(Bits);
				}

				int DistNumber = decodeNumber(DD);
				int Distance = DDecode[DistNumber] + 1;
				if ((Bits = DBits[DistNumber]) > 0) {
					Distance += getbits() >>> (16 - Bits);
					addbits(Bits);
				}

				if (Distance >= 0x2000) {
					Length++;
					if (Distance >= 0x40000L)
						Length++;
				}

				CopyString20(Length, Distance);
				continue;
			}
			if (Number == 269) {
				if (!ReadTables20())
					break;
				continue;
			}
			if (Number == 256) {
				CopyString20(lastLength, lastDist);
				continue;
			}
			if (Number < 261) {
				int Distance = oldDist[(oldDistPtr - (Number - 256)) & 3];
				int LengthNumber = decodeNumber(RD);
				int Length = LDecode[LengthNumber] + 2;
				if ((Bits = LBits[LengthNumber]) > 0) {
					Length += getbits() >>> (16 - Bits);
					addbits(Bits);
				}
				if (Distance >= 0x101) {
					Length++;
					if (Distance >= 0x2000) {
						Length++;
						if (Distance >= 0x40000)
							Length++;
					}
				}
				CopyString20(Length, Distance);
				continue;
			}
			if (Number < 270) {
				int Distance = SDDecode[Number -= 261] + 1;
				if ((Bits = SDBits[Number]) > 0) {
					Distance += getbits() >>> (16 - Bits);
					addbits(Bits);
				}
				CopyString20(2, Distance);
				continue;
			}
		}
		ReadLastTables();
		oldUnpWriteBuf();

	}

	protected void CopyString20(int Length, int Distance)
	{
		lastDist = oldDist[oldDistPtr++ & 3] = Distance;
		lastLength = Length;
		destUnpSize -= Length;

		int DestPtr = unpPtr - Distance;
		if (DestPtr < Compress.MAXWINSIZE - 300
				&& unpPtr < Compress.MAXWINSIZE - 300) {
			window[unpPtr++] = window[DestPtr++];
			window[unpPtr++] = window[DestPtr++];
			while (Length > 2) {
				Length--;
				window[unpPtr++] = window[DestPtr++];
			}
		} else {
			while ((Length--) != 0) {
				window[unpPtr] = window[DestPtr++ & Compress.MAXWINMASK];
				unpPtr = (unpPtr + 1) & Compress.MAXWINMASK;
			}
		}
	}

	protected void makeDecodeTables(byte[] lenTab, int offset, Decode dec,
			int size)
	{
		int[] lenCount = new int[16];
		int[] tmpPos = new int[16];
		int i;
		long M, N;

		Arrays.fill(lenCount, 0);// memset(LenCount,0,sizeof(LenCount));

		Arrays.fill(dec.getDecodeNum(), 0);// memset(Dec->DecodeNum,0,Size*sizeof(*Dec->DecodeNum));

		for (i = 0; i < size; i++) {
			lenCount[(int) (lenTab[offset + i] & 0xF)]++;
		}
		lenCount[0] = 0;
		for (tmpPos[0] = 0, dec.getDecodePos()[0] = 0, dec.getDecodeLen()[0] = 0, N = 0, i = 1; i < 16; i++) {
			N = 2 * (N + lenCount[i]);
			M = N << (15 - i);
			if (M > 0xFFFF) {
				M = 0xFFFF;
			}
			dec.getDecodeLen()[i] = (int) M;
			tmpPos[i] = dec.getDecodePos()[i] = dec.getDecodePos()[i - 1]
					+ lenCount[i - 1];
		}

		for (i = 0; i < size; i++) {
			if (lenTab[offset + i] != 0) {
				dec.getDecodeNum()[tmpPos[lenTab[offset + i] & 0xF]++] = i;
			}
		}
		dec.setMaxNum(size);
	}

	protected int decodeNumber(Decode dec)
	{
		int bits;
		long bitField = getbits() & 0xfffe;
//        if (bitField < dec.getDecodeLen()[8]) {
//			if (bitField < dec.getDecodeLen()[4]) {
//				if (bitField < dec.getDecodeLen()[2]) {
//					if (bitField < dec.getDecodeLen()[1]) {
//						bits = 1;
//					} else {
//						bits = 2;
//					}
//				} else {
//					if (bitField < dec.getDecodeLen()[3]) {
//						bits = 3;
//					} else {
//						bits = 4;
//					}
//				}
//			} else {
//				if (bitField < dec.getDecodeLen()[6]) {
//					if (bitField < dec.getDecodeLen()[5])
//						bits = 5;
//					else
//						bits = 6;
//				} else {
//					if (bitField < dec.getDecodeLen()[7]) {
//						bits = 7;
//					} else {
//						bits = 8;
//					}
//				}
//			}
//		} else {
//			if (bitField < dec.getDecodeLen()[12]) {
//				if (bitField < dec.getDecodeLen()[10])
//					if (bitField < dec.getDecodeLen()[9])
//						bits = 9;
//					else
//						bits = 10;
//				else if (bitField < dec.getDecodeLen()[11])
//					bits = 11;
//				else
//					bits = 12;
//			} else {
//				if (bitField < dec.getDecodeLen()[14]) {
//					if (bitField < dec.getDecodeLen()[13]) {
//						bits = 13;
//					} else {
//						bits = 14;
//					}
//				} else {
//					bits = 15;
//				}
//			}
//		}
//		addbits(bits);
//		int N = dec.getDecodePos()[bits]
//				+ (((int) bitField - dec.getDecodeLen()[bits - 1]) >>> (16 - bits));
//		if (N >= dec.getMaxNum()) {
//			N = 0;
//		}
//		return (dec.getDecodeNum()[N]);
        int[] decodeLen = dec.getDecodeLen();
        if (bitField < decodeLen[8]) {
			if (bitField < decodeLen[4]) {
				if (bitField < decodeLen[2]) {
					if (bitField < decodeLen[1]) {
						bits = 1;
					} else {
						bits = 2;
					}
				} else {
					if (bitField < decodeLen[3]) {
						bits = 3;
					} else {
						bits = 4;
					}
				}
			} else {
				if (bitField < decodeLen[6]) {
					if (bitField < decodeLen[5])
						bits = 5;
					else
						bits = 6;
				} else {
					if (bitField < decodeLen[7]) {
						bits = 7;
					} else {
						bits = 8;
					}
				}
			}
		} else {
			if (bitField < decodeLen[12]) {
				if (bitField < decodeLen[10])
					if (bitField < decodeLen[9])
						bits = 9;
					else
						bits = 10;
				else if (bitField < decodeLen[11])
					bits = 11;
				else
					bits = 12;
			} else {
				if (bitField < decodeLen[14]) {
					if (bitField < decodeLen[13]) {
						bits = 13;
					} else {
						bits = 14;
					}
				} else {
					bits = 15;
				}
			}
		}
		addbits(bits);
		int N = dec.getDecodePos()[bits]
				+ (((int) bitField - decodeLen[bits - 1]) >>> (16 - bits));
		if (N >= dec.getMaxNum()) {
			N = 0;
		}
		return (dec.getDecodeNum()[N]);
	}

	protected boolean ReadTables20() throws IOException, RarException
	{
		byte[] BitLength = new byte[Compress.BC20];
		byte[] Table = new byte[Compress.MC20 * 4];
		int TableSize, N, I;
		if (inAddr > readTop - 25) {
			if (!unpReadBuf()) {
				return (false);
			}
		}
		int BitField = getbits();
		UnpAudioBlock = (BitField & 0x8000);

		if (0 == (BitField & 0x4000)) {
			// memset(UnpOldTable20,0,sizeof(UnpOldTable20));
			Arrays.fill(UnpOldTable20, (byte) 0);
		}
		addbits(2);

		if (UnpAudioBlock != 0) {
			UnpChannels = ((BitField >>> 12) & 3) + 1;
			if (UnpCurChannel >= UnpChannels) {
				UnpCurChannel = 0;
			}
			addbits(2);
			TableSize = Compress.MC20 * UnpChannels;
		} else {
			TableSize = Compress.NC20 + Compress.DC20 + Compress.RC20;
		}
		for (I = 0; I < Compress.BC20; I++) {
			BitLength[I] = (byte) (getbits() >>> 12);
			addbits(4);
		}
		makeDecodeTables(BitLength, 0, BD, Compress.BC20);
		I = 0;
		while (I < TableSize) {
			if (inAddr > readTop - 5) {
				if (!unpReadBuf()) {
					return (false);
				}
			}
			int Number = decodeNumber(BD);
			if (Number < 16) {
				Table[I] = (byte) ((Number + UnpOldTable20[I]) & 0xf);
				I++;
			} else if (Number == 16) {
				N = (getbits() >>> 14) + 3;
				addbits(2);
				while (N-- > 0 && I < TableSize) {
					Table[I] = Table[I - 1];
					I++;
				}
			} else {
				if (Number == 17) {
					N = (getbits() >>> 13) + 3;
					addbits(3);
				} else {
					N = (getbits() >>> 9) + 11;
					addbits(7);
				}
				while (N-- > 0 && I < TableSize)
					Table[I++] = 0;
			}
		}
		if (inAddr > readTop) {
			return (true);
		}
		if (UnpAudioBlock != 0)
			for (I = 0; I < UnpChannels; I++)
				makeDecodeTables(Table, I * Compress.MC20, MD[I], Compress.MC20);
		else {
			makeDecodeTables(Table, 0, LD, Compress.NC20);
			makeDecodeTables(Table, Compress.NC20, DD, Compress.DC20);
			makeDecodeTables(Table, Compress.NC20 + Compress.DC20, RD,
					Compress.RC20);
		}
		// memcpy(UnpOldTable20,Table,sizeof(UnpOldTable20));
		for (int i = 0; i < UnpOldTable20.length; i++) {
			UnpOldTable20[i] = Table[i];
		}
		return (true);
	}

	protected void unpInitData20(boolean Solid)
	{
		if (!Solid) {
			UnpChannelDelta = UnpCurChannel = 0;
			UnpChannels = 1;
			// memset(AudV,0,sizeof(AudV));
			Arrays.fill(AudV, new AudioVariables());
			// memset(UnpOldTable20,0,sizeof(UnpOldTable20));
			Arrays.fill(UnpOldTable20, (byte) 0);
		}
	}

	protected void ReadLastTables() throws IOException, RarException
	{
		if (readTop >= inAddr + 5) {
			if (UnpAudioBlock != 0) {
				if (decodeNumber(MD[UnpCurChannel]) == 256) {
					ReadTables20();
				}
			} else {
				if (decodeNumber(LD) == 269) {
					ReadTables20();
				}
			}
		}
	}

	protected byte DecodeAudio(int Delta)
	{
		AudioVariables v = AudV[UnpCurChannel];
		v.setByteCount(v.getByteCount() + 1);
		v.setD4(v.getD3());
		v.setD3(v.getD2());// ->D3=V->D2;
		v.setD2(v.getLastDelta() - v.getD1());// ->D2=V->LastDelta-V->D1;
		v.setD1(v.getLastDelta());// V->D1=V->LastDelta;
		// int PCh=8*V->LastChar+V->K1*V->D1 +V->K2*V->D2 +V->K3*V->D3
		// +V->K4*V->D4+ V->K5*UnpChannelDelta;
		int PCh = 8 * v.getLastChar() + v.getK1() * v.getD1();
		PCh += v.getK2() * v.getD2() + v.getK3() * v.getD3();
		PCh += v.getK4() * v.getD4() + v.getK5() * UnpChannelDelta;
		PCh = (PCh >>> 3) & 0xFF;

		int Ch = PCh - Delta;

		int D = ((byte) Delta) << 3;

		v.getDif()[0] += Math.abs(D);// V->Dif[0]+=abs(D);
		v.getDif()[1] += Math.abs(D - v.getD1());// V->Dif[1]+=abs(D-V->D1);
		v.getDif()[2] += Math.abs(D + v.getD1());// V->Dif[2]+=abs(D+V->D1);
		v.getDif()[3] += Math.abs(D - v.getD2());// V->Dif[3]+=abs(D-V->D2);
		v.getDif()[4] += Math.abs(D + v.getD2());// V->Dif[4]+=abs(D+V->D2);
		v.getDif()[5] += Math.abs(D - v.getD3());// V->Dif[5]+=abs(D-V->D3);
		v.getDif()[6] += Math.abs(D + v.getD3());// V->Dif[6]+=abs(D+V->D3);
		v.getDif()[7] += Math.abs(D - v.getD4());// V->Dif[7]+=abs(D-V->D4);
		v.getDif()[8] += Math.abs(D + v.getD4());// V->Dif[8]+=abs(D+V->D4);
		v.getDif()[9] += Math.abs(D - UnpChannelDelta);// V->Dif[9]+=abs(D-UnpChannelDelta);
		v.getDif()[10] += Math.abs(D + UnpChannelDelta);// V->Dif[10]+=abs(D+UnpChannelDelta);

		v.setLastDelta((byte) (Ch - v.getLastChar()));
		UnpChannelDelta = v.getLastDelta();
		v.setLastChar(Ch);// V->LastChar=Ch;

		if ((v.getByteCount() & 0x1F) == 0) {
			int MinDif = v.getDif()[0], NumMinDif = 0;
			v.getDif()[0] = 0;// ->Dif[0]=0;
			for (int I = 1; I < v.getDif().length; I++) {
				if (v.getDif()[I] < MinDif) {
					MinDif = v.getDif()[I];
					NumMinDif = I;
				}
				v.getDif()[I] = 0;
			}
			switch (NumMinDif) {
			case 1:
				if (v.getK1() >= -16) {
					v.setK1(v.getK1() - 1);// V->K1--;
				}
				break;
			case 2:
				if (v.getK1() < 16) {
					v.setK1(v.getK1() + 1);// V->K1++;
				}
				break;
			case 3:
				if (v.getK2() >= -16) {
					v.setK2(v.getK2() - 1);// V->K2--;
				}
				break;
			case 4:
				if (v.getK2() < 16) {
					v.setK2(v.getK2() + 1);// V->K2++;
				}
				break;
			case 5:
				if (v.getK3() >= -16) {
					v.setK3(v.getK3() - 1);
				}
				break;
			case 6:
				if (v.getK3() < 16) {
					v.setK3(v.getK3() + 1);
				}
				break;
			case 7:
				if (v.getK4() >= -16) {
					v.setK4(v.getK4() - 1);
				}
				break;
			case 8:
				if (v.getK4() < 16) {
					v.setK4(v.getK4() + 1);
				}
				break;
			case 9:
				if (v.getK5() >= -16) {
					v.setK5(v.getK5() - 1);
				}
				break;
			case 10:
				if (v.getK5() < 16) {
					v.setK5(v.getK5() + 1);
				}
				break;
			}
		}
		return ((byte) Ch);
	}

}
