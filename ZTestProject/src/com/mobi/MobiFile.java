/**
 * Copyright (C) 2013 
 * Nicholas J. Little <arealityfarbetween@googlemail.com>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.mobi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.mobi.format.headers.InvalidHeaderException;
import com.mobi.format.headers.MobiDocHeader;
import com.mobi.format.headers.PalmDocHeader;
import com.mobi.format.records.EofRecord;
import com.mobi.format.records.FcisRecord;
import com.mobi.format.records.FlisRecord;
import com.mobi.format.records.PdbRecord;
import com.mobi.little.nj.adts.ByteFieldMapSet;
import com.mobi.little.nj.adts.IntByteField;
import com.mobi.m2.EXTHRecord;
import com.mobi.m2.MobiHeader;
import com.mobi.m2.PDBHeader;
import com.mobi.m2.RecordInfo;
import com.mobi.m2.StreamUtils;

public class MobiFile extends PdbFile {

    public static final String IMAGE_EXT = "jpg";

    private List<ByteBuffer> images = new LinkedList<ByteBuffer>();

    private MobiDocHeader mobi;

    private PalmDocHeader palm;

    private PalmDocText text;

    private PdbRecord zero;

    private boolean write_flis, write_fcis;

    private static FileWriter fileWriter;

    static DefaultCodecManager codec_manager = new DefaultCodecManager();

    public MobiFile(CodecManager codecs) {
        zero = new PdbRecord();
        palm = new PalmDocHeader();
        mobi = new MobiDocHeader();
        text = new PalmDocText(mobi.getEncoding());
    }

    private static byte[] filterBOM(byte[] fileData) {
        if (fileData.length < 3) {
            return fileData;
        }
        final byte[] array = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        final byte[] data = { fileData[0], fileData[1], fileData[2] };
        return fileData.length > 3 && Arrays.equals(data, array) ? Arrays.copyOfRange(fileData, 3, fileData.length) : fileData;
    }

    public static final String UTF8_BOM = "\uFEFF";

    private static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }

    public static void main(String[] args) throws IOException {
        String HOME = "/home/ivan-dev/dev/workspace/pdf4/ZTestProject";

        File mobi = new File(HOME + "/input/Вожак.mobi");

        byte[] data = null; // Files.readAllBytes(path);
        ByteArrayInputStream mobiStream = new ByteArrayInputStream(data);
        PDBHeader pdbheader = new PDBHeader(mobiStream);

        byte[] name = Arrays.copyOfRange(data, 0, 32);
        System.out.println("Name: " + new String(name).trim() + "|");
        System.out.println("Name: " + pdbheader.getName() + "|");

        byte[] records = Arrays.copyOfRange(data, 76, 76 + 2);
        int recordCount = StreamUtils.byteArrayToInt(records);
        System.out.println("Records:" + recordCount + " " + pdbheader.getNumberOfRecord());

        System.out.println("Size:" + pdbheader.getMobiHeaderSize());

        MobiHeader mobiHeader = new MobiHeader(mobiStream, pdbheader.getMobiHeaderSize());

        int first = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, 78, 78 + 4));
        int second = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, 78 + 8, 78 + 8 + 4));
        int headSize = second - first;
        System.out.println("Head Size:" + headSize);

        // mobi hader
        int offestMobi = 78 + 8 * recordCount + 2;
        System.out.println("Offest Mobi " + offestMobi);
        int comppression = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi, offestMobi + 2));
        System.out.println("Compression:" + comppression);

        System.out.println("Compression:" + mobiHeader.getCompression());

        System.out.println("the characters " + StreamUtils.byteArrayToString(Arrays.copyOfRange(data, offestMobi + 16, offestMobi + 16 + 4)));
        System.out.println("type " + StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi + 24, offestMobi + 24 + 4)));
        System.out.println("Encoding " + StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi + 28, offestMobi + 28 + 4)));

        int fullNameOffset = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi + 84, offestMobi + 84 + 4));
        System.out.println("Full Name Offset " + fullNameOffset);
        int fullNameLen = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi + 88, offestMobi + 88 + 4));
        System.out.println("Full Name Length " + fullNameLen);

        System.out.println("Full Name |" + StreamUtils.byteArrayToString(Arrays.copyOfRange(data, offestMobi + fullNameOffset, offestMobi + fullNameOffset + fullNameLen)) + "|");

        int headerSize = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi + 20, offestMobi + 20 + 4));
        System.out.println("Mibi Header Size " + headerSize);
        int extFlag = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi + 128, offestMobi + 128 + 4));
        boolean extFlagSet = (extFlag & 0x40) != 0;

        System.out.println("EXTHflag: " + extFlagSet + " " + mobiHeader.getEXTHRecords().size());

        int firstText = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi + 192, offestMobi + 192 + 2));
        int lastText = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, offestMobi + 194, offestMobi + 194 + 2));

        System.out.println("First+Last " + first + " " + lastText);

        int j = 0;
        List<Integer> offsets = new ArrayList<Integer>();
        for (int i = 78; i < 78 + 8 * recordCount; i += 8) {
            RecordInfo recordInfo = pdbheader.recordInfoList.get(j++);
            int recordOffset = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, i, i + 4));
            int recordID = StreamUtils.byteArrayToInt(Arrays.copyOfRange(data, i + 5, i + 5 + 3));
            System.out.println("Offset ID: " + recordOffset + " " + recordID);
            offsets.add(recordOffset);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 1; i <= 15; i++) {
            System.out.println("--start-- " + i);
            int start = offsets.get(i);
            int end = offsets.get(i + 1);
            int len = end - start;

            byte[] copyOfRange = Arrays.copyOfRange(data, start, start + len);
            byte[] decoded = PalmDocCodec.decompressOther(copyOfRange);
            for (int n = 0; n < decoded.length; n++) {
                if (decoded[n] != (short) 0x00) {
                    out.write(decoded[n]);
                }

            }

            // System.out.println(StreamUtils.byteArrayToString(decoded));
            // System.out.println(new String(decoded, "UTF-8"));
            System.out.println("--end-- " + i);
        }
        System.out.println("============");
        // System.out.println(out.toString());
        System.out.println("---");

        FileWriter write = new FileWriter(new File("/home/ivan-dev/dev/workspace/pdf4/ZTestProject/output/0_BOOK1.html"));
        String string = out.toString();
        string = string.replace("<head>", "<head><meta charset='utf-8'>");
        string = string.replaceAll("recindex=\"([0]*)([0-9]+)\"", "src=\"$2.jpg\"");
        write.write(string);
        write.flush();
        write.close();

    }

    public static void main2(String[] args) throws IOException, InvalidHeaderException {

        // http://wiki.mobileread.com/wiki/MOBI

        // See also class PalmDocCodec, two algoritm
        // method 1 public byte[] decompress(byte[] input) {
        // method 2 public static byte[] decompressOther(byte[] bytes) {

        String HOME = "/home/ivan-dev/dev/workspace/pdf4/ZTestProject";

        MobiFile mobi = new MobiFile(new File(HOME + "/input/Вожак.mobi"));
        // MobiFile mobi = new MobiFile(new File(HOME +
        // "/input/Aleksandrova_Domovyonok-Kuzka.297647.fb2.mobi"));

        System.out.println("Author: " + mobi.getAuthor());
        System.out.println("Title: " + mobi.getTitle());

        File DIR = new File(HOME + "/output");
        File file = new File(DIR, "0_BOOK.html");
        File file1 = new File(DIR, "0_BOOK.html.txt");

        for (File item : DIR.listFiles()) {
            item.delete();
        }

        ByteArrayOutputStream outStream = mobi.getText().getOutStrem();

        String string = outStream.toString("UTF-8");

        FileWriter writer = new FileWriter(file);

        string = string.replace("<head>", "<head><meta charset='utf-8'>");
        string = string.replaceAll("recindex=\"([0]*)([0-9]+)\"", "src=\"$2.jpg\"");
        // string = string.replace("\u0000", "");
        // string = string.replace(new String(new byte[] { (byte) 0xD1, (byte)
        // 0x83, (byte) 0xEF, (byte) 0xBF }), "");

        // string = string.replace(new String(new byte[] { (byte) 0xEF, (byte)
        // 0xBF }), "");

        writer.write(string);
        writer.flush();
        writer.close();

        FileWriter writer1 = new FileWriter(file1);
        writer1.write(string);
        writer1.flush();
        writer1.close();

        List<ByteBuffer> images = mobi.getImages();
        for (int i = 0; i < images.size(); i++) {
            FileOutputStream image = new FileOutputStream(new File(DIR, "" + (i + 1) + ".jpg"));
            image.write(images.get(i).array());
            image.flush();
            image.close();
        }

        InputStream in = new ByteArrayInputStream(mobi.buf);
        PDBHeader pdbheader = new PDBHeader(in);
        MobiHeader mobiHeader = new MobiHeader(in, pdbheader.getMobiHeaderSize());
        System.out.println("Title: " + mobiHeader.getFullName());

        for (EXTHRecord record : mobiHeader.getEXTHRecords()) {
            if (record.getTypeDescription().equals("author")) {
                System.out.println("Author: " + new String(record.getData()));
            }
        }

    }

    public static long copy(Reader input, Writer output) throws IOException {

        char[] buffer = new char[8192];
        long count = 0;
        int n;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static MobiMeta parseMobiMeta(File file) {
        try {
            MobiFile mobi = new MobiFile(file);
            ByteBuffer coverOrThumb = mobi.getCoverOrThumb();
            return new MobiMeta(mobi.getTitle(), mobi.getAuthor(), coverOrThumb != null ? coverOrThumb.array() : null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public MobiFile(File in) throws IOException, InvalidHeaderException {
        super(in);
        parse();
    }

    protected void parse() throws InvalidHeaderException {
        zero = getToc().iterator().next();
        ByteBuffer _zero = zero.getBuffer();

        System.out.println("Extracting Palm Header...");

        palm = PalmDocHeader.parseBuffer(_zero);
        System.out.println("Extracting Mobi Header...");

        mobi = MobiDocHeader.parseBuffer(_zero);
        text = new PalmDocText(palm.getTextRecordLength(), mobi.getEncoding());
        extractContent();
    }

    protected void extractContent() {
        System.out.println("Extracting Text...");
        extractText();
        System.out.println("Extracting Images...");
        extractImages();
    }

    protected void extractImages() {
        int record = mobi.getFirstImageRecord();
        int end = mobi.getLastContentRecord();
        System.out.println("==== " + record + " " + end);
        if (record > 0) {
            ListIterator<PdbRecord> it = getToc().iterator(record);
            int j;
            while ((j = it.nextIndex()) <= end) {
                PdbRecord next = it.next();
                byte[] data = next.getData();
                // System.out.println("OFFset " + next.getID());
                images.add(ByteBuffer.wrap(data));
            }
        }
    }

    int fromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    protected void extractText() {
        int record = mobi.getFirstContentRecord();
        int count = palm.getTextRecordCount();
        if (record > 0) {
            ensureTextCodec();
            ListIterator<PdbRecord> it = getToc().iterator(record);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (count-- > 0) {
                PdbRecord next = it.next();
                // System.out.println("Extract ID:" + next.getID());
                // System.out.println("String: " + new
                // String(PalmDocCodec.decompressOther(next.getData())));
                text.addToFile(next.getID(), next.getBuffer());
                // text.onlyAdd(next.getData());
                try {
                    // out.write(next.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // text.addToFile(ByteBuffer.wrap(out.toByteArray()));
        }
    }

    public MobiDocHeader getMobiDocHeader() {
        return mobi;
    }

    public PalmDocHeader getPalmDocHeader() {
        return palm;
    }

    public PalmDocText getText() {
        return text;
    }

    public List<ByteBuffer> getImages() {
        return images;
    }

    public String getTitle() {
        if (!mobi.hasExthHeader()) {
            return getHeader().getName();
        }
        return mobi.getExthHeader().getTitle();
    }

    public String getAuthor() {
        if (!mobi.hasExthHeader())
            return StringUtil.EMPTY_STRING;

        return mobi.getExthHeader().getAuthor();
    }

    public String getBlurb() {
        if (!mobi.hasExthHeader())
            return StringUtil.EMPTY_STRING;

        return mobi.getExthHeader().getBlurb();
    }

    public ByteBuffer getCover() {
        if (!mobi.hasExthHeader())
            return null;

        int x = mobi.getExthHeader().getCover();

        if (x < 0)
            return null;

        return images.get(x);
    }

    public ByteBuffer getThumb() {
        if (!mobi.hasExthHeader())
            return null;

        int x = mobi.getExthHeader().getThumb();
        if (x < 0)
            return null;

        return images.get(x);
    }

    public ByteBuffer getCoverOrThumb() {
        ByteBuffer cover = getCover();
        return cover != null ? cover : getThumb();
    }

    public void setTitle(String x) {
        getHeader().setName(x);
        mobi.getExthHeader().setTitle(x);
    }

    public void setAuthor(String x) {
        mobi.setExthHeader(true);
        mobi.getExthHeader().setAuthor(x);
    }

    public void setBlurb(String x) {
        mobi.setExthHeader(true);
        mobi.getExthHeader().setBlurb(x);
    }

    public void setCovers(ByteBuffer... covers) {
        ByteBuffer cover = null;
        ByteBuffer thumb = null;
        if (covers.length > 1) {
            cover = covers[0];
            thumb = covers[1];
        } else if (covers.length > 0) {
            cover = covers[0];
            thumb = cover;
        }
        if (cover != null) {
            mobi.setExthHeader(true);
            int index = images.indexOf(cover);
            if (index < 0) {
                index = images.size();
                images.add(cover);
            }
            mobi.getExthHeader().setCover(index);
            index = images.indexOf(thumb);
            if (index < 0) {
                index = images.size();
                images.add(thumb);
            }
            mobi.getExthHeader().setThumb(index);
        }
    }

    @Override
    public boolean writeToFile(File out) {
        refresh();
        return super.writeToFile(out);
    }

    public final void refresh() {
        buildFile();
    }

    protected void buildFile() {
        getHeader().setBookType("BOOK");
        getHeader().setCreator(MobiDocHeader.MOBI);

        getToc().clear(); // Clear the toc
        getToc().iterator().add(zero); // Place record zero
        int curr = 1; // Start placing from first record

        /*
         * Set any unsupported record pointers as they will have been stripped
         */
        setUnsupportedRecordPointers();

        /*
         * Insert text, set pointers: - First Content Record - First Non Book
         * Record
         */
        curr += insertText(curr);

        int fnonbook = curr;
        /*
         * Insert images, set pointers: - First Image Record
         */
        curr += insertImages(curr);

        /*
         * If we inserted anything after the book we need to set the first non
         * book record
         */
        if (fnonbook < curr) {
            mobi.setFirstNonBookRecord(fnonbook);
        }

        /*
         * Done inserting content
         */
        mobi.setLastContentRecord(curr - 1);

        curr += insertFlis(curr);

        curr += insertFcis(curr);

        insertEof(curr);

        buildRecordZero();
    }

    protected void setUnsupportedRecordPointers() {
        /*
         * FIXME: Deal with these records
         */
        mobi.setFcisRecord(-1);
        mobi.setFlisRecord(-1);
        mobi.setHuffmanRecord(0);
        mobi.setIndxRecord(-1);
        mobi.setHuffmanCount(0);
    }

    protected int insertText(int record) {
        ensureTextCodec();
        byte[][] records = text.getCompressedRecords();
        ListIterator<PdbRecord> it = getToc().iterator(record);
        for (byte[] i : records)
            it.add(new PdbRecord(i));

        /*
         * Update mobi header
         */
        mobi.setEncoding(text.getEncoding());
        mobi.setFirstContentRecord(record);

        /*
         * And palm
         */
        palm.setUncompressedTextLength(text.getUncompressedLength());
        palm.setTextRecordCount(records.length);

        return records.length;
    }

    protected void ensureTextCodec() {
        Codec codec = codec_manager.getCodec(palm.getCompression().toString());
        getText().setCodec(codec);
    }

    protected int insertImages(int record) {
        ListIterator<PdbRecord> it_toc = getToc().iterator(record);
        Iterator<ByteBuffer> it_img = images.iterator();
        while (it_img.hasNext())
            it_toc.add(new PdbRecord(it_img.next().array()));

        /*
         * Update Mobi image record pointer
         */
        mobi.setFirstImageRecord(images.size() > 0 ? record : -1);

        return images.size();
    }

    protected int insertFlis(int record) {
        if (write_flis) {
            mobi.setFlisRecord(record);
            PdbRecord flis = new PdbRecord(FlisRecord.getFields().getBuffer().array());
            getToc().iterator(record).add(flis);

            return 1;
        }

        return 0;
    }

    protected int insertFcis(int record) {
        if (write_fcis) {
            mobi.setFcisRecord(record);
            ByteFieldMapSet fcis = FcisRecord.getFields();

            fcis.<IntByteField> getAs("Text Length").setValue(palm.getUncompressedTextLength());

            PdbRecord rec = new PdbRecord(fcis.getBuffer().array());
            getToc().iterator(record).add(rec);

            return 1;
        }

        return 0;
    }

    protected int insertEof(int record) {
        PdbRecord eof = new PdbRecord(EofRecord.getFields().getBuffer().array());

        getToc().iterator(record).add(eof);

        return 1;
    }

    /**
     * Populate record zero with the {@link PalmDocHeader},
     * {@link MobiDocHeader} and Book Title
     */
    protected void buildRecordZero() {
        int length = PalmDocHeader.LENGTH + mobi.getLength();

        /*
         * Update mobi header information
         */
        byte[] title = getTitle().getBytes(mobi.getEncoding().getCharset());
        mobi.setFullNameOffset(length);
        mobi.setFullNameLength(title.length);

        /*
         * Calculate extra length for padding, 2 bytes after the title to a
         * multiple of 4
         */
        length += title.length + 2;
        if (length % 4 != 0)
            length += 4 - length % 4;
        ByteBuffer buf = ByteBuffer.allocate(length);

        /*
         * Write palm and mobi headers to buffer
         */
        palm.getFields().write(buf);
        mobi.write(buf);
        buf.put(title);
        zero.setData(buf);
    }
}
