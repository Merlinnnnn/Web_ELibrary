package com.spkt.libraSys.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES"; // Thuật toán AES
    private static final int KEY_LENGTH = 32; // Độ dài key AES 256-bit (32 bytes)

    // Mã hóa nội dung
    public byte[] encryptContent(byte[] rawData, String encryptionKey) {
        try {
            // Validate Key Length
            if (encryptionKey == null || encryptionKey.getBytes().length != KEY_LENGTH) {
                throw new IllegalArgumentException("Invalid encryption key length. Expected 32 bytes (256-bit).");
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(rawData);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa nội dung", e);
        }
    }

    // Giải mã nội dung
    public byte[] decryptContent(byte[] encryptedData, String encryptionKey) {
        try {
            // Validate Key Length
            if (encryptionKey == null || encryptionKey.getBytes().length != KEY_LENGTH) {
                throw new IllegalArgumentException("Invalid encryption key length. Expected 32 bytes (256-bit).");
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã nội dung", e);
        }
    }

    // Tạo khóa ngẫu nhiên (dành cho bài test)
    public String generateRandomKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256); // Độ dài key AES 256-bit
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo khóa mã hóa", e);
        }
    }
}