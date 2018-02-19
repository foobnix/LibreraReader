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

import junrar.exception.RarException;
import junrar.unpack.Unpack;


/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class RangeCoder
{
	public static final int TOP = 1 << 24;

	public static final int BOT = 1 << 15;

	private static final long uintMask = 0xFFFFffffL;

    // uint low, code, range;
	private long low, code, range;

	private final SubRange subRange = new SubRange();

	private Unpack unpackRead;

	public SubRange getSubRange()
	{
		return subRange;
	}

	public void initDecoder(Unpack unpackRead) throws IOException, RarException
	{
		this.unpackRead = unpackRead;

		low = code = 0L;
		range = 0xFFFFffffL;
		for (int i = 0; i < 4; i++) {
			code = ((code << 8) | getChar())&uintMask;
		}
	}

	public int getCurrentCount()
	{
		range = (range / subRange.getScale())&uintMask;
		return (int)((code - low) / (range));
	}

	public long getCurrentShiftCount(int SHIFT)
	{
		range = range >>>SHIFT;
		return ((code - low) / (range))&uintMask;
	}

	public void decode()
	{
		low = (low + (range * subRange.getLowCount()))&uintMask;
		range = (range * (subRange.getHighCount() - subRange.getLowCount()))&uintMask;
	}

    private int getChar() throws IOException, RarException
	{
		return (unpackRead.getChar());
	}

	public void ariDecNormalize() throws IOException, RarException
	{
//		while ((low ^ (low + range)) < TOP || range < BOT && ((range = -low & (BOT - 1)) != 0 ? true : true)) 
//		{
//			code = ((code << 8) | unpackRead.getChar()&0xff)&uintMask;
//			range = (range << 8)&uintMask;
//			low = (low << 8)&uintMask;
//		}

        // Rewrote for clarity
        boolean c2 = false;
		while ((low ^ (low + range)) < TOP || (c2 = range < BOT)) {
            if (c2) {
                range = (-low & (BOT - 1))&uintMask;
                c2 = false;
            }
			code = ((code << 8) | getChar())&uintMask;
			range = (range << 8)&uintMask;
			low = (low << 8)&uintMask;
		}
    }

    // Debug
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("RangeCoder[");
        buffer.append("\n  low=");
        buffer.append(low);
        buffer.append("\n  code=");
        buffer.append(code);
        buffer.append("\n  range=");
        buffer.append(range);
        buffer.append("\n  subrange=");
        buffer.append(subRange);
        buffer.append("]");
        return buffer.toString();
    }

	public static class SubRange
	{
        // uint LowCount, HighCount, scale;
		private long lowCount, highCount, scale;

		public long getHighCount()
		{
			return highCount;
		}

		public void setHighCount(long highCount)
		{
			this.highCount = highCount&uintMask;
		}

		public long getLowCount()
		{
			return lowCount&uintMask;
		}

		public void setLowCount(long lowCount)
		{
			this.lowCount = lowCount&uintMask;
		}

		public long getScale()
		{
			return scale;
		}

		public void setScale(long scale)
		{
			this.scale = scale&uintMask;
		}

        public void incScale(int dScale) {
            setScale(getScale() + dScale);
        }
        
        // Debug
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("SubRange[");
            buffer.append("\n  lowCount=");
            buffer.append(lowCount);
            buffer.append("\n  highCount=");
            buffer.append(highCount);
            buffer.append("\n  scale=");
            buffer.append(scale);
            buffer.append("]");
            return buffer.toString();
        }
	}
}
