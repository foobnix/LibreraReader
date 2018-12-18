package de.rtner.security.auth.spi;

/**
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
 * For Details, see <a
 * href="http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
 * >http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html</a>.
 * </p>
 * 
 * @author Matthias G&auml;rtner
 * @version 1.0
 */
public interface PRF {
    
    /**
     * Initialize this instance with the user-supplied password.
     * 
     * @param P
     *            The password supplied as array of bytes. It is the caller's
     *            task to convert String passwords to bytes as appropriate.
     */
    public void init(byte[] P);
    
    /**
     * Pseudo Random Function
     * 
     * @param M
     *            Input data/message etc. Together with any data supplied during
     *            initilization.
     * @return Random bytes of hLen length.
     */
    public byte[] doFinal(byte[] M);
    
    /**
     * Query block size of underlying algorithm/mechanism.
     * 
     * @return block size
     */
    public int getHLen();
}
