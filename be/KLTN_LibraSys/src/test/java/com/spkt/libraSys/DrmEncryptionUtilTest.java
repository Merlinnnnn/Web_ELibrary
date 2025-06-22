package com.spkt.libraSys;

import com.spkt.libraSys.service.drm.DrmEncryptionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.drm.master-key=TestMasterKey123!@#" // Use a test master key
})
public class DrmEncryptionUtilTest {

    @Autowired
    private DrmEncryptionUtil drmEncryptionUtil;

    @Test
    public void testDecryptKey() {
        // 1. Prepare test data - encrypt a known content key
        String originalContentKey = "ThisIsATestContentKey12345";
        
        // 2. Encrypt the content key
        String encryptedKey = drmEncryptionUtil.encryptKey(originalContentKey);
        System.out.println("Encrypted key: " + encryptedKey);
        
        // 3. Ensure the encrypted key is in the expected format
//        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedKey);
//        assertEquals(
//            SALT_LENGTH + IV_LENGTH + (originalContentKey.getBytes().length + GCM_TAG_LENGTH/8),
//            encryptedBytes.length,
//            "Encrypted data length is not as expected"
//        );
//
//        // 4. Decrypt the key
//        String decryptedKey = drmEncryptionUtil.decryptKey(encryptedKey);
//
//        // 5. Verify the result
//        assertEquals(originalContentKey, decryptedKey, "Decrypted key does not match original key");
    }
    
    @Test
    public void testDecryptKeyWithInvalidData() {
        // 1. Test with random data
        String invalidData = Base64.getEncoder().encodeToString("invalid data".getBytes());
        Exception exception = assertThrows(RuntimeException.class, () -> {
            drmEncryptionUtil.decryptKey(invalidData);
        });
        assertTrue(exception.getMessage().contains("Error decrypting key"));
        
        // 2. Test with data that's too short
        byte[] tooShortData = new byte[10]; // Shorter than SALT_LENGTH + IV_LENGTH
        String tooShortEncrypted = Base64.getEncoder().encodeToString(tooShortData);
        exception = assertThrows(RuntimeException.class, () -> {
            drmEncryptionUtil.decryptKey(tooShortEncrypted);
        });
        assertTrue(exception.getMessage().contains("Error decrypting key"));
        
        // 3. Test with valid salt/IV but invalid encrypted data
        byte[] tamperedData = new byte[SALT_LENGTH + IV_LENGTH + 16]; // 16 is a minimal ciphertext length
        String tamperedEncrypted = Base64.getEncoder().encodeToString(tamperedData);
        exception = assertThrows(RuntimeException.class, () -> {
            drmEncryptionUtil.decryptKey(tamperedEncrypted);
        });
        assertTrue(exception.getMessage().contains("Error decrypting key"));
    }
    
    @Test
    public void testEncryptDecryptCycle() {
        // 1. Test encryption and decryption in a cycle with different inputs
        String[] testKeys = {
            "simple",
            "ComplexKey123!@#",
            "VeryLongContentKeyThatIsMoreThan32Characters",
            "ÜñìçøÐê Tèxt with špéçial çhäräçtérs",
            "" // Empty string as edge case
        };
        
        for (String testKey : testKeys) {
            String encrypted = drmEncryptionUtil.encryptKey(testKey);
            String decrypted = drmEncryptionUtil.decryptKey(encrypted);
            
            assertEquals(testKey, decrypted, 
                "Encryption-decryption cycle failed for key: " + testKey);
            
            // Verify the structure of the encrypted data
            byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
            assertTrue(encryptedBytes.length >= SALT_LENGTH + IV_LENGTH, 
                "Encrypted data too short for key: " + testKey);
        }
    }
    
    @Test
    public void testDecryptWithWrongMasterKey() throws Exception {
        // This test requires manually modifying the master key
        // We'll create a utility method to test with a different master key
        
        // 1. Encrypt with the current master key
        String originalKey = "TestDecryptionKey";
        String encryptedKey = drmEncryptionUtil.encryptKey(originalKey);
        
        // 2. Try decrypting with our test method that uses a different master key
        Exception exception = assertThrows(RuntimeException.class, () -> {
            decryptWithDifferentMasterKey(encryptedKey, "WrongMasterKey");
        });
        
        assertTrue(exception.getMessage().contains("Error decrypting key"));
    }
    
    /**
     * Helper method to decrypt with a specific master key
     */
    private String decryptWithDifferentMasterKey(String encryptedKey, String differentMasterKey) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptedKey);
            ByteBuffer buffer = ByteBuffer.wrap(decodedKey);
            
            byte[] salt = new byte[SALT_LENGTH];
            buffer.get(salt);
            
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            
            // Use the different master key here
            SecretKey key = deriveKeyForTest(differentMasterKey, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting key", e);
        }
    }
    
    /**
     * Helper method to derive a key for testing with a different master key
     */
    private SecretKey deriveKeyForTest(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    
    // Import the constants directly for the test
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int ITERATION_COUNT = 10000;
    private static final int KEY_LENGTH = 256;
}