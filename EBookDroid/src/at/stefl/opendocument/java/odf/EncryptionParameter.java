package at.stefl.opendocument.java.odf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import at.stefl.commons.codec.Base64;
import at.stefl.commons.codec.Base64Settings;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.reader.LWXMLStreamReader;

// TODO: improve parsing
public class EncryptionParameter {
    
    private static final String FILE_ENTRY_ELEMENT = "manifest:file-entry";
    private static final String ENCRYPTION_DATA_ELEMENT = "manifest:encryption-data";
    private static final String ALGORITHM_ELEMENT = "manifest:algorithm";
    private static final String KEY_DERIVATION_ELEMENT = "manifest:key-derivation";
    private static final String START_KEY_GENERATION_ELEMENT = "manifest:start-key-generation";
    
    private static final String FULL_PATH_ATTRIBUTE = "manifest:full-path";
    private static final String PLAIN_SIZE_ATTRIBUTE = "manifest:size";
    
    private static final String CHECKSUM_TYPE_ATTRIBUTE = "manifest:checksum-type";
    private static final String CHECKSUM_ATTRIBUTE = "manifest:checksum";
    
    private static final String ALGORITHM_ATTRIBUTE = "manifest:algorithm-name";
    private static final String INITIALISATION_VECTOR_ATTRIBUTE = "manifest:initialisation-vector";
    
    private static final String KEY_DERIVATION_FUNCTION_ATTRIBUTE = "manifest:key-derivation-name";
    private static final String KEY_DERIVATION_KEY_SIZE_ATTRIBUTE = "manifest:key-size";
    private static final String KEY_DERIVATION_ITERATION_COUNT_ATTRIBUTE = "manifest:iteration-count";
    private static final String KEY_DERIVATION_SALT_ATTRIBUTE = "manifest:salt";
    
    private static final String START_KEY_GENERATION_ATTRIBUTE = "manifest:start-key-generation-name";
    private static final String START_KEY_SIZE_ATTRIBUTE = "manifest:key-size";
    
    private static final String KEY_DERIVATION_FUNCTION = "PBKDF2";
    
    private static final int DEFAULT_CHECKSUM_USED_SIZE = 1024;
    private static final int DEFAULT_KEY_DERIVATION_KEY_SIZE = 16;
    private static final String DEFAULT_START_KEY_GENERATION = "SHA-1";
    
    // TODO: improve
    private static String getDigestAlgorithm(String string) {
        string = string.toLowerCase();
        
        if (string.contains("sha1")) {
            return "SHA-1";
        } else if (string.contains("sha256")) {
            return "SHA-256";
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    public static Map<String, EncryptionParameter> parseEncryptionParameters(
            OpenDocumentFile documentFile) throws IOException {
        Map<String, EncryptionParameter> result = new HashMap<String, EncryptionParameter>();
        LWXMLReader in = new LWXMLStreamReader(documentFile.getManifest());
        
        String element = null;
        String file = null;
        EncryptionParameter encryptionParameter = new EncryptionParameter();
        
        while (true) {
            LWXMLEvent event = in.readEvent();
            if (event == LWXMLEvent.END_DOCUMENT) break;
            
            switch (event) {
            case START_ELEMENT:
                element = in.readValue();
                break;
            case END_ELEMENT:
                if (!in.readValue().equals(FILE_ENTRY_ELEMENT)) continue;
                if (!encryptionParameter.isEmpty()) result.put(file,
                        encryptionParameter);
                encryptionParameter = new EncryptionParameter();
                break;
            case ATTRIBUTE_NAME:
                String attributeName = in.readValue();
                String attributeValue = in.readFollowingValue();
                if (element.equals(FILE_ENTRY_ELEMENT)
                        && attributeName.equals(FULL_PATH_ATTRIBUTE)) {
                    file = attributeValue;
                } else {
                    encryptionParameter.parseParameter(element, attributeName,
                            attributeValue);
                }
                break;
            default:
                break;
            }
        }
        
        in.close();
        return result;
    }
    
    private void parseParameter(String element, String attributeName,
            String attributeValue) {
        if (element.equals(FILE_ENTRY_ELEMENT)) {
            if (attributeName.equals(PLAIN_SIZE_ATTRIBUTE)) {
                parsePlainSize(attributeValue);
            }
        } else if (element.equals(ENCRYPTION_DATA_ELEMENT)) {
            if (attributeName.equals(CHECKSUM_TYPE_ATTRIBUTE)) {
                parseChecksumType(attributeValue);
            } else if (attributeName.equals(CHECKSUM_ATTRIBUTE)) {
                parseChecksum(attributeValue);
            }
        } else if (element.equals(ALGORITHM_ELEMENT)) {
            if (attributeName.equals(ALGORITHM_ATTRIBUTE)) {
                parseAlgorithm(attributeValue);
            } else if (attributeName.equals(INITIALISATION_VECTOR_ATTRIBUTE)) {
                parseInitialisationVector(attributeValue);
            }
        } else if (element.equals(KEY_DERIVATION_ELEMENT)) {
            if (attributeName.equals(KEY_DERIVATION_FUNCTION_ATTRIBUTE)) {
                parseKeyDerivationFunction(attributeValue);
            } else if (attributeName.equals(KEY_DERIVATION_KEY_SIZE_ATTRIBUTE)) {
                parseKeyDerivationKeySize(attributeValue);
            } else if (attributeName
                    .equals(KEY_DERIVATION_ITERATION_COUNT_ATTRIBUTE)) {
                parseKeyDerivationIterationCount(attributeValue);
            } else if (attributeName.equals(KEY_DERIVATION_SALT_ATTRIBUTE)) {
                parseKeyDerivationSalt(attributeValue);
            }
        } else if (element.equals(START_KEY_GENERATION_ELEMENT)) {
            if (attributeName.equals(START_KEY_GENERATION_ATTRIBUTE)) {
                parseStartKeyGeneration(attributeValue);
            } else if (attributeName.equals(START_KEY_SIZE_ATTRIBUTE)) {
                parseStartKeySize(attributeValue);
            }
        }
    }
    
    private void parsePlainSize(String string) {
        int plainSize = Integer.parseInt(string);
        setPlainSize(plainSize);
    }
    
    private void parseChecksumType(String string) {
        parseChecksumAlgorithm(string);
        parseChecksumUsedSize(string);
    }
    
    private void parseChecksumAlgorithm(String string) {
        String checksumAlgorithm = getDigestAlgorithm(string);
        setChecksumAlgorithm(checksumAlgorithm);
    }
    
    private void parseChecksumUsedSize(String string) {
        string = string.toLowerCase();
        if (!string.contains("1k")) throw new IllegalArgumentException();
        
        setChecksumUsedSize(DEFAULT_CHECKSUM_USED_SIZE);
    }
    
    private void parseChecksum(String string) {
        byte[] checksum = Base64.decodeChars(string, Base64Settings.ORIGINAL);
        setChecksum(checksum);
    }
    
    private void parseAlgorithm(String string) {
        string = string.toLowerCase();
        String algorithm;
        String transformation;
        
        // TODO: improve
        if (string.contains("blowfish")) {
            algorithm = "Blowfish";
            transformation = "Blowfish/CFB/NoPadding";
        } else if (string.contains("aes")) {
            algorithm = "AES";
            transformation = "AES/CBC/NoPadding";
        } else {
            throw new UnsupportedEncryptionException(
                    "cannot identify crypto algorithm: " + string);
        }
        
        setAlgorithm(algorithm);
        setTransformation(transformation);
    }
    
    private void parseInitialisationVector(String string) {
        byte[] initialisationVector = Base64.decodeChars(string,
                Base64Settings.ORIGINAL);
        setInitialisationVector(initialisationVector);
    }
    
    private void parseKeyDerivationFunction(String string) {
        if (!string.equalsIgnoreCase(KEY_DERIVATION_FUNCTION)) throw new IllegalArgumentException();
        setKeyDerivationFunction(KEY_DERIVATION_FUNCTION);
    }
    
    private void parseKeyDerivationKeySize(String string) {
        int keySize = Integer.parseInt(string);
        setKeyDerivationKeySize(keySize);
    }
    
    private void parseKeyDerivationIterationCount(String string) {
        int iterationCount = Integer.parseInt(string);
        setKeyDerivationIterationCount(iterationCount);
    }
    
    private void parseKeyDerivationSalt(String string) {
        byte[] salt = Base64.decodeChars(string, Base64Settings.ORIGINAL);
        setKeyDerivationSalt(salt);
    }
    
    private void parseStartKeyGeneration(String string) {
        String generation = getDigestAlgorithm(string);
        setStartKeyGeneration(generation);
    }
    
    private void parseStartKeySize(String string) {
        int keySize = Integer.parseInt(string);
        setStartKeySize(keySize);
    }
    
    private int plainSize = -1;
    private String checksumAlgorithm;
    private int checksumUsedSize;
    private byte[] checksum;
    private String algorithm;
    private String transformation;
    private byte[] initialisationVector;
    private String keyDerivationFunction;
    private int keyDerivationKeySize = DEFAULT_KEY_DERIVATION_KEY_SIZE;
    private int keyDerivationIterationCount = -1;
    private byte[] keyDerivationSalt;
    private String startKeyGeneration = DEFAULT_START_KEY_GENERATION;
    private int startKeySize;
    
    // TODO: improve
    public boolean isEmpty() {
        if (algorithm != null) return false;
        if (transformation != null) return false;
        
        return true;
    }
    
    public int getPlainSize() {
        return plainSize;
    }
    
    public void setPlainSize(int plainSize) {
        this.plainSize = plainSize;
    }
    
    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }
    
    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }
    
    public int getChecksumUsedSize() {
        return checksumUsedSize;
    }
    
    public void setChecksumUsedSize(int checksumUsedSize) {
        this.checksumUsedSize = checksumUsedSize;
    }
    
    public byte[] getChecksum() {
        return checksum;
    }
    
    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    
    public String getTransformation() {
        return transformation;
    }
    
    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }
    
    public byte[] getInitialisationVector() {
        return initialisationVector;
    }
    
    public void setInitialisationVector(byte[] initialisationVector) {
        this.initialisationVector = initialisationVector;
    }
    
    public String getKeyDerivationFunction() {
        return keyDerivationFunction;
    }
    
    public void setKeyDerivationFunction(String keyDerivationFunction) {
        this.keyDerivationFunction = keyDerivationFunction;
    }
    
    public int getKeyDerivationKeySize() {
        return keyDerivationKeySize;
    }
    
    public void setKeyDerivationKeySize(int keyDerivationKeySize) {
        this.keyDerivationKeySize = keyDerivationKeySize;
    }
    
    public int getKeyDerivationIterationCount() {
        return keyDerivationIterationCount;
    }
    
    public void setKeyDerivationIterationCount(int keyDerivationIterationCount) {
        this.keyDerivationIterationCount = keyDerivationIterationCount;
    }
    
    public byte[] getKeyDerivationSalt() {
        return keyDerivationSalt;
    }
    
    public void setKeyDerivationSalt(byte[] keyDerivationSalt) {
        this.keyDerivationSalt = keyDerivationSalt;
    }
    
    public String getStartKeyGeneration() {
        return startKeyGeneration;
    }
    
    public void setStartKeyGeneration(String startKeyGeneration) {
        this.startKeyGeneration = startKeyGeneration;
    }
    
    public int getStartKeySize() {
        return startKeySize;
    }
    
    public void setStartKeySize(int startKeySize) {
        this.startKeySize = startKeySize;
    }
    
}