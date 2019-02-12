/*
* Copyright 2010 Srikanth Reddy Lingala  
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License. 
* You may obtain a copy of the License at 
* 
* http://www.apache.org/licenses/LICENSE-2.0 
* 
* Unless required by applicable law or agreed to in writing, 
* software distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*/

package net.lingala.zip4j.crypto.PBKDF2;

/*
 * Source referred from Matthias Gartner's PKCS#5 implementation - 
 * see http://rtner.de/software/PBKDF2.html 
 */

class PBKDF2HexFormatter
{
    public boolean fromString(PBKDF2Parameters p, String s)
    {
        if (p == null || s == null)
        {
            return true;
        }

        String[] p123 = s.split(":");
        if (p123 == null || p123.length != 3)
        {
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

    public String toString(PBKDF2Parameters p)
    {
        String s = BinTools.bin2hex(p.getSalt()) + ":"
                + String.valueOf(p.getIterationCount()) + ":"
                + BinTools.bin2hex(p.getDerivedKey());
        return s;
    }
}
