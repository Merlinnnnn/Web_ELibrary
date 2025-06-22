package com.spkt.libraSys.service.drm.key;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Slf4j
public class KeyPairManagementService {

    @Value("${drm.keystore.path:config/drm_keystore.p12}")
    private String keyStorePath;

    @Value("${drm.keystore.password:librasys}")
    private String keyStorePassword;

    @Value("${drm.key.alias:drm-server-key}")
    private String keyAlias;

    @Value("${drm.key.rotation.days:90}")
    private int keyRotationDays;

    @Value("${drm.keys.directory:config/keys}")
    private String keysDirectory;

    private PrivateKey currentPrivateKey;
    private PublicKey currentPublicKey;
    private LocalDateTime keyCreationDate;

    //@PostConstruct
    public void initialize() {
        try {
            // Đảm bảo thư mục tồn tại
            Path keysDirPath = Paths.get(keysDirectory);
            if (!Files.exists(keysDirPath)) {
                Files.createDirectories(keysDirPath);
            }

            Path privateKeyPath = Paths.get(keysDirectory, keyAlias + ".private");
            Path publicKeyPath = Paths.get(keysDirectory, keyAlias + ".public");

            // Kiểm tra xem các file key đã tồn tại chưa
            if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
                log.info("Existing key pair found. Loading keys...");
                loadKeyPair(privateKeyPath, publicKeyPath);
            } else {
                log.info("No existing key pair found. Generating new RSA key pair...");
                generateAndSaveKeyPair(privateKeyPath, publicKeyPath);
            }

            log.info("KeyPairManagementService initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize KeyPairManagementService", e);
            throw new RuntimeException("Failed to initialize key management", e);
        }
    }

    /**
     * Loads an existing key pair from files
     */
    private void loadKeyPair(Path privateKeyPath, Path publicKeyPath) throws Exception {
        // Đọc private key
        byte[] privateKeyBytes = Files.readAllBytes(privateKeyPath);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        currentPrivateKey = keyFactory.generatePrivate(privateKeySpec);

        // Đọc public key
        byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        currentPublicKey = keyFactory.generatePublic(publicKeySpec);

        // Đọc thời gian tạo key từ file metadata nếu có
        Path metadataPath = Paths.get(keysDirectory, keyAlias + ".metadata");
        if (Files.exists(metadataPath)) {
            String metadata = new String(Files.readAllBytes(metadataPath));
            keyCreationDate = LocalDateTime.parse(metadata.trim());
        } else {
            // Nếu không có metadata, đặt thời gian tạo là hiện tại
            keyCreationDate = LocalDateTime.now();
            saveKeyMetadata(metadataPath);
        }

        log.info("Loaded existing key pair successfully from: {}", keysDirectory);
    }

    /**
     * Generates a new RSA key pair and saves it to files
     */
    private void generateAndSaveKeyPair(Path privateKeyPath, Path publicKeyPath) throws Exception {
        // Tạo cặp khóa mới
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        currentPrivateKey = keyPair.getPrivate();
        currentPublicKey = keyPair.getPublic();

        String privateKeyBase64 = Base64.getEncoder().encodeToString(currentPrivateKey.getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(currentPublicKey.getEncoded());

        System.out.println("==== PUBLIC KEY (Base64 PEM) ====");
        System.out.println("-----BEGIN PUBLIC KEY-----");
        System.out.println(publicKeyBase64);
        System.out.println("-----END PUBLIC KEY-----");

        System.out.println("==== PRIVATE KEY (Base64 PEM) ====");
        System.out.println("-----BEGIN PRIVATE KEY-----");
        System.out.println(privateKeyBase64);
        System.out.println("-----END PRIVATE KEY-----");


        keyCreationDate = LocalDateTime.now();

        // Lưu private key
        try (FileOutputStream fos = new FileOutputStream(privateKeyPath.toFile())) {
            fos.write(currentPrivateKey.getEncoded());
        }

        // Lưu public key
        try (FileOutputStream fos = new FileOutputStream(publicKeyPath.toFile())) {
            fos.write(currentPublicKey.getEncoded());
        }

        // Lưu metadata (thời gian tạo key)
        Path metadataPath = Paths.get(keysDirectory, keyAlias + ".metadata");
        saveKeyMetadata(metadataPath);

        log.info("Generated and saved new RSA key pair to: {}", keysDirectory);
    }

    /**
     * Saves key metadata (creation date)
     */
    private void saveKeyMetadata(Path metadataPath) throws IOException {
        try (FileWriter writer = new FileWriter(metadataPath.toFile())) {
            writer.write(keyCreationDate.toString());
        }
    }

    /**
     * Checks if the key pair needs rotation based on creation date
     */
    private boolean shouldRotateKey() {
        LocalDateTime rotationDue = keyCreationDate.plusDays(keyRotationDays);
        return LocalDateTime.now().isAfter(rotationDue);
    }

    /**
     * Scheduled task to check and rotate keys if needed
     */
    public void checkAndRotateKeyIfNeeded() {
        try {
            if (shouldRotateKey()) {
                log.info("Key rotation needed. Generating new key pair...");
                Path privateKeyPath = Paths.get(keysDirectory, keyAlias + ".private");
                Path publicKeyPath = Paths.get(keysDirectory, keyAlias + ".public");

                // Backup existing keys with timestamp
                String timestamp = String.valueOf(System.currentTimeMillis());
                Path backupPrivateKeyPath = Paths.get(keysDirectory, keyAlias + "." + timestamp + ".private.bak");
                Path backupPublicKeyPath = Paths.get(keysDirectory, keyAlias + "." + timestamp + ".public.bak");

                Files.copy(privateKeyPath, backupPrivateKeyPath);
                Files.copy(publicKeyPath, backupPublicKeyPath);

                // Generate new keys
                generateAndSaveKeyPair(privateKeyPath, publicKeyPath);
                log.info("Key rotation completed successfully");
            }
        } catch (Exception e) {
            log.error("Failed to rotate key pair", e);
        }
    }

    /**
     * Returns the current private key
     */
    public PrivateKey getCurrentPrivateKey() {
        return currentPrivateKey;
    }

    /**
     * Returns the current public key
     */
    public PublicKey getCurrentPublicKey() {
        return currentPublicKey;
    }

    /**
     * Returns the key creation date
     */
    public LocalDateTime getKeyCreationDate() {
        return keyCreationDate;
    }

    /**
     * Returns the current public key encoded as Base64 string
     */
    public String getEncodedPublicKey() {
        if (currentPublicKey != null) {
            return Base64.getEncoder().encodeToString(currentPublicKey.getEncoded());
        }
        return null;
    }
    public String getEncodedPrivateKey() {
        if (currentPublicKey != null) {
            return Base64.getEncoder().encodeToString(currentPrivateKey.getEncoded());
        }
        return null;
    }

    /**
     * Encrypts data using the current public key
     */
    public byte[] encryptWithPublicKey(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, currentPublicKey);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts data using the current private key
     */
    public byte[] decryptWithPrivateKey(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, currentPrivateKey);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Signs data with the current private key
     */
    public byte[] signData(byte[] data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(currentPrivateKey);
        signature.update(data);
        return signature.sign();
    }

    /**
     * Verifies a signature using the current public key
     */
    public boolean verifySignature(byte[] data, byte[] signatureBytes) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(currentPublicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
}