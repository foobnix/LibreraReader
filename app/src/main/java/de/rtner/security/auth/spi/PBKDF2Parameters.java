package de.rtner.security.auth.spi;

/**
 * <p>
 * Parameter data holder for PBKDF2 configuration.
 * </p>
 * <hr />
 * <p>
 * A free Java implementation of Password Based Key Derivation Function 2 as
 * defined by RFC 2898. Copyright (c) 2007 Matthias G&auml;rtner
 * </p>
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * </p>
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * </p>
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * </p>
 * <p>
 * For Details, see <a href
 * ="http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html">http://www.gnu.org
 * /licenses/old-licenses/lgpl-2.1.html</a>.
 * </p>
 * 
 * @author Matthias G&auml;rtner
 * @version 1.0
 */
public class PBKDF2Parameters {
    
    protected byte[] salt;
    
    protected int iterationCount;
    
    protected String hashAlgorithm;
    
    protected String hashCharset;
    
    /**
     * The derived key is actually only a convenience to store a reference
     * derived key. It is not used during computation.
     */
    protected byte[] derivedKey;
    
    /**
     * Constructor. Defaults to <code>null</code> for byte arrays, UTF-8 as
     * character set and 1000 for iteration count.
     */
    public PBKDF2Parameters() {
        this.hashAlgorithm = null;
        this.hashCharset = "UTF-8";
        this.salt = null;
        this.iterationCount = 1000;
        this.derivedKey = null;
    }
    
    /**
     * Constructor. Defaults to <code>null</code> for byte arrays, UTF-8 as
     * character.
     */
    public PBKDF2Parameters(byte[] salt, int iterationCount) {
        this.hashAlgorithm = null;
        this.hashCharset = "UTF-8";
        this.salt = salt;
        this.iterationCount = iterationCount;
        this.derivedKey = null;
    }
    
    /**
     * Constructor.
     * 
     * @param hashAlgorithm
     *            for example HMacSHA1 or HMacMD5
     * @param hashCharset
     *            for example UTF-8
     * @param salt
     *            Salt as byte array, may be <code>null</code> (not recommended)
     * @param iterationCount
     *            Number of iterations to execute. Recommended value 1000.
     */
    public PBKDF2Parameters(String hashAlgorithm, String hashCharset,
            byte[] salt, int iterationCount) {
        this.hashAlgorithm = hashAlgorithm;
        this.hashCharset = hashCharset;
        this.salt = salt;
        this.iterationCount = iterationCount;
        this.derivedKey = null;
    }
    
    /**
     * Constructor.
     * 
     * @param hashAlgorithm
     *            for example HMacSHA1 or HMacMD5
     * @param hashCharset
     *            for example UTF-8
     * @param salt
     *            Salt as byte array, may be <code>null</code> (not recommended)
     * @param iterationCount
     *            Number of iterations to execute. Recommended value 1000.
     * @param derivedKey
     *            Convenience data holder, not used during computation.
     */
    public PBKDF2Parameters(String hashAlgorithm, String hashCharset,
            byte[] salt, int iterationCount, byte[] derivedKey) {
        this.hashAlgorithm = hashAlgorithm;
        this.hashCharset = hashCharset;
        this.salt = salt;
        this.iterationCount = iterationCount;
        this.derivedKey = derivedKey;
    }
    
    public int getIterationCount() {
        return iterationCount;
    }
    
    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }
    
    public byte[] getSalt() {
        return salt;
    }
    
    public void setSalt(byte[] salt) {
        this.salt = salt;
    }
    
    public byte[] getDerivedKey() {
        return derivedKey;
    }
    
    public void setDerivedKey(byte[] derivedKey) {
        this.derivedKey = derivedKey;
    }
    
    public String getHashAlgorithm() {
        return hashAlgorithm;
    }
    
    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
    
    public String getHashCharset() {
        return hashCharset;
    }
    
    public void setHashCharset(String hashCharset) {
        this.hashCharset = hashCharset;
    }
}
