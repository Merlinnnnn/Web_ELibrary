package com.spkt.libraSys.service.drm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spkt.libraSys.service.drm.key.KeyPairManagementService;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.*;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class DrmEncryptionUtil {


    private final KeyPairManagementService serverKeyPair;
    public DrmEncryptionUtil(KeyPairManagementService serverKeyPair) {
        this.serverKeyPair = serverKeyPair;
    }

    
    @Value("${app.drm.master-key}")
    private String masterKey;
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int ITERATION_COUNT = 10000;
    private static final int KEY_LENGTH = 256;
    
    /**
     * Mã hóa khóa nội dung với master key
     */
    public String encryptKey(String contentKey) {
        try {
            byte[] salt = generateRandomBytes(SALT_LENGTH);
            byte[] iv = generateRandomBytes(IV_LENGTH);
            
            SecretKey key = deriveKey(masterKey, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            
            byte[] encrypted = cipher.doFinal(contentKey.getBytes());
            
            ByteBuffer buffer = ByteBuffer.allocate(SALT_LENGTH + IV_LENGTH + encrypted.length);
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(encrypted);
            
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting key", e);
        }
    }
    
    /**
     * Giải mã khóa nội dung với master key
     */
    public String decryptKey(String encryptedKey) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptedKey);
            ByteBuffer buffer = ByteBuffer.wrap(decodedKey);
            
            byte[] salt = new byte[SALT_LENGTH];
            buffer.get(salt);
            
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            
            SecretKey key = deriveKey(masterKey, salt);
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
     * Mã hóa nội dung với khóa nội dung
     */
    public byte[] encryptContent(byte[] content, String contentKey) {
        try {
            byte[] salt = generateRandomBytes(SALT_LENGTH);
            byte[] iv = generateRandomBytes(IV_LENGTH);
            
            SecretKey key = deriveKey(contentKey, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            
            byte[] encrypted = cipher.doFinal(content);
            
            ByteBuffer buffer = ByteBuffer.allocate(SALT_LENGTH + IV_LENGTH + encrypted.length);
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(encrypted);
            
            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting content", e);
        }
    }
    
    /**
     * Giải mã nội dung với khóa nội dung
     */
    public byte[] decryptContent(byte[] encryptedContent, String contentKey) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedContent);
            
            byte[] salt = new byte[SALT_LENGTH];
            buffer.get(salt);
            
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            
            SecretKey key = deriveKey(contentKey, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting content", e);
        }
    }
    
    /**
     * Mã hóa khóa nội dung cho thiết bị cụ thể
     */
    public String encryptForDevice(String contentKey, String deviceId) {
        try {
            // Trong triển khai thực tế, khóa này sẽ được mã hóa bằng khóa công khai của thiết bị
            // Ở đây ta sử dụng deviceId làm salt để đơn giản hóa
            byte[] salt = deviceId.getBytes();
            byte[] iv = generateRandomBytes(IV_LENGTH);
            
            SecretKey key = deriveKey(masterKey, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            
            byte[] encrypted = cipher.doFinal(contentKey.getBytes());
            
            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting for device", e);
        }
    }

    /**
     * Mã hóa khóa nội dung cho thiết bị cụ thể
     */
    public String encryptForDevice(String contentKey, String deviceId,String publicKey) {
        try {
            // Trong triển khai thực tế, khóa này sẽ được mã hóa bằng khóa công khai của thiết bị
            // Ở đây ta sử dụng deviceId làm salt để đơn giản hóa
            byte[] salt = deviceId.getBytes();
            byte[] iv = generateRandomBytes(IV_LENGTH);

            SecretKey key = deriveKey(publicKey, salt);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] encrypted = cipher.doFinal(contentKey.getBytes());

            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting for device", e);
        }
    }

    public String encryptForPublicKey(String plaintext, String base64PublicKey) throws Exception {
        try {
            // Giải mã Base64 public key (X.509)
            byte[] decodedKey = Base64.getDecoder().decode(base64PublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Tạo Cipher với thuật toán OAEP + SHA-256
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);

            // Mã hóa dữ liệu
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Trả về chuỗi base64
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new Exception("Không thể mã hóa với public key: " + e.getMessage(), e);
        }
    }

    private PublicKey getPublicKeyFromPkcs1(String pem) throws Exception {
        // Bước 1: Đọc phần nội dung base64 từ PEM
        PemReader pemReader = new PemReader(new StringReader(pem));
        byte[] content = pemReader.readPemObject().getContent();
        pemReader.close();

        // Bước 2: Parse modulus và exponent từ DER sequence sử dụng BouncyCastle
        RSAPublicKey rsaPublicKey = RSAPublicKey.getInstance(content);
        BigInteger modulus = rsaPublicKey.getModulus();
        BigInteger exponent = rsaPublicKey.getPublicExponent();

        // Bước 3: Tạo PublicKey
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(keySpec);
    }

    public String encryptContentKeyForAndroid(String plaintext, String pemPublicKey) throws Exception {
        try{
            PublicKey publicKey = getPublicKeyFromPkcs1(pemPublicKey);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        }catch(Exception e) {
            throw new Exception("Không thể mã hóa với public key: " + e.getMessage(), e);
        }

    }


    public byte[] decryptWithPrivateKey(byte[] encryptedData) throws Exception {
        try {
            // Sử dụng RSA-OAEP với SHA-256 để khớp với mã hóa ở frontend
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

            cipher.init(Cipher.DECRYPT_MODE, serverKeyPair.getCurrentPrivateKey(), oaepParams);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            //log.error("Giải mã RSA thất bại", e);
            throw new Exception("Không thể giải mã khóa client: " + e.getMessage(), e);
        }

    }



    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    
    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }







}