/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 31.05.2007
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
package junrar.unpack.ppm;

import java.io.IOException;
import java.util.Arrays;

import junrar.exception.RarException;
import junrar.unpack.Unpack;


/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class ModelPPM
{
	public static final int MAX_O = 64; /* maximum allowed model order */

	public static final int INT_BITS = 7;

	public static final int PERIOD_BITS = 7;

	public static final int TOT_BITS = INT_BITS + PERIOD_BITS;

	public static final int INTERVAL = 1 << INT_BITS;

	public static final int BIN_SCALE = 1 << TOT_BITS;

	public static final int MAX_FREQ = 124;

	private SEE2Context[][] SEE2Cont = new SEE2Context[25][16];

	private SEE2Context dummySEE2Cont;

	private PPMContext minContext, maxContext;

	private State foundState; // found next state transition

	private int numMasked, initEsc, orderFall, maxOrder, runLength, initRL;

	private int[] charMask = new int[256];

	private int[] NS2Indx = new int[256];

	private int[] NS2BSIndx = new int[256];

	private int[] HB2Flag = new int[256];

    // byte EscCount, PrevSuccess, HiBitsFlag;
	private int escCount, prevSuccess, hiBitsFlag;

	private int[][] binSumm = new int[128][64]; // binary SEE-contexts

	private RangeCoder coder = new RangeCoder();

	private SubAllocator subAlloc = new SubAllocator();

	private static int InitBinEsc[] = { 0x3CDD, 0x1F3F, 0x59BF, 0x48F3,
			0x64A1, 0x5ABC, 0x6632, 0x6051 };

    // Temp fields
    private final State tempState1 = new State(null);
    private final State tempState2 = new State(null);
    private final State tempState3 = new State(null);
    private final State tempState4 = new State(null);
    private final StateRef tempStateRef1 = new StateRef();
    private final StateRef tempStateRef2 = new StateRef();
    private final PPMContext tempPPMContext1 = new PPMContext(null);
    private final PPMContext tempPPMContext2 = new PPMContext(null);
    private final PPMContext tempPPMContext3 = new PPMContext(null);
    private final PPMContext tempPPMContext4 = new PPMContext(null);
    private final int[] ps = new int[MAX_O];

	public ModelPPM()
	{
		minContext = null;
		maxContext = null;
	}

	public SubAllocator getSubAlloc()
	{
		return subAlloc;
	}

	private void restartModelRare()
	{
		Arrays.fill(charMask, 0);
		subAlloc.initSubAllocator();
		initRL = -(maxOrder < 12 ? maxOrder : 12) - 1;
		int addr = subAlloc.allocContext();
		minContext.setAddress(addr);
		maxContext.setAddress(addr);
		minContext.setSuffix(0);
		orderFall = maxOrder;
		minContext.setNumStats(256);
		minContext.getFreqData().setSummFreq(minContext.getNumStats()+1);
				
		addr = subAlloc.allocUnits(256 / 2);
		foundState.setAddress(addr);
		minContext.getFreqData().setStats(addr);

		State state = new State(subAlloc.getHeap());
		addr = minContext.getFreqData().getStats();
        runLength = initRL;
        prevSuccess = 0;
		for (int i = 0; i < 256; i++) {
			state.setAddress(addr + i * State.size);
			state.setSymbol(i);
			state.setFreq(1);
			state.setSuccessor(0);
		}

		for (int i = 0; i < 128; i++) {
			for (int k = 0; k < 8; k++) {
				for (int m = 0; m < 64; m += 8) {
					binSumm[i][k + m] = BIN_SCALE - InitBinEsc[k] / (i + 2);
				}
			}
		}
		for (int i = 0; i < 25; i++) {
			for (int k = 0; k < 16; k++) {
				SEE2Cont[i][k].init(5 * i + 10);
			}
		}
	}

	private void startModelRare(int MaxOrder)
	{
        int i, k, m, Step;
		escCount = 1;
		this.maxOrder = MaxOrder;
		restartModelRare();
        // Bug Fixed
        NS2BSIndx[0] = 0;
        NS2BSIndx[1] = 2;
		for (int j = 0; j < 9; j++) {
			NS2BSIndx[2 + j] = 4;
		}
		for (int j = 0; j < 256 - 11; j++) {
			NS2BSIndx[11 + j] = 6;
		}
		for (i = 0; i < 3; i++) {
			NS2Indx[i] = i;
		}
		for (m = i, k = 1, Step = 1; i < 256; i++) {
			NS2Indx[i] = m;
			if ((--k) == 0) {
				k = ++Step;
				m++;
			}
		}
		for (int j = 0; j < 0x40; j++) {
			HB2Flag[j] = 0;
		}
		for (int j = 0; j < 0x100 - 0x40; j++) {
			HB2Flag[0x40 + j] = 0x08;
		}
		dummySEE2Cont.setShift(PERIOD_BITS);

	}

	private void clearMask()
	{
		escCount = 1;
		Arrays.fill(charMask, 0);
	}

	public boolean decodeInit(Unpack unpackRead, int escChar/* ref */)
			throws IOException, RarException
	{

		int MaxOrder = unpackRead.getChar() & 0xff;
		boolean reset = ((MaxOrder & 0x20) != 0);

		int MaxMB = 0;
		if (reset) {
			MaxMB = unpackRead.getChar();
			// Workaround: http://stackoverflow.com/questions/14109455/outofmemoryerror-when-i-decompress-rar-file-on-android
			if (MaxMB > 1) {
				MaxMB = 1;
			}
		} else {
			if (subAlloc.GetAllocatedMemory() == 0) {
				return (false);
			}
		}
		if ((MaxOrder & 0x40) != 0) {
			escChar = unpackRead.getChar();
			unpackRead.setPpmEscChar(escChar);
		}
		coder.initDecoder(unpackRead);
		if (reset) {
			MaxOrder = (MaxOrder & 0x1f) + 1;
			if (MaxOrder > 16) {
				MaxOrder = 16 + (MaxOrder - 16) * 3;
			}
			if (MaxOrder == 1) {
				subAlloc.stopSubAllocator();
				return (false);
			}
			subAlloc.startSubAllocator(MaxMB + 1);
			minContext = new PPMContext(getHeap());
			maxContext = new PPMContext(getHeap());
			foundState = new State(getHeap());
			dummySEE2Cont = new SEE2Context();
			for (int i = 0; i < 25; i++) {
				for (int j = 0; j < 16; j++) {
					SEE2Cont[i][j] = new SEE2Context();
				}
			}
			startModelRare(MaxOrder);
		}
		return (minContext.getAddress() != 0);
	}

	public int decodeChar() throws IOException, RarException
	{
        // Debug
        //subAlloc.dumpHeap();

		if (minContext.getAddress() <= subAlloc.getPText()
				|| minContext.getAddress() > subAlloc.getHeapEnd()) {
			return (-1);
		}

		if (minContext.getNumStats() != 1) {
			if (minContext.getFreqData().getStats() <= subAlloc.getPText()
					|| minContext.getFreqData().getStats() > subAlloc.getHeapEnd()) {
				return (-1);
			}
			if (!minContext.decodeSymbol1(this)) {
				return (-1);
			}
		} else {
			minContext.decodeBinSymbol(this);
		}
		coder.decode();
		while (foundState.getAddress() == 0) {
			coder.ariDecNormalize();
			do {
				orderFall++;
				minContext.setAddress(minContext.getSuffix());// =MinContext->Suffix;
				if (minContext.getAddress() <= subAlloc.getPText()
						|| minContext.getAddress() > subAlloc.getHeapEnd()) {
					return (-1);
				}
			} while (minContext.getNumStats() == numMasked);
			if (!minContext.decodeSymbol2(this)) {
				return (-1);
			}
			coder.decode();
		}
		int Symbol = foundState.getSymbol();
		if ((orderFall == 0) && foundState.getSuccessor() > subAlloc.getPText()) {
			// MinContext=MaxContext=FoundState->Successor;
			int addr = foundState.getSuccessor();
			minContext.setAddress(addr);
			maxContext.setAddress(addr);
		} else {
			updateModel();
			//this.foundState.setAddress(foundState.getAddress());//TODO just 4 debugging
			if (escCount == 0) {
				clearMask();
			}
		}
		coder.ariDecNormalize();// ARI_DEC_NORMALIZE(Coder.code,Coder.low,Coder.range,Coder.UnpackRead);
		return (Symbol);
	}

	public SEE2Context[][] getSEE2Cont()
	{
		return SEE2Cont;
	}

	public SEE2Context getDummySEE2Cont()
	{
		return dummySEE2Cont;
	}

	public int getInitRL()
	{
		return initRL;
	}

	public void setEscCount(int escCount)
	{
		this.escCount = escCount&0xff;
	}

	public int getEscCount()
	{
		return escCount;
	}

    public void incEscCount(int dEscCount) {
        setEscCount(getEscCount() + dEscCount);
    }

	public int[] getCharMask()
	{
		return charMask;
	}

	public int getNumMasked()
	{
		return numMasked;
	}

	public void setNumMasked(int numMasked)
	{
		this.numMasked = numMasked;
	}

	public void setPrevSuccess(int prevSuccess)
	{
		this.prevSuccess = prevSuccess&0xff;
	}

	public int getInitEsc()
	{
		return initEsc;
	}

	public void setInitEsc(int initEsc)
	{
		this.initEsc = initEsc;
	}

	public void setRunLength(int runLength)
	{
		this.runLength = runLength;
	}

	public int getRunLength()
	{
		return runLength;
	}

    public void incRunLength(int dRunLength) {
        setRunLength(getRunLength() + dRunLength);
    }

	public int getPrevSuccess()
	{
		return prevSuccess;
	}

	public int getHiBitsFlag()
	{
		return hiBitsFlag;
	}

	public void setHiBitsFlag(int hiBitsFlag)
	{
		this.hiBitsFlag = hiBitsFlag&0xff;
	}

	public int[][] getBinSumm()
	{
		return binSumm;
	}

	public RangeCoder getCoder()
	{
		return coder;
	}

	public int[] getHB2Flag()
	{
		return HB2Flag;
	}

	public int[] getNS2BSIndx()
	{
		return NS2BSIndx;
	}

	public int[] getNS2Indx()
	{
		return NS2Indx;
	}

	public State getFoundState()
	{
		return foundState;
	}

	public byte[] getHeap()
	{
		return subAlloc.getHeap();
	}

	public int getOrderFall()
	{
		return orderFall;
	}

	private int /* ppmcontext ptr */createSuccessors(boolean Skip,
            State p1 /* state ptr */) {
		//State upState = tempState1.init(null);
		StateRef upState = tempStateRef2;
		State tempState = tempState1.init(getHeap());

		// PPM_CONTEXT* pc=MinContext, * UpBranch=FoundState->Successor;
		PPMContext pc = tempPPMContext1.init(getHeap());
		pc.setAddress(minContext.getAddress());
		PPMContext upBranch = tempPPMContext2.init(getHeap());
		upBranch.setAddress(foundState.getSuccessor());

		// STATE * p, * ps[MAX_O], ** pps=ps;
		State p = tempState2.init(getHeap());
		int pps = 0;

		boolean noLoop = false;

		if (!Skip) {
			ps[pps++] = foundState.getAddress();// *pps++ = FoundState;
			if (pc.getSuffix() == 0) {
				noLoop = true;
			}
		}
		if (!noLoop) {
			boolean loopEntry = false;
			if (p1.getAddress() != 0) {
				p.setAddress(p1.getAddress());
				pc.setAddress(pc.getSuffix());// =pc->Suffix;
				loopEntry = true;
			}
			do {
				if (!loopEntry) {
					pc.setAddress(pc.getSuffix());// pc=pc->Suffix;
					if (pc.getNumStats() != 1) {
						p.setAddress(pc.getFreqData().getStats());// p=pc->U.Stats
						if (p.getSymbol() != foundState.getSymbol()) {
							do {
								p.incAddress();
							} while (p.getSymbol() != foundState.getSymbol());
						}
					} else {
						p.setAddress(pc.getOneState().getAddress());// p=&(pc->OneState);
					}
				}// LOOP_ENTRY:
				loopEntry = false;
				if (p.getSuccessor() != upBranch.getAddress()) {
					pc.setAddress(p.getSuccessor());// =p->Successor;
					break;
				}
				ps[pps++] = p.getAddress();
			} while (pc.getSuffix() != 0);

		} // NO_LOOP:
		if (pps == 0) {
			return pc.getAddress();
		}
		upState.setSymbol(getHeap()[upBranch.getAddress()]);// UpState.Symbol=*(byte*)
															// UpBranch;
		// UpState.Successor=(PPM_CONTEXT*) (((byte*) UpBranch)+1);
		upState.setSuccessor(upBranch.getAddress() + 1); //TODO check if +1 necessary
		if (pc.getNumStats() != 1) {
			if (pc.getAddress() <= subAlloc.getPText()) {
				return (0);
			}
			p.setAddress(pc.getFreqData().getStats());
			if (p.getSymbol() != upState.getSymbol()) {
				do {
					p.incAddress();
				} while (p.getSymbol() != upState.getSymbol());
			}
			int cf = p.getFreq() - 1;
			int s0 = pc.getFreqData().getSummFreq() - pc.getNumStats() - cf;
			// UpState.Freq=1+((2*cf <= s0)?(5*cf > s0):((2*cf+3*s0-1)/(2*s0)));
			upState.setFreq(1 + ((2 * cf <= s0) ? (5 * cf > s0 ? 1 : 0) :
                    ((2 * cf + 3 * s0 - 1) / (2 * s0))));
		} else {
			upState.setFreq(pc.getOneState().getFreq());// UpState.Freq=pc->OneState.Freq;
		}
		do {
			// pc = pc->createChild(this,*--pps,UpState);
			tempState.setAddress(ps[--pps]);
			pc.setAddress(pc.createChild(this, tempState, upState));
			if (pc.getAddress() == 0) {
				return 0;
			}
		} while (pps != 0);
		return pc.getAddress();
	}

	private void updateModelRestart()
	{
		restartModelRare();
		escCount = 0;
	}

	private void updateModel()
	{
        //System.out.println("ModelPPM.updateModel()");
		// STATE fs = *FoundState, *p = NULL;
		StateRef fs = tempStateRef1;
		fs.setValues(foundState);
		State p = tempState3.init(getHeap());
		State tempState = tempState4.init(getHeap());

		PPMContext pc = tempPPMContext3.init(getHeap());
		PPMContext successor = tempPPMContext4.init(getHeap());

		int ns1, ns, cf, sf, s0;
		pc.setAddress(minContext.getSuffix());
		if (fs.getFreq() < MAX_FREQ / 4 && pc.getAddress() != 0) {
			if (pc.getNumStats() != 1) {
				p.setAddress(pc.getFreqData().getStats());
				if (p.getSymbol() != fs.getSymbol()) {
					do {
						p.incAddress();
					} while (p.getSymbol() != fs.getSymbol());
					tempState.setAddress(p.getAddress() - State.size);
					if (p.getFreq() >= tempState.getFreq()) {
						State.ppmdSwap(p, tempState);
						p.decAddress();
					}
				}
				if (p.getFreq() < MAX_FREQ - 9) {
					p.incFreq(2);
					pc.getFreqData().incSummFreq(2);
				}
			} else {
				p.setAddress(pc.getOneState().getAddress());
                if (p.getFreq() < 32) {
                    p.incFreq(1);
                }
			}
		}
		if (orderFall == 0) {
			foundState.setSuccessor(createSuccessors(true, p));
			minContext.setAddress(foundState.getSuccessor());
			maxContext.setAddress(foundState.getSuccessor());
			if (minContext.getAddress() == 0) {
				updateModelRestart();
				return;
			}
			return;
		}
		subAlloc.getHeap()[subAlloc.getPText()] = (byte)fs.getSymbol();
		subAlloc.incPText();
		successor.setAddress(subAlloc.getPText());
		if (subAlloc.getPText() >= subAlloc.getFakeUnitsStart()) {
			updateModelRestart();
			return;
		}
//        // Debug
//        subAlloc.dumpHeap();
		if (fs.getSuccessor() != 0) {
			if (fs.getSuccessor() <= subAlloc.getPText()) {
				fs.setSuccessor(createSuccessors(false, p));
				if (fs.getSuccessor() == 0) {
					updateModelRestart();
					return;
				}
			}
			if (--orderFall == 0) {
				successor.setAddress(fs.getSuccessor());
                if (maxContext.getAddress() != minContext.getAddress()) {
                    subAlloc.decPText(1);
                }
			}
		}
        else {
			foundState.setSuccessor(successor.getAddress());
			fs.setSuccessor(minContext);
		}
//        // Debug
//        subAlloc.dumpHeap();
		ns = minContext.getNumStats();
		s0 = minContext.getFreqData().getSummFreq() - (ns) - (fs.getFreq() - 1);
		for (pc.setAddress(maxContext.getAddress());
                pc.getAddress() != minContext.getAddress();
                pc.setAddress(pc.getSuffix())) {
			if ((ns1 = pc.getNumStats()) != 1) {
				if ((ns1 & 1) == 0) {
					//System.out.println(ns1);
					pc.getFreqData().setStats(
							subAlloc.expandUnits(pc.getFreqData().getStats(),
									ns1 >>> 1));
					if (pc.getFreqData().getStats() == 0) {
						updateModelRestart();
						return;
					}
				}
                // bug fixed
//				int sum = ((2 * ns1 < ns) ? 1 : 0) +
//                        2 * ((4 * ((ns1 <= ns) ? 1 : 0)) & ((pc.getFreqData()
//								.getSummFreq() <= 8 * ns1) ? 1 : 0));
				int sum = ((2 * ns1 < ns) ? 1 : 0) + 2 * (
                        ((4 * ns1 <= ns) ? 1 : 0) &
                        ((pc.getFreqData().getSummFreq() <= 8 * ns1) ? 1 : 0)
                        );
				pc.getFreqData().incSummFreq(sum);
			}
            else {
				p.setAddress(subAlloc.allocUnits(1));
				if (p.getAddress() == 0) {
					updateModelRestart();
					return;
				}
				p.setValues(pc.getOneState());
				pc.getFreqData().setStats(p);
				if (p.getFreq() < MAX_FREQ / 4 - 1) {
					p.incFreq(p.getFreq());
				}
                else {
					p.setFreq(MAX_FREQ - 4);
				}
				pc.getFreqData().setSummFreq(
                        (p.getFreq() + initEsc + (ns > 3 ? 1 : 0)));
			}
			cf = 2 * fs.getFreq() * (pc.getFreqData().getSummFreq() + 6);
			sf = s0 + pc.getFreqData().getSummFreq();
			if (cf < 6 * sf) {
				cf = 1 + (cf > sf ? 1 : 0) + (cf >= 4 * sf ? 1 : 0);
				pc.getFreqData().incSummFreq(3);
			}
            else {
				cf = 4 + (cf >= 9 * sf ? 1 : 0) + (cf >= 12 * sf ? 1 : 0) +
                        (cf >= 15 * sf ? 1 : 0);
				pc.getFreqData().incSummFreq(cf);
			}
			p.setAddress(pc.getFreqData().getStats() + ns1*State.size);
			p.setSuccessor(successor);
			p.setSymbol(fs.getSymbol());
			p.setFreq(cf);
			pc.setNumStats(++ns1);
		}
		
		int address = fs.getSuccessor();
		maxContext.setAddress(address);
		minContext.setAddress(address);
		//TODO-----debug
//		int pos = minContext.getFreqData().getStats();
//		State a = new State(getHeap());
//		a.setAddress(pos);
//		pos+=State.size;
//		a.setAddress(pos);
		//--dbg end
		return;
	}

    // Debug
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("ModelPPM[");
        buffer.append("\n  numMasked=");
        buffer.append(numMasked);
        buffer.append("\n  initEsc=");
        buffer.append(initEsc);
        buffer.append("\n  orderFall=");
        buffer.append(orderFall);
        buffer.append("\n  maxOrder=");
        buffer.append(maxOrder);
        buffer.append("\n  runLength=");
        buffer.append(runLength);
        buffer.append("\n  initRL=");
        buffer.append(initRL);
        buffer.append("\n  escCount=");
        buffer.append(escCount);
        buffer.append("\n  prevSuccess=");
        buffer.append(prevSuccess);
        buffer.append("\n  foundState=");
        buffer.append(foundState);
        buffer.append("\n  coder=");
        buffer.append(coder);
        buffer.append("\n  subAlloc=");
        buffer.append(subAlloc);
        buffer.append("\n]");
        return buffer.toString();
    }

    // Debug
//    public void dumpHeap() {
//        subAlloc.dumpHeap();
//    }
}
