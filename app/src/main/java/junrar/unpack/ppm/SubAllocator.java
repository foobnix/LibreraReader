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

import java.util.Arrays;

/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class SubAllocator {
    public static final int N1 = 4, N2 = 4, N3 = 4, N4 = (128 + 3 - 1 * N1 - 2
	    * N2 - 3 * N3) / 4;

    public static final int N_INDEXES = N1 + N2 + N3 + N4;

    public static final int UNIT_SIZE = Math.max(PPMContext.size,
	    RarMemBlock.size);

    public static final int FIXED_UNIT_SIZE = 12;

    private int subAllocatorSize;

    // byte Indx2Units[N_INDEXES], Units2Indx[128], GlueCount;
    private int[] indx2Units = new int[N_INDEXES];
    private int[] units2Indx = new int[128];
    private int glueCount;

    // byte *HeapStart,*LoUnit, *HiUnit;
    private int heapStart, loUnit, hiUnit;

    private final RarNode[] freeList = new RarNode[N_INDEXES];

    // byte *pText, *UnitsStart,*HeapEnd,*FakeUnitsStart;
    private int pText, unitsStart, heapEnd, fakeUnitsStart;

    private byte[] heap;

    private int freeListPos;

    private int tempMemBlockPos;

    // Temp fields
    private RarNode tempRarNode = null;
    private RarMemBlock tempRarMemBlock1 = null;
    private RarMemBlock tempRarMemBlock2 = null;
    private RarMemBlock tempRarMemBlock3 = null;

    public SubAllocator() {
	clean();
    }

    public void clean() {
	subAllocatorSize = 0;
    }

    private void insertNode(int p/* rarnode ptr */, int indx) {
	RarNode temp = tempRarNode;
	temp.setAddress(p);
	temp.setNext(freeList[indx].getNext());
	freeList[indx].setNext(temp);
    }

    public void incPText() {
	pText++;
    }

    private int removeNode(int indx) {
	int retVal = freeList[indx].getNext();
	RarNode temp = tempRarNode;
	temp.setAddress(retVal);
	freeList[indx].setNext(temp.getNext());
	return retVal;
    }

    private int U2B(int NU) {
	return /* 8*NU+4*NU */UNIT_SIZE * NU;
    }

    /* memblockptr */
    private int MBPtr(int BasePtr, int Items) {
	return (BasePtr + U2B(Items));
    }

    private void splitBlock(int pv/* ptr */, int oldIndx, int newIndx) {
	int i, uDiff = indx2Units[oldIndx] - indx2Units[newIndx];
	int p = pv + U2B(indx2Units[newIndx]);
	if (indx2Units[i = units2Indx[uDiff - 1]] != uDiff) {
	    insertNode(p, --i);
	    p += U2B(i = indx2Units[i]);
	    uDiff -= i;
	}
	insertNode(p, units2Indx[uDiff - 1]);
    }

    public void stopSubAllocator() {
	if (subAllocatorSize != 0) {
	    subAllocatorSize = 0;
	    heap = null;
	    heapStart = 1;
	    // rarfree(HeapStart);
	    // Free temp fields
	    tempRarNode = null;
	    tempRarMemBlock1 = null;
	    tempRarMemBlock2 = null;
	    tempRarMemBlock3 = null;
	}
    }

    public int GetAllocatedMemory() {
	return subAllocatorSize;
    };

    public boolean startSubAllocator(int SASize) {
	int t = SASize << 20;
	if (subAllocatorSize == t) {
	    return true;
	}
	stopSubAllocator();
	int allocSize = t / FIXED_UNIT_SIZE * UNIT_SIZE + UNIT_SIZE;

	// adding space for freelist (needed for poiters)
	// 1+ for null pointer
	int realAllocSize = 1 + allocSize + 4 * N_INDEXES;
	// adding space for an additional memblock
	tempMemBlockPos = realAllocSize;
	realAllocSize += RarMemBlock.size;

	heap = new byte[realAllocSize];
	heapStart = 1;
	heapEnd = heapStart + allocSize - UNIT_SIZE;
	subAllocatorSize = t;
	// Bug fixed
	freeListPos = heapStart + allocSize;
	assert (realAllocSize - tempMemBlockPos == RarMemBlock.size) : realAllocSize
		+ " " + tempMemBlockPos + " " + RarMemBlock.size;

	// Init freeList
	for (int i = 0, pos = freeListPos; i < freeList.length; i++, pos += RarNode.size) {
	    freeList[i] = new RarNode(heap);
	    freeList[i].setAddress(pos);
	}

	// Init temp fields
	tempRarNode = new RarNode(heap);
	tempRarMemBlock1 = new RarMemBlock(heap);
	tempRarMemBlock2 = new RarMemBlock(heap);
	tempRarMemBlock3 = new RarMemBlock(heap);

	return true;
    }

    private void glueFreeBlocks() {
	RarMemBlock s0 = tempRarMemBlock1;
	s0.setAddress(tempMemBlockPos);
	RarMemBlock p = tempRarMemBlock2;
	RarMemBlock p1 = tempRarMemBlock3;
	int i, k, sz;
	if (loUnit != hiUnit) {
	    heap[loUnit] = 0;
	}
	for (i = 0, s0.setPrev(s0), s0.setNext(s0); i < N_INDEXES; i++) {
	    while (freeList[i].getNext() != 0) {
		p.setAddress(removeNode(i));// =(RAR_MEM_BLK*)RemoveNode(i);
		p.insertAt(s0);// p->insertAt(&s0);
		p.setStamp(0xFFFF);// p->Stamp=0xFFFF;
		p.setNU(indx2Units[i]);// p->NU=Indx2Units[i];
	    }
	}
	for (p.setAddress(s0.getNext()); p.getAddress() != s0.getAddress(); p
		.setAddress(p.getNext())) {
	    // while ((p1=MBPtr(p,p->NU))->Stamp == 0xFFFF && int(p->NU)+p1->NU
	    // < 0x10000)
	    // Bug fixed
	    p1.setAddress(MBPtr(p.getAddress(), p.getNU()));
	    while (p1.getStamp() == 0xFFFF && p.getNU() + p1.getNU() < 0x10000) {
		p1.remove();
		p.setNU(p.getNU() + p1.getNU());// ->NU += p1->NU;
		p1.setAddress(MBPtr(p.getAddress(), p.getNU()));
	    }
	}
	// while ((p=s0.next) != &s0)
	// Bug fixed
	p.setAddress(s0.getNext());
	while (p.getAddress() != s0.getAddress()) {
	    for (p.remove(), sz = p.getNU(); sz > 128; sz -= 128, p
		    .setAddress(MBPtr(p.getAddress(), 128))) {
		insertNode(p.getAddress(), N_INDEXES - 1);
	    }
	    if (indx2Units[i = units2Indx[sz - 1]] != sz) {
		k = sz - indx2Units[--i];
		insertNode(MBPtr(p.getAddress(), sz - k), k - 1);
	    }
	    insertNode(p.getAddress(), i);
	    p.setAddress(s0.getNext());
	}
    }

    private int allocUnitsRare(int indx) {
	if (glueCount == 0) {
	    glueCount = 255;
	    glueFreeBlocks();
	    if (freeList[indx].getNext() != 0) {
		return removeNode(indx);
	    }
	}
	int i = indx;
	do {
	    if (++i == N_INDEXES) {
		glueCount--;
		i = U2B(indx2Units[indx]);
		int j = FIXED_UNIT_SIZE * indx2Units[indx];
		if (fakeUnitsStart - pText > j) {
		    fakeUnitsStart -= j;
		    unitsStart -= i;
		    return unitsStart;
		}
		return (0);
	    }
	} while (freeList[i].getNext() == 0);
	int retVal = removeNode(i);
	splitBlock(retVal, i, indx);
	return retVal;
    }

    public int allocUnits(int NU) {
	int indx = units2Indx[NU - 1];
	if (freeList[indx].getNext() != 0) {
	    return removeNode(indx);
	}
	int retVal = loUnit;
	loUnit += U2B(indx2Units[indx]);
	if (loUnit <= hiUnit) {
	    return retVal;
	}
	loUnit -= U2B(indx2Units[indx]);
	return allocUnitsRare(indx);
    }

    public int allocContext() {
	if (hiUnit != loUnit)
	    return (hiUnit -= UNIT_SIZE);
	if (freeList[0].getNext() != 0) {
	    return removeNode(0);
	}
	return allocUnitsRare(0);
    }

    public int expandUnits(int oldPtr, int OldNU) {
	int i0 = units2Indx[OldNU - 1];
	int i1 = units2Indx[OldNU - 1 + 1];
	if (i0 == i1) {
	    return oldPtr;
	}
	int ptr = allocUnits(OldNU + 1);
	if (ptr != 0) {
	    // memcpy(ptr,OldPtr,U2B(OldNU));
	    System.arraycopy(heap, oldPtr, heap, ptr, U2B(OldNU));
	    insertNode(oldPtr, i0);
	}
	return ptr;
    }

    public int shrinkUnits(int oldPtr, int oldNU, int newNU) {
	// System.out.println("SubAllocator.shrinkUnits(" + OldPtr + ", " +
	// OldNU + ", " + NewNU + ")");
	int i0 = units2Indx[oldNU - 1];
	int i1 = units2Indx[newNU - 1];
	if (i0 == i1) {
	    return oldPtr;
	}
	if (freeList[i1].getNext() != 0) {
	    int ptr = removeNode(i1);
	    // memcpy(ptr,OldPtr,U2B(NewNU));
	    // for (int i = 0; i < U2B(NewNU); i++) {
	    // heap[ptr + i] = heap[OldPtr + i];
	    // }
	    System.arraycopy(heap, oldPtr, heap, ptr, U2B(newNU));
	    insertNode(oldPtr, i0);
	    return ptr;
	} else {
	    splitBlock(oldPtr, i0, i1);
	    return oldPtr;
	}
    }

    public void freeUnits(int ptr, int OldNU) {
	insertNode(ptr, units2Indx[OldNU - 1]);
    }

    public int getFakeUnitsStart() {
	return fakeUnitsStart;
    }

    public void setFakeUnitsStart(int fakeUnitsStart) {
	this.fakeUnitsStart = fakeUnitsStart;
    }

    public int getHeapEnd() {
	return heapEnd;
    }

    public int getPText() {
	return pText;
    }

    public void setPText(int text) {
	pText = text;
    }

    public void decPText(int dPText) {
	setPText(getPText() - dPText);
    }

    public int getUnitsStart() {
	return unitsStart;
    }

    public void setUnitsStart(int unitsStart) {
	this.unitsStart = unitsStart;
    }

    public void initSubAllocator() {
	int i, k;
	Arrays
		.fill(heap, freeListPos, freeListPos + sizeOfFreeList(),
			(byte) 0);

	pText = heapStart;

	int size2 = FIXED_UNIT_SIZE
		* (subAllocatorSize / 8 / FIXED_UNIT_SIZE * 7);
	int realSize2 = size2 / FIXED_UNIT_SIZE * UNIT_SIZE;
	int size1 = subAllocatorSize - size2;
	int realSize1 = size1 / FIXED_UNIT_SIZE * UNIT_SIZE + size1
		% FIXED_UNIT_SIZE;
	hiUnit = heapStart + subAllocatorSize;
	loUnit = unitsStart = heapStart + realSize1;
	fakeUnitsStart = heapStart + size1;
	hiUnit = loUnit + realSize2;

	for (i = 0, k = 1; i < N1; i++, k += 1) {
	    indx2Units[i] = k & 0xff;
	}
	for (k++; i < N1 + N2; i++, k += 2) {
	    indx2Units[i] = k & 0xff;
	}
	for (k++; i < N1 + N2 + N3; i++, k += 3) {
	    indx2Units[i] = k & 0xff;
	}
	for (k++; i < (N1 + N2 + N3 + N4); i++, k += 4) {
	    indx2Units[i] = k & 0xff;
	}

	for (glueCount = 0, k = 0, i = 0; k < 128; k++) {
	    i += ((indx2Units[i] < (k + 1)) ? 1 : 0);
	    units2Indx[k] = i & 0xff;
	}

    }

    private int sizeOfFreeList() {
	return freeList.length * RarNode.size;
    }

    public byte[] getHeap() {
	return heap;
    }

    // Debug
    // public void dumpHeap() {
    // File file = new File("P:\\test\\heapdumpj");
    // OutputStream out = null;
    // try {
    // out = new FileOutputStream(file);
    // out.write(heap, heapStart, heapEnd - heapStart);
    // out.flush();
    // System.out.println("Heap dumped to " + file.getAbsolutePath());
    // }
    // catch (IOException e) {
    // e.printStackTrace();
    // }
    // finally {
    // FileUtil.close(out);
    // }
    // }

    // Debug
    public String toString() {
	StringBuilder buffer = new StringBuilder();
	buffer.append("SubAllocator[");
	buffer.append("\n  subAllocatorSize=");
	buffer.append(subAllocatorSize);
	buffer.append("\n  glueCount=");
	buffer.append(glueCount);
	buffer.append("\n  heapStart=");
	buffer.append(heapStart);
	buffer.append("\n  loUnit=");
	buffer.append(loUnit);
	buffer.append("\n  hiUnit=");
	buffer.append(hiUnit);
	buffer.append("\n  pText=");
	buffer.append(pText);
	buffer.append("\n  unitsStart=");
	buffer.append(unitsStart);
	buffer.append("\n]");
	return buffer.toString();
    }

}
