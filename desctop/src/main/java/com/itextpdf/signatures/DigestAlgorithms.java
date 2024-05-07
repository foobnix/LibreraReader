    /*
 *
 * This file is part of the iText (R) project.
    Copyright (c) 1998-2021 iText Group NV
 * Authors: Bruno Lowagie, Paulo Soares, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */
package com.itextpdf.signatures;

	import java.io.IOException;
	import java.io.InputStream;
	import java.security.GeneralSecurityException;
	import java.security.MessageDigest;
	import java.security.NoSuchAlgorithmException;
	import java.security.NoSuchProviderException;
	import java.util.HashMap;
	import java.util.Map;

	import com.itextpdf.signatures.exceptions.SignExceptionMessageConstant;

	/**
	 * Class that contains a map with the different message digest algorithms.
	 */
	public class DigestAlgorithms {

		/**
		 * Algorithm available for signatures since PDF 1.3.
		 */
		public static final String SHA1 = "SHA-1";

		/**
		 * Algorithm available for signatures since PDF 1.6.
		 */
		public static final String SHA256 = "SHA-256";

		/**
		 * Algorithm available for signatures since PDF 1.7.
		 */
		public static final String SHA384 = "SHA-384";

		/**
		 * Algorithm available for signatures since PDF 1.7.
		 */
		public static final String SHA512 = "SHA-512";

		/**
		 * Algorithm available for signatures since PDF 1.7.
		 */
		public static final String RIPEMD160 = "RIPEMD160";

		/**
		 * Maps the digest IDs with the human-readable name of the digest algorithm.
		 */
		private static final Map<String, String> digestNames = new HashMap<>();

		/**
		 * Maps digest algorithm that are unknown by the JDKs MessageDigest object to a known one.
		 */
		private static final Map<String, String> fixNames = new HashMap<>();

		/**
		 * Maps the name of a digest algorithm with its ID.
		 */
		private static final Map<String, String> allowedDigests = new HashMap<>();

		static {
			digestNames.put("1.2.840.113549.2.5", "MD5");
			digestNames.put("1.2.840.113549.2.2", "MD2");
			digestNames.put("1.3.14.3.2.26", "SHA1");
			digestNames.put("2.16.840.1.101.3.4.2.4", "SHA224");
			digestNames.put("2.16.840.1.101.3.4.2.1", "SHA256");
			digestNames.put("2.16.840.1.101.3.4.2.2", "SHA384");
			digestNames.put("2.16.840.1.101.3.4.2.3", "SHA512");
			digestNames.put("1.3.36.3.2.2", "RIPEMD128");
			digestNames.put("1.3.36.3.2.1", "RIPEMD160");
			digestNames.put("1.3.36.3.2.3", "RIPEMD256");
			digestNames.put("1.2.840.113549.1.1.4", "MD5");
			digestNames.put("1.2.840.113549.1.1.2", "MD2");
			digestNames.put("1.2.840.113549.1.1.5", "SHA1");
			digestNames.put("1.2.840.113549.1.1.14", "SHA224");
			digestNames.put("1.2.840.113549.1.1.11", "SHA256");
			digestNames.put("1.2.840.113549.1.1.12", "SHA384");
			digestNames.put("1.2.840.113549.1.1.13", "SHA512");
			digestNames.put("1.2.840.113549.2.5", "MD5");
			digestNames.put("1.2.840.113549.2.2", "MD2");
			digestNames.put("1.2.840.10040.4.3", "SHA1");
			digestNames.put("1.3.14.3.2.29", "SHA1");
			digestNames.put("2.16.840.1.101.3.4.3.1", "SHA224");
			digestNames.put("2.16.840.1.101.3.4.3.2", "SHA256");
			digestNames.put("2.16.840.1.101.3.4.3.3", "SHA384");
			digestNames.put("2.16.840.1.101.3.4.3.4", "SHA512");
			digestNames.put("1.3.36.3.3.1.3", "RIPEMD128");
			digestNames.put("1.3.36.3.3.1.2", "RIPEMD160");
			digestNames.put("1.3.36.3.3.1.4", "RIPEMD256");
			digestNames.put("1.2.643.2.2.9", "GOST3411");
			digestNames.put("1.2.156.10197.1.401", "SM3");

			fixNames.put("SHA256", SHA256);
			fixNames.put("SHA384", SHA384);
			fixNames.put("SHA512", SHA512);

			allowedDigests.put("MD2", "1.2.840.113549.2.2");
			allowedDigests.put("MD-2", "1.2.840.113549.2.2");
			allowedDigests.put("MD5", "1.2.840.113549.2.5");
			allowedDigests.put("MD-5", "1.2.840.113549.2.5");
			allowedDigests.put("SHA1", "1.3.14.3.2.26");
			allowedDigests.put("SHA-1", "1.3.14.3.2.26");
			allowedDigests.put("SHA224", "2.16.840.1.101.3.4.2.4");
			allowedDigests.put("SHA-224", "2.16.840.1.101.3.4.2.4");
			allowedDigests.put("SHA256", "2.16.840.1.101.3.4.2.1");
			allowedDigests.put("SHA-256", "2.16.840.1.101.3.4.2.1");
			allowedDigests.put("SHA384", "2.16.840.1.101.3.4.2.2");
			allowedDigests.put("SHA-384", "2.16.840.1.101.3.4.2.2");
			allowedDigests.put("SHA512", "2.16.840.1.101.3.4.2.3");
			allowedDigests.put("SHA-512", "2.16.840.1.101.3.4.2.3");
			allowedDigests.put("RIPEMD128", "1.3.36.3.2.2");
			allowedDigests.put("RIPEMD-128", "1.3.36.3.2.2");
			allowedDigests.put("RIPEMD160", "1.3.36.3.2.1");
			allowedDigests.put("RIPEMD-160", "1.3.36.3.2.1");
			allowedDigests.put("RIPEMD256", "1.3.36.3.2.3");
			allowedDigests.put("RIPEMD-256", "1.3.36.3.2.3");
			allowedDigests.put("GOST3411", "1.2.643.2.2.9");
			allowedDigests.put("SM3", "1.2.156.10197.1.401");
		}

		/**
		 * Get a digest algorithm.
		 *
		 * @param digestOid oid of the digest algorithm
		 * @param provider the provider you want to use to create the hash
		 * @return MessageDigest object
		 * @throws NoSuchAlgorithmException thrown when a particular cryptographic algorithm is
		 * requested but is not available in the environment
		 * @throws NoSuchProviderException thrown when a particular security provider is
		 * requested but is not available in the environment
		 */
		public static MessageDigest getMessageDigestFromOid(String digestOid, String provider)
				throws NoSuchAlgorithmException, NoSuchProviderException {
			return getMessageDigest(getDigest(digestOid), provider);
		}

		/**
		 * Creates a MessageDigest object that can be used to create a hash.
		 *
		 * @param hashAlgorithm	the algorithm you want to use to create a hash
		 * @param provider	the provider you want to use to create the hash
		 * @return	a MessageDigest object
		 * @throws NoSuchAlgorithmException thrown when a particular cryptographic algorithm is
		 * requested but is not available in the environment
		 * @throws NoSuchProviderException thrown when a particular security provider is
		 * requested but is not available in the environment
		 */
		public static MessageDigest getMessageDigest(String hashAlgorithm, String provider)
				throws NoSuchAlgorithmException, NoSuchProviderException {
			return SignUtils.getMessageDigest(hashAlgorithm, provider);
		}

		/**
		 * Creates a hash using a specific digest algorithm and a provider.
		 *
		 * @param data	the message of which you want to create a hash
		 * @param hashAlgorithm	the algorithm used to create the hash
		 * @param provider	the provider used to create the hash
		 * @return	the hash
		 * @throws GeneralSecurityException when requested cryptographic algorithm or security provider
		 * is not available
		 * @throws IOException signals that an I/O exception has occurred
		 */
		public static byte[] digest(InputStream data, String hashAlgorithm, String provider)
				throws GeneralSecurityException, IOException {
			MessageDigest messageDigest = getMessageDigest(hashAlgorithm, provider);
			return digest(data, messageDigest);
		}

		/**
		 * Create a digest based on the inputstream.
		 *
		 * @param data data to be digested
		 * @param messageDigest algorithm to be used
		 * @return digest of the data
		 * @throws IOException signals that an I/O exception has occurred
		 */
		public static byte[] digest(InputStream data, MessageDigest messageDigest)
				throws IOException {
			byte[] buf = new byte[8192];
			int n;
			while ((n = data.read(buf)) > 0) {
				messageDigest.update(buf, 0, n);
			}
			return messageDigest.digest();
		}

		/**
		 * Gets the digest name for a certain id
		 *
		 * @param oid	an id (for instance "1.2.840.113549.2.5")
		 * @return	a digest name (for instance "MD5")
		 */
		public static String getDigest(String oid) {
			String ret = digestNames.get(oid);
			if (ret == null)
				return oid;
			else
				return ret;
		}

		/**
		 * Normalize the digest name.
		 *
		 * @param algo the name to be normalized
		 * @return normalized name
		 */
		public static String normalizeDigestName(String algo) {
			if (fixNames.containsKey(algo))
				return fixNames.get(algo);
			return algo;
		}

		/**
		 * Returns the id of a digest algorithms that is allowed in PDF,
		 * or null if it isn't allowed.
		 *
		 * @param name	The name of the digest algorithm.
		 * @return	An oid.
		 */
		public static String getAllowedDigest(String name) {
			if (name == null) {
				throw new IllegalArgumentException(SignExceptionMessageConstant.THE_NAME_OF_THE_DIGEST_ALGORITHM_IS_NULL);
			}
			return allowedDigests.get(name.toUpperCase());
		}
	}
