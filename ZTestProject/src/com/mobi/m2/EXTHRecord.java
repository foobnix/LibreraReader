package com.mobi.m2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EXTHRecord {

    public byte[] recordType = { 0, 0, 0, 0 };
    public byte[] recordLength = { 0, 0, 0, 0 };
    public byte[] recordData = null;

    public static String getDescriptionForType(int type) {
        return ExthField.getById(type).getTitle();
    }

    public EXTHRecord(int recType, String data, String characterEncoding) {
        this(recType, StreamUtils.stringToByteArray(data, characterEncoding));
    }

    public EXTHRecord(int recType, boolean data) {
        StreamUtils.intToByteArray(recType, recordType);
        recordData = new byte[1];
        recordData[0] = data ? (byte) 1 : 0;
        StreamUtils.intToByteArray(size(), recordLength);
    }

    public EXTHRecord(int recType, byte[] data) {
        StreamUtils.intToByteArray(recType, recordType);
        int len = (data == null) ? 0 : data.length;
        StreamUtils.intToByteArray(len + 8, recordLength);
        recordData = new byte[len];
        if (len > 0) {
            System.arraycopy(data, 0, recordData, 0, len);
        }
    }

    public EXTHRecord(InputStream in) throws IOException {
        MobiCommon.logMessage("*** EXTHRecord ***");

        StreamUtils.readByteArray(in, recordType);
        StreamUtils.readByteArray(in, recordLength);

        int len = StreamUtils.byteArrayToInt(recordLength);
        if (len < 8) throw new IOException("Invalid EXTH record length");

        recordData = new byte[len - 8];
        StreamUtils.readByteArray(in, recordData);

        if (MobiCommon.debug) {
            int recType = StreamUtils.byteArrayToInt(recordType);
            System.out.print("EXTH record type: ");
            switch (recType) {
                case 100:
                    MobiCommon.logMessage("author");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 101:
                    MobiCommon.logMessage("publisher");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 103:
                    MobiCommon.logMessage("description");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 104:
                    MobiCommon.logMessage("isbn");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 105:
                    MobiCommon.logMessage("subject");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 106:
                    MobiCommon.logMessage("publishingdate");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 109:
                    MobiCommon.logMessage("rights");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 113:
                case 504:
                    MobiCommon.logMessage("asin");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 118:
                    MobiCommon.logMessage("retail price");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 119:
                    MobiCommon.logMessage("retail price currency");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 200:
                    MobiCommon.logMessage("dictionary short name");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                case 404:
                    MobiCommon.logMessage("text to speech");
                    int ttsflag = StreamUtils.byteArrayToInt(recordData);
                    MobiCommon.logMessage((ttsflag == 0) ? "enabled" : "disabled");
                    break;
                case 501:
                    MobiCommon.logMessage("cdetype");
                    MobiCommon.logMessage
                            (StreamUtils.byteArrayToString(recordData));
                    break;
                default:
                    MobiCommon.logMessage(Integer.toString(recType));
            }
        }
    }

    public int getRecordType() {
        return StreamUtils.byteArrayToInt(recordType);
    }

    public byte[] getData() {
        return recordData;
    }

    public int getDataLength() {
        return recordData.length;
    }

    public int size() {
        return getDataLength() + 8;
    }

    public void setData(String s, String encoding) {
        recordData = StreamUtils.stringToByteArray(s, encoding);
        StreamUtils.intToByteArray(size(), recordLength);
    }

    private void setData(int value) {
        if (recordData == null) {
            recordData = new byte[4];
            StreamUtils.intToByteArray(size(), recordLength);
        }

        StreamUtils.intToByteArray(value, recordData);
    }

    public void setData(boolean value) {
        if (recordData == null) {
            recordData = new byte[1];
            StreamUtils.intToByteArray(size(), recordLength);
        }

        StreamUtils.intToByteArray(value ? 1 : 0, recordData);
    }

    public EXTHRecord copy() {
        return new EXTHRecord(StreamUtils.byteArrayToInt(recordType),
                recordData);
    }

    public boolean isKnownType() {
        int typeId = StreamUtils.byteArrayToInt(recordType);
        return ExthField.isKnown(typeId);
    }

    public String getTypeDescription() {
        return getDescriptionForType(StreamUtils.byteArrayToInt(recordType));
    }

    public void write(OutputStream out) throws IOException {
        if (MobiCommon.debug) {
            MobiCommon.logMessage("*** Write EXTHRecord ***");
            MobiCommon.logMessage(StreamUtils.dumpByteArray(recordType));
            MobiCommon.logMessage(StreamUtils.dumpByteArray(recordLength));
            MobiCommon.logMessage(StreamUtils.dumpByteArray(recordData));
            MobiCommon.logMessage("************************");
        }
        out.write(recordType);
        out.write(recordLength);
        out.write(recordData);
    }
}
