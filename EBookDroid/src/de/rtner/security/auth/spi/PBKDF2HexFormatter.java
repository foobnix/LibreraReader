package de.rtner.security.auth.spi;

import de.rtner.misc.BinTools;

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
public class PBKDF2HexFormatter implements PBKDF2Formatter {
    
    @Override
    public boolean fromString(PBKDF2Parameters p, String s) {
        if (p == null || s == null) {
            return true;
        }
        
        String[] p123 = s.split(":");
        if (p123 == null || p123.length != 3) {
            return true;
        }
        
        byte salt[] = BinTools.hex2bin(p123[0]);
        int iterationCount = Integer.parseInt(p123[1]);
        byte bDK[] = BinTools.hex2bin(p123[2]);
        
        p.setSalt(salt);
        p.setIterationCount(iterationCount);
        p.setDerivedKey(bDK);
        return false;
    }
    
    @Override
    public String toString(PBKDF2Parameters p) {
        String s = BinTools.bin2hex(p.getSalt()) + ":"
                + String.valueOf(p.getIterationCount()) + ":"
                + BinTools.bin2hex(p.getDerivedKey());
        return s;
    }
}
