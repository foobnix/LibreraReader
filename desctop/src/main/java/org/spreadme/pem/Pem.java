package org.spreadme.pem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Base64;

public class Pem {

	public static final String PEM_STRING_X509_OLD = "X509 CERTIFICATE";
	public static final String PEM_STRING_X509 = "CERTIFICATE";
	public static final String PEM_STRING_X509_TRUSTED = "TRUSTED CERTIFICATE";
	public static final String PEM_STRING_X509_REQ_OLD = "NEW CERTIFICATE REQUEST";
	public static final String PEM_STRING_X509_REQ = "CERTIFICATE REQUEST";
	public static final String PEM_STRING_X509_CRL = "X509 CRL";
	public static final String PEM_STRING_EVP_PKEY = "ANY PRIVATE KEY";
	public static final String PEM_STRING_PUBLIC = "PUBLIC KEY";
	public static final String PEM_STRING_RSA = "RSA PRIVATE KEY";
	public static final String PEM_STRING_RSA_PUBLIC = "RSA PUBLIC KEY";
	public static final String PEM_STRING_DSA = "DSA PRIVATE KEY";
	public static final String PEM_STRING_DSA_PUBLIC = "DSA PUBLIC KEY";
	public static final String PEM_STRING_PKCS7 = "PKCS7";
	public static final String PEM_STRING_PKCS7_SIGNED = "PKCS #7 SIGNED DATA";
	public static final String PEM_STRING_PKCS8 = "ENCRYPTED PRIVATE KEY";
	public static final String PEM_STRING_PKCS8INF = "PRIVATE KEY";
	public static final String PEM_STRING_DHPARAMS = "DH PARAMETERS";
	public static final String PEM_STRING_SSL_SESSION = "SSL SESSION PARAMETERS";
	public static final String PEM_STRING_DSAPARAMS = "DSA PARAMETERS";
	public static final String PEM_STRING_ECDSA_PUBLIC = "ECDSA PUBLIC KEY";
	public static final String PEM_STRING_ECPARAMETERS = "EC PARAMETERS";
	public static final String PEM_STRING_ECPRIVATEKEY = "EC PRIVATE KEY";
	public static final String PEM_STRING_PARAMETERS = "PARAMETERS";
	public static final String PEM_STRING_CMS = "CMS";

	public static final String BEGIN = "-----BEGIN ";
	public static final String END = "-----END ";

	private static final String lineSeparator = System.lineSeparator();

	public static void write(OutputStream out, String type, byte[] encoded) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(out));
		writePreEncapsulation(bufferedWriter, type);
		final Base64.Encoder encoder = Base64.getMimeEncoder(64, lineSeparator.getBytes());
		writeContent(bufferedWriter, encoder.encodeToString(encoded));
		writePostEncapsulation(bufferedWriter, type);
		bufferedWriter.flush();
	}

	private static void writePreEncapsulation(BufferedWriter writer, String type) throws IOException {
		writer.write(BEGIN + type + "-----");
		writer.newLine();
	}

	private static void writeContent(BufferedWriter writer, String content) throws IOException {
		writer.write(content);
		writer.newLine();
	}

	private static void writePostEncapsulation(BufferedWriter writer, String type) throws IOException {
		writer.write(END + type + "-----");
	}

	public static byte[] read(InputStream in) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
		final String content = loadContent(bufferedReader);
		return Base64.getDecoder().decode(content);
	}

	private static String loadContent(BufferedReader reader) throws IOException {
		String line = reader.readLine();

		while (line != null && !line.startsWith(BEGIN)) {
			line = reader.readLine();
		}

		StringBuilder content = new StringBuilder();
		if (line != null) {
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(END)) {
					break;
				}
				content.append(line);
			}
		}

		return content.toString();
	}
}
