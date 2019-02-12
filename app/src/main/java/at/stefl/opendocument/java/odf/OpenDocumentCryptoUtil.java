package at.stefl.opendocument.java.odf;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import at.stefl.commons.io.ByteStreamUtil;
import at.stefl.commons.io.CountingInputStream;
import at.stefl.commons.io.LimitedInputStream;
import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.comparator.MapEntryValueComparator;
import de.rtner.security.auth.spi.MacBasedPRF;
import de.rtner.security.auth.spi.PBKDF2Engine;
import de.rtner.security.auth.spi.PBKDF2Parameters;

// TODO: improve class design
public class OpenDocumentCryptoUtil {
    
    private static final Comparator<EncryptionParameter> PLAIN_SIZE_COMPERATOR = new Comparator<EncryptionParameter>() {
        @Override
        public int compare(EncryptionParameter o1, EncryptionParameter o2) {
            return o1.getPlainSize() - o2.getPlainSize();
        }
    };
    
    private static final MapEntryValueComparator<EncryptionParameter> ENTRY_PLAIN_SIZE_COMPERATOR = new MapEntryValueComparator<EncryptionParameter>(
            PLAIN_SIZE_COMPERATOR);
    
    public static int getDeflatedSize(InputStream in) throws IOException {
    	in = new InflaterInputStream(in, new Inflater(true), 1);
        CountingInputStream cin = new CountingInputStream(in);
        ByteStreamUtil.flushBytewise(cin);
        return (int) cin.count();
    }
    
    public static boolean validatePassword(String password,
            OpenDocumentFile documentFile) throws IOException {
        Map<String, EncryptionParameter> encryptionParameterMap = documentFile
                .getEncryptionParameterMap();
        
        Map.Entry<String, EncryptionParameter> smallest = CollectionUtil
                .getSmallest(ENTRY_PLAIN_SIZE_COMPERATOR,
                        encryptionParameterMap.entrySet());
        
        if (smallest == null) return true;
        String path = smallest.getKey();
        EncryptionParameter encryptionParameter = smallest.getValue();
        InputStream in = getDecryptedInputStream(
                documentFile.getRawFileStream(path), encryptionParameter,
                password);
        return validatePassword(password, encryptionParameter, in);
    }
    
    public static boolean validatePassword(String password,
            EncryptionParameter encryptionParameter, InputStream in)
            throws IOException {
        try {
            String checksumAlgorithm = encryptionParameter
                    .getChecksumAlgorithm();
            byte[] checksum = encryptionParameter.getChecksum();
            int checksumUsedSize = encryptionParameter.getChecksumUsedSize();
            
            MessageDigest digest = MessageDigest.getInstance(checksumAlgorithm);
            in = new DigestInputStream(in, digest);
            in = new LimitedInputStream(in, checksumUsedSize);
            in = new InflaterInputStream(in, new Inflater(true), 1);
            try {
            	ByteStreamUtil.flushBytewise(in);
            } catch (EOFException ee) {
            	// TODO: could be logged
            }
            byte[] calculatedChecksum = digest.digest();
            
            if (!Arrays.equals(checksum, calculatedChecksum)) return false;
        } catch (ZipException e) {
            // TODO: sufficient?
            return false;
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedEncryptionException(
                    "unsupported message digest: "
                            + encryptionParameter.getChecksumAlgorithm(), e);
        }
        
        return true;
    }
    
    public static InputStream getDecryptedInputStream(InputStream in,
            EncryptionParameter encryptionParameter, String password) {
        String algorithm = encryptionParameter.getAlgorithm();
        String transformation = encryptionParameter.getTransformation();
        
        int keySize = encryptionParameter.getKeyDerivationKeySize();
        String startKeyGeneration = encryptionParameter.getStartKeyGeneration();
        
        try {
            // TODO: password charset
            byte[] passwordBytes = password.getBytes();
            
            MessageDigest digest = MessageDigest
                    .getInstance(startKeyGeneration);
            byte[] startKey = digest.digest(passwordBytes);
            
            byte[] salt = encryptionParameter.getKeyDerivationSalt();
            int iterationCount = encryptionParameter
                    .getKeyDerivationIterationCount();
            
            MacBasedPRF macBasedPRF = new MacBasedPRF("HmacSHA1");
            PBKDF2Parameters pbkdf2Parameters = new PBKDF2Parameters(salt,
                    iterationCount);
            PBKDF2Engine pbkdf2Engine = new PBKDF2Engine(pbkdf2Parameters,
                    macBasedPRF);
            byte[] dk = pbkdf2Engine.deriveKey(startKey, keySize);
            Key key = new SecretKeySpec(dk, algorithm);
            
            byte[] initialisationVector = encryptionParameter
                    .getInitialisationVector();
            IvParameterSpec iv = new IvParameterSpec(initialisationVector);
            
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            return new CipherInputStream(in, cipher);
        } catch (Exception e) {
            throw new UnsupportedEncryptionException(e);
        }
    }
    
    public OpenDocumentCryptoUtil() {}
    
}