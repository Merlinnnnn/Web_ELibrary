package com.spkt.libraSys.service.document.upload;

import com.cloudinary.utils.ObjectUtils;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.service.cloudinary.CloudinaryService;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.drm.DrmService;
import com.spkt.libraSys.util.EncryptionKeyEntity;
import com.spkt.libraSys.util.EncryptionKeyRepository;
import com.spkt.libraSys.util.FileEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceImplTest {

    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private UploadRepository uploadRepository;
    @Mock
    private EncryptionKeyRepository encryptionKeyRepository;
    @Mock
    private DrmService drmService;

    @InjectMocks
    private UploadServiceImpl uploadService;

    private DocumentEntity testDocument;
    private DigitalDocumentEntity testDigitalDocument;
    private UploadEntity testUpload;
    private MockMultipartFile testFile;
    private EncryptionKeyEntity testKey;
    private Path testUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        // Setup test document
        testDocument = DocumentEntity.builder()
                .documentId(1L)
                .documentName("Test Document")
                .build();

        // Setup test digital document
        testDigitalDocument = DigitalDocumentEntity.builder()
                .digitalDocumentId(1L)
                .build();

        // Create test upload directory
        testUploadDir = Paths.get("test-uploads");
        Files.createDirectories(testUploadDir);

        // Setup test upload entity with paths in test directory
        String testFilePath = testUploadDir.resolve("test.pdf").toString();
        String testOriginalPath = testUploadDir.resolve("original.pdf").toString();
        testUpload = UploadEntity.builder()
                .uploadId(1L)
                .fileName("test.pdf")
                .fileType("pdf")
                .filePath(testFilePath)
                .originalFilePath(testOriginalPath)
                .uploadedAt(LocalDateTime.now())
                .build();

        // Setup test file
        testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // Setup test encryption key
        testKey = new EncryptionKeyEntity();
        testKey.setUploadId(1L);
        testKey.setEncryptionKey("test-key");

        // Set upload directory for testing
        ReflectionTestUtils.setField(uploadService, "UPLOAD_DIR", testUploadDir.toString() + "/");
    }

    @Test
    void uploadImage_Success() throws IOException {
        // Arrange
        Map<String, Object> cloudinaryResponse = new HashMap<>();
        cloudinaryResponse.put("secure_url", "https://cloudinary.com/test.jpg");
        when(cloudinaryService.uploadFile(any(MultipartFile.class), any(Map.class)))
                .thenReturn(cloudinaryResponse);

        // Act
        uploadService.uploadImage(testFile, testDocument);

        // Assert
        assertNotNull(testDocument.getCoverImage());
        assertNotNull(testDocument.getImagePublicId());
        verify(cloudinaryService).uploadFile(any(MultipartFile.class), any(Map.class));
    }

    @Test
    void uploadImage_Failure() throws IOException {
        // Arrange
        when(cloudinaryService.uploadFile(any(MultipartFile.class), any(Map.class)))
                .thenThrow(new IOException("Upload failed"));

        // Act & Assert
        assertThrows(AppException.class, () -> uploadService.uploadImage(testFile, testDocument));
    }

    @Test
    void uploadFiles_Success() throws IOException {
        // Arrange
        List<MultipartFile> files = Collections.singletonList(testFile);
        when(uploadRepository.save(any(UploadEntity.class))).thenReturn(testUpload);
        when(drmService.createDrmPackage(anyLong(), any(byte[].class)))
                .thenReturn("encrypted content".getBytes());

        // Act
        Set<UploadEntity> result = uploadService.uploadFiles(files);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(uploadRepository, times(1)).save(any(UploadEntity.class));
    }

    @Test
    void uploadFiles_WithDigitalDocument_Success() throws IOException {
        // Arrange
        List<MultipartFile> files = Collections.singletonList(testFile);
        when(uploadRepository.save(any(UploadEntity.class))).thenReturn(testUpload);
        when(drmService.createDrmPackage(anyLong(), any(byte[].class)))
                .thenReturn("encrypted content".getBytes());

        // Act
        Set<UploadEntity> result = uploadService.uploadFiles(files, testDigitalDocument);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(uploadRepository, times(2)).save(any(UploadEntity.class));
    }

    @Test
    void deleteFiles_Success() throws IOException {
        // Arrange
        Set<UploadEntity> uploads = Collections.singleton(testUpload);
        Path testPath = Paths.get(testUpload.getFilePath());
        Files.createDirectories(testPath.getParent());
        Files.write(testPath, "test content".getBytes());

        // Act
        uploadService.deleteFiles(uploads);

        // Assert
        assertFalse(Files.exists(testPath));
    }

    @Test
    void getDecryptedDocument_Success() throws Exception {
        // Arrange
        when(uploadRepository.findById(1L)).thenReturn(Optional.of(testUpload));
        when(encryptionKeyRepository.findByUploadId(1L)).thenReturn(Optional.of(testKey));

        // Create and encrypt the test file
        Path testFilePath = Paths.get(testUpload.getFilePath());
        Files.createDirectories(testFilePath.getParent());
        
        // Generate salt and IV
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(salt);
        new SecureRandom().nextBytes(iv);
        
        // Generate a proper encryption key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        String base64Key = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        
        // Derive key using PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(base64Key.toCharArray(), salt, 65536, 256);
        SecretKey derivedKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        
        // Encrypt the test content
        byte[] testContent = "test content".getBytes();
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey, parameterSpec);
        
        byte[] encryptedContent = cipher.doFinal(testContent);
        
        // Combine salt, IV and encrypted content
        ByteBuffer buffer = ByteBuffer.allocate(salt.length + iv.length + encryptedContent.length);
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(encryptedContent);
        
        // Write the combined data to file
        Files.write(testFilePath, buffer.array());
        
        // Update the test key with the actual encryption key
        testKey.setEncryptionKey(base64Key);

        // Act
        byte[] result = uploadService.getDecryptedDocument(1L, "test-user");

        // Assert
        assertNotNull(result);
        assertArrayEquals(testContent, result);
        
        // Cleanup
        Files.deleteIfExists(testFilePath);
    }

    @Test
    void getDecryptedDocument_UploadNotFound() {
        // Arrange
        when(uploadRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> uploadService.getDecryptedDocument(1L, "test-user"));
    }

    @Test
    void getDecryptedDocument_KeyNotFound() {
        // Arrange
        when(uploadRepository.findById(1L)).thenReturn(Optional.of(testUpload));
        when(encryptionKeyRepository.findByUploadId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> uploadService.getDecryptedDocument(1L, "test-user"));
    }

    @Test
    void getFileAsStream_Success() throws IOException {
        // Arrange
        when(uploadRepository.findById(1L)).thenReturn(Optional.of(testUpload));
        Path testPath = Paths.get(testUpload.getOriginalFilePath());
        Files.createDirectories(testPath.getParent());
        Files.write(testPath, "test content".getBytes());

        // Act
        FileStreamResponse response = uploadService.getFileAsStream(1L);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getFileType());
        
        // Cleanup
        Files.deleteIfExists(testPath);
    }

    @Test
    void getFileAsStream_FileNotFound() {
        // Arrange
        when(uploadRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IOException.class, () -> uploadService.getFileAsStream(1L));
    }
} 