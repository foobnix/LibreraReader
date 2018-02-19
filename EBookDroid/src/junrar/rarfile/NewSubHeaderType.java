/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 24.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 * 
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
package junrar.rarfile;

import java.util.Arrays;

/**
 * subheaders new version of the info headers
 *
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class NewSubHeaderType {
    
    /**
     * comment subheader
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_CMT = new NewSubHeaderType(new byte[]{'C','M','T'});
    /**
     * 
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_ACL = new NewSubHeaderType(new byte[]{'A','C','L'});
    /**
     * 
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_STREAM = new NewSubHeaderType(new byte[]{'S','T','M'});
    /**
     * 
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_UOWNER = new NewSubHeaderType(new byte[]{'U','O','W'});
    /**
     * 
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_AV = new NewSubHeaderType(new byte[]{'A','V'});
    /**
     * recovery record subheader
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_RR = new NewSubHeaderType(new byte[]{'R','R'});
    /**
     * 
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_OS2EA = new NewSubHeaderType(new byte[]{'E','A','2'});
    /**
     * 
     */
    public static final NewSubHeaderType SUBHEAD_TYPE_BEOSEA = new NewSubHeaderType(new byte[]{'E','A','B','E'});
    
    private byte[] headerTypes;
    
    /**
     * Private constructor
     * @param headerTypes
     */
    private NewSubHeaderType(byte[] headerTypes)
    {
        this.headerTypes = headerTypes;
    }
    
    /**
     * @param toCompare
     * @return Returns true if the given byte array matches to the internal byte array of this header.
     */
    public boolean byteEquals(byte[] toCompare)
    {
        return Arrays.equals(this.headerTypes, toCompare);
    }

    @Override
    public String toString()
    {
        return new String(this.headerTypes);
    }
}
