/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.zip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipException;

/**
 * ZipExtraField related methods
 * @NotThreadSafe because the HashMap is not synch.
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public class ExtraFieldUtils {

    private static final int WORD = 4;

    /**
     * Static registry of known extra fields.
     */
    private static final Map<ZipShort, Class<?>> implementations;

    static {
        implementations = new ConcurrentHashMap<>();
        register(AsiExtraField.class);
        register(X5455_ExtendedTimestamp.class);
        register(X7875_NewUnix.class);
        register(JarMarker.class);
        register(UnicodePathExtraField.class);
        register(UnicodeCommentExtraField.class);
        register(Zip64ExtendedInformationExtraField.class);
        register(X000A_NTFS.class);
        register(X0014_X509Certificates.class);
        register(X0015_CertificateIdForFile.class);
        register(X0016_CertificateIdForCentralDirectory.class);
        register(X0017_StrongEncryptionHeader.class);
        register(X0019_EncryptionRecipientCertificateList.class);
        register(ResourceAlignmentExtraField.class);
    }

    /**
     * Register a ZipExtraField implementation.
     *
     * <p>The given class must have a no-arg constructor and implement
     * the {@link ZipExtraField ZipExtraField interface}.</p>
     * @param c the class to register
     */
    public static void register(final Class<?> c) {
        try {
            final ZipExtraField ze = (ZipExtraField) c.newInstance();
            implementations.put(ze.getHeaderId(), c);
        } catch (final ClassCastException cc) {
            throw new RuntimeException(c + " doesn\'t implement ZipExtraField"); //NOSONAR
        } catch (final InstantiationException ie) {
            throw new RuntimeException(c + " is not a concrete class"); //NOSONAR
        } catch (final IllegalAccessException ie) {
            throw new RuntimeException(c + "\'s no-arg constructor is not public"); //NOSONAR
        }
    }

    /**
     * Create an instance of the appropriate ExtraField, falls back to
     * {@link UnrecognizedExtraField UnrecognizedExtraField}.
     * @param headerId the header identifier
     * @return an instance of the appropriate ExtraField
     * @throws InstantiationException if unable to instantiate the class
     * @throws IllegalAccessException if not allowed to instantiate the class
     */
    public static ZipExtraField createExtraField(final ZipShort headerId)
        throws InstantiationException, IllegalAccessException {
        final Class<?> c = implementations.get(headerId);
        if (c != null) {
            return (ZipExtraField) c.newInstance();
        }
        final UnrecognizedExtraField u = new UnrecognizedExtraField();
        u.setHeaderId(headerId);
        return u;
    }

    /**
     * Split the array into ExtraFields and populate them with the
     * given data as local file data, throwing an exception if the
     * data cannot be parsed.
     * @param data an array of bytes as it appears in local file data
     * @return an array of ExtraFields
     * @throws ZipException on error
     */
    public static ZipExtraField[] parse(final byte[] data) throws ZipException {
        return parse(data, true, UnparseableExtraField.THROW);
    }

    /**
     * Split the array into ExtraFields and populate them with the
     * given data, throwing an exception if the data cannot be parsed.
     * @param data an array of bytes
     * @param local whether data originates from the local file data
     * or the central directory
     * @return an array of ExtraFields
     * @throws ZipException on error
     */
    public static ZipExtraField[] parse(final byte[] data, final boolean local)
        throws ZipException {
        return parse(data, local, UnparseableExtraField.THROW);
    }

    /**
     * Split the array into ExtraFields and populate them with the
     * given data.
     * @param data an array of bytes
     * @param local whether data originates from the local file data
     * or the central directory
     * @param onUnparseableData what to do if the extra field data
     * cannot be parsed.
     * @return an array of ExtraFields
     * @throws ZipException on error
     *
     * @since 1.1
     */
    public static ZipExtraField[] parse(final byte[] data, final boolean local,
                                        final UnparseableExtraField onUnparseableData)
        throws ZipException {
        final List<ZipExtraField> v = new ArrayList<>();
        int start = 0;
        LOOP:
        while (start <= data.length - WORD) {
            final ZipShort headerId = new ZipShort(data, start);
            final int length = new ZipShort(data, start + 2).getValue();
            if (start + WORD + length > data.length) {
                switch(onUnparseableData.getKey()) {
                case UnparseableExtraField.THROW_KEY:
                    throw new ZipException("bad extra field starting at "
                                           + start + ".  Block length of "
                                           + length + " bytes exceeds remaining"
                                           + " data of "
                                           + (data.length - start - WORD)
                                           + " bytes.");
                case UnparseableExtraField.READ_KEY:
                    final UnparseableExtraFieldData field =
                        new UnparseableExtraFieldData();
                    if (local) {
                        field.parseFromLocalFileData(data, start,
                                                     data.length - start);
                    } else {
                        field.parseFromCentralDirectoryData(data, start,
                                                            data.length - start);
                    }
                    v.add(field);
                    //$FALL-THROUGH$
                case UnparseableExtraField.SKIP_KEY:
                    // since we cannot parse the data we must assume
                    // the extra field consumes the whole rest of the
                    // available data
                    break LOOP;
                default:
                    throw new ZipException("unknown UnparseableExtraField key: "
                                           + onUnparseableData.getKey());
                }
            }
            try {
                final ZipExtraField ze = createExtraField(headerId);
                try {
                    if (local) {
                        ze.parseFromLocalFileData(data, start + WORD, length);
                    } else {
                        ze.parseFromCentralDirectoryData(data, start + WORD, length);
                    }
                } catch (ArrayIndexOutOfBoundsException aiobe) {
                    throw (ZipException) new ZipException("Failed to parse corrupt ZIP extra field of type "
                        + Integer.toHexString(headerId.getValue())).initCause(aiobe);
                }
                v.add(ze);
            } catch (final InstantiationException | IllegalAccessException ie) {
                throw (ZipException) new ZipException(ie.getMessage()).initCause(ie);
            }
            start += length + WORD;
        }

        final ZipExtraField[] result = new ZipExtraField[v.size()];
        return v.toArray(result);
    }

    /**
     * Merges the local file data fields of the given ZipExtraFields.
     * @param data an array of ExtraFiles
     * @return an array of bytes
     */
    public static byte[] mergeLocalFileDataData(final ZipExtraField[] data) {
        final boolean lastIsUnparseableHolder = data.length > 0
            && data[data.length - 1] instanceof UnparseableExtraFieldData;
        final int regularExtraFieldCount =
            lastIsUnparseableHolder ? data.length - 1 : data.length;

        int sum = WORD * regularExtraFieldCount;
        for (final ZipExtraField element : data) {
            sum += element.getLocalFileDataLength().getValue();
        }

        final byte[] result = new byte[sum];
        int start = 0;
        for (int i = 0; i < regularExtraFieldCount; i++) {
            System.arraycopy(data[i].getHeaderId().getBytes(),
                             0, result, start, 2);
            System.arraycopy(data[i].getLocalFileDataLength().getBytes(),
                             0, result, start + 2, 2);
            start += WORD;
            final byte[] local = data[i].getLocalFileDataData();
            if (local != null) {
                System.arraycopy(local, 0, result, start, local.length);
                start += local.length;
            }
        }
        if (lastIsUnparseableHolder) {
            final byte[] local = data[data.length - 1].getLocalFileDataData();
            if (local != null) {
                System.arraycopy(local, 0, result, start, local.length);
            }
        }
        return result;
    }

    /**
     * Merges the central directory fields of the given ZipExtraFields.
     * @param data an array of ExtraFields
     * @return an array of bytes
     */
    public static byte[] mergeCentralDirectoryData(final ZipExtraField[] data) {
        final boolean lastIsUnparseableHolder = data.length > 0
            && data[data.length - 1] instanceof UnparseableExtraFieldData;
        final int regularExtraFieldCount =
            lastIsUnparseableHolder ? data.length - 1 : data.length;

        int sum = WORD * regularExtraFieldCount;
        for (final ZipExtraField element : data) {
            sum += element.getCentralDirectoryLength().getValue();
        }
        final byte[] result = new byte[sum];
        int start = 0;
        for (int i = 0; i < regularExtraFieldCount; i++) {
            System.arraycopy(data[i].getHeaderId().getBytes(),
                             0, result, start, 2);
            System.arraycopy(data[i].getCentralDirectoryLength().getBytes(),
                             0, result, start + 2, 2);
            start += WORD;
            final byte[] local = data[i].getCentralDirectoryData();
            if (local != null) {
                System.arraycopy(local, 0, result, start, local.length);
                start += local.length;
            }
        }
        if (lastIsUnparseableHolder) {
            final byte[] local = data[data.length - 1].getCentralDirectoryData();
            if (local != null) {
                System.arraycopy(local, 0, result, start, local.length);
            }
        }
        return result;
    }

    /**
     * "enum" for the possible actions to take if the extra field
     * cannot be parsed.
     *
     * @since 1.1
     */
    public static final class UnparseableExtraField {
        /**
         * Key for "throw an exception" action.
         */
        public static final int THROW_KEY = 0;
        /**
         * Key for "skip" action.
         */
        public static final int SKIP_KEY = 1;
        /**
         * Key for "read" action.
         */
        public static final int READ_KEY = 2;

        /**
         * Throw an exception if field cannot be parsed.
         */
        public static final UnparseableExtraField THROW
            = new UnparseableExtraField(THROW_KEY);

        /**
         * Skip the extra field entirely and don't make its data
         * available - effectively removing the extra field data.
         */
        public static final UnparseableExtraField SKIP
            = new UnparseableExtraField(SKIP_KEY);

        /**
         * Read the extra field data into an instance of {@link
         * UnparseableExtraFieldData UnparseableExtraFieldData}.
         */
        public static final UnparseableExtraField READ
            = new UnparseableExtraField(READ_KEY);

        private final int key;

        private UnparseableExtraField(final int k) {
            key = k;
        }

        /**
         * Key of the action to take.
         * @return the key
         */
        public int getKey() { return key; }
    }
}
