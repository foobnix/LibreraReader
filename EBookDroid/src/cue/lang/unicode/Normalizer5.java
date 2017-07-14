/*
   Copyright 2009 IBM Corp

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package cue.lang.unicode;

import java.io.ObjectInputStream;

/**
 * A "poor man's" normalizer, which at least knows how to turn accented
 * characters into their normal-form representations. This is used as a
 * fall-back when Java 6 is not available.
 * 
 * @author Jonathan Feinberg <jdf@us.ibm.com>
 * 
 */
class Normalizer5 extends Normalizer {
	private static final char[] TABLE;
	static {
		try {
			final ObjectInputStream in = new ObjectInputStream(
					Normalizer5.class.getResourceAsStream("normtable.bin"));
			try {
				TABLE = (char[]) in.readObject();
			} finally {
				in.close();
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Normalizer5() {
	}

	@Override
	public String normalize(final String s) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0, len = s.length(); i < len; i++) {
			final char c = s.charAt(i);
			sb.append(c >= TABLE.length ? c : TABLE[c]);
		}
		return sb.toString();
	}
}
