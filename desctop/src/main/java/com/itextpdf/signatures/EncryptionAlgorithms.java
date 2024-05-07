/*

    This file is part of the iText (R) project.
    Copyright (c) 1998-2021 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.signatures;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that contains a map with the different encryption algorithms.
 */
public class EncryptionAlgorithms {

    /** Maps IDs of encryption algorithms with its human-readable name. */
    static final Map<String, String> algorithmNames = new HashMap<>();

    static {
        algorithmNames.put("1.2.840.113549.1.1.1", "RSA");
        algorithmNames.put("1.2.840.10040.4.1", "DSA");
        algorithmNames.put("1.2.840.113549.1.1.2", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.4", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.5", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.14", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.11", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.12", "RSA");
        algorithmNames.put("1.2.840.113549.1.1.13", "RSA");
        algorithmNames.put("1.2.840.10040.4.3", "DSA");
        algorithmNames.put("2.16.840.1.101.3.4.3.1", "DSA");
        algorithmNames.put("2.16.840.1.101.3.4.3.2", "DSA");
        algorithmNames.put("1.3.14.3.2.29", "RSA");
        algorithmNames.put("1.3.36.3.3.1.2", "RSA");
        algorithmNames.put("1.3.36.3.3.1.3", "RSA");
        algorithmNames.put("1.3.36.3.3.1.4", "RSA");
        algorithmNames.put("1.2.643.2.2.19", "ECGOST3410");

        algorithmNames.put("1.2.840.10045.2.1", "ECDSA"); //Elliptic curve public key cryptography.
        algorithmNames.put("1.2.840.10045.4.1", "ECDSA"); //Elliptic curve Digital Signature Algorithm (DSA) coupled with the Secure Hashing Algorithm (SHA) algorithm.
        algorithmNames.put("1.2.840.10045.4.3", "ECDSA"); //Elliptic curve Digital Signature Algorithm (DSA).
        algorithmNames.put("1.2.840.10045.4.3.2", "ECDSA"); //Elliptic curve Digital Signature Algorithm (DSA) coupled with the Secure Hashing Algorithm (SHA256) algorithm.
        algorithmNames.put("1.2.840.10045.4.3.3", "ECDSA"); //Elliptic curve Digital Signature Algorithm (DSA) coupled with the Secure Hashing Algorithm (SHA384) algorithm.
        algorithmNames.put("1.2.840.10045.4.3.4", "ECDSA"); //Elliptic curve Digital Signature Algorithm (DSA) coupled with the Secure Hashing Algorithm (SHA512) algorithm.
        algorithmNames.put("1.2.156.10197.1.501", "SM2");
        algorithmNames.put("1.2.156.10197.1.301.1", "SM2");
    }

    /**
     * Gets the algorithm name for a certain id.
     * @param oid	an id (for instance "1.2.840.113549.1.1.1")
     * @return	an algorithm name (for instance "RSA")
     */
    public static String getAlgorithm(String oid) {
        String ret = algorithmNames.get(oid);
        if (ret == null)
            return oid;
        else
            return ret;
    }
}
