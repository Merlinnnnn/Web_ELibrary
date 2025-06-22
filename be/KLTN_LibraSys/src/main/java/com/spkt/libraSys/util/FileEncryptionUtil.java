package com.spkt.libraSys.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;

public class FileEncryptionUtil {
    private static final String AES_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes
    private static final int SALT_LENGTH = 16; // bytes
    private static final int KEY_LENGTH = 256; // bits
    private static final int ITERATION_COUNT = 65536;
    private static final int BUFFER_SIZE = 8192; // Tăng kích thước buffer để cải thiện hiệu suất

    /**
     * Mã hóa file sử dụng AES-GCM với IV và salt ngẫu nhiên
     */
    public static void encryptFile(File inputFile, File outputFile, String key) throws Exception {
        // Tạo salt và IV ngẫu nhiên
        byte[] salt = generateRandomBytes(SALT_LENGTH);
        byte[] iv = generateRandomBytes(IV_LENGTH);
        
        // Tạo khóa mã hóa từ key và salt
        SecretKey secretKey = deriveKey(key, salt);
        
        // Khởi tạo cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        
        // Ghi salt và IV vào đầu file đầu ra
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            out.write(salt);
            out.write(iv);
            
            // Mã hóa và ghi dữ liệu
            try (FileInputStream in = new FileInputStream(inputFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    byte[] outputBytes = cipher.update(buffer, 0, bytesRead);
                    if (outputBytes != null) {
                        out.write(outputBytes);
                    }
                }
                
                byte[] finalBytes = cipher.doFinal();
                if (finalBytes != null) {
                    out.write(finalBytes);
                }
            }
        }
    }

    /**
     * Giải mã file sử dụng AES-GCM và trả về mảng byte
     */
    public static byte[] decryptFile(File inputFile, String key) throws Exception {
        try (FileInputStream in = new FileInputStream(inputFile)) {
            // Đọc salt và IV từ đầu file
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            
            in.read(salt);
            in.read(iv);
            
            // Tạo khóa từ key và salt
            SecretKey secretKey = deriveKey(key, salt);
            
            // Khởi tạo cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            // Đọc và giải mã dữ liệu
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] outputBytes = cipher.update(buffer, 0, bytesRead);
                if (outputBytes != null) {
                    out.write(outputBytes);
                }
            }
            
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
                out.write(finalBytes);
            }
            
            return out.toByteArray();
        }
    }

    /**
     * Mã hóa mảng byte và trả về mảng byte đã mã hóa
     */
    public static byte[] encryptData(byte[] data, String key) throws Exception {
        // Tạo salt và IV ngẫu nhiên
        byte[] salt = generateRandomBytes(SALT_LENGTH);
        byte[] iv = generateRandomBytes(IV_LENGTH);
        
        // Tạo khóa mã hóa từ key và salt
        SecretKey secretKey = deriveKey(key, salt);
        
        // Khởi tạo cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        
        // Mã hóa dữ liệu
        byte[] encryptedData = cipher.doFinal(data);
        
        // Tạo output bao gồm salt + iv + encrypted data
        ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + encryptedData.length);
        byteBuffer.put(salt);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        
        return byteBuffer.array();
    }

    /**
     * Giải mã mảng byte và trả về dữ liệu gốc
     */
    public static byte[] decryptData(byte[] encryptedData, String key) throws Exception {
        // Tách salt, iv và dữ liệu đã mã hóa
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
        
        byte[] salt = new byte[SALT_LENGTH];
        byteBuffer.get(salt);
        
        byte[] iv = new byte[IV_LENGTH];
        byteBuffer.get(iv);
        
        byte[] encrypted = new byte[byteBuffer.remaining()];
        byteBuffer.get(encrypted);
        
        // Tạo khóa từ key và salt
        SecretKey secretKey = deriveKey(key, salt);
        
        // Khởi tạo cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        
        // Giải mã và trả về dữ liệu
        return cipher.doFinal(encrypted);
    }

    /**
     * Tạo khóa bảo mật ngẫu nhiên 256-bit và trả về base64 encoding
     */
    public static String generateKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[KEY_LENGTH / 8]; // 32 bytes cho AES-256
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
    
    /**
     * Dẫn xuất khóa từ mật khẩu và salt sử dụng PBKDF2
     */
    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
    }
    
    /**
     * Sinh ra mảng byte ngẫu nhiên với độ dài chỉ định
     */
    private static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}