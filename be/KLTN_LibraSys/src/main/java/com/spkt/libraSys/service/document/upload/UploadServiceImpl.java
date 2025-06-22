package com.spkt.libraSys.service.document.upload;

import com.cloudinary.utils.ObjectUtils;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.cloudinary.CloudinaryService;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.drm.DrmService;
import com.spkt.libraSys.util.EncryptionKeyEntity;
import com.spkt.libraSys.util.EncryptionKeyRepository;
import com.spkt.libraSys.util.FileEncryptionUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadServiceImpl implements UploadService {

    CloudinaryService cloudinaryService;
    UploadRepository uploadRepository;
    EncryptionKeyRepository encryptionKeyRepository;
    DrmService drmService;

    @NonFinal
    @Value("${upload.dir:uploads/documents/}")
    String UPLOAD_DIR;

    @Override
    public void uploadImage(MultipartFile file, DocumentEntity document) {
        try {
            String publicId = UUID.randomUUID().toString();
            Map options = ObjectUtils.asMap("folder", "document", "overwrite", true, "public_id", publicId);
            String coverImage = (String) cloudinaryService.uploadFile(file, options).get("secure_url");
            document.setCoverImage(coverImage);
            document.setImagePublicId(publicId);
        } catch (IOException e) {
            throw new AppException(ErrorCode.CLOUDINARY_UPLOAD_FAILED, "Error uploading image");
        }
    }

    @Override
    @Transactional
    public Set<UploadEntity> uploadFiles(List<MultipartFile> fileList) {
        Set<UploadEntity> result = new HashSet<>();
        for (MultipartFile file : fileList) {
            UploadEntity entity = UploadEntity.builder()
                    .fileName(file.getOriginalFilename())
                    .fileType(getFileExtension(file))
                    .uploadedAt(LocalDateTime.now())
                    .build();
            entity = uploadRepository.save(entity);
            entity = uploadAndEncryptFile(file, entity);
            result.add(entity);
        }
        return result;
    }

    @Override
    @Transactional
    public Set<UploadEntity> uploadFiles(List<MultipartFile> fileList, DigitalDocumentEntity digitalDocument) {
        Set<UploadEntity> uploadEntities = new HashSet<>();

        for (MultipartFile file : fileList) {
            UploadEntity uploadEntity = UploadEntity.builder()
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .digitalDocument(digitalDocument)
                    .build();

            uploadEntity = uploadRepository.save(uploadEntity);
            uploadEntity = uploadAndEncryptFile(file, uploadEntity);
            uploadEntities.add(uploadRepository.save(uploadEntity));
        }

        return uploadEntities;
    }

    private UploadEntity uploadAndEncryptFile(MultipartFile file, UploadEntity entity) {
        try {
            Path originalPath = Paths.get(UPLOAD_DIR, "original");
            Path drmPath = Paths.get(UPLOAD_DIR, "drm");
            Files.createDirectories(originalPath);
            Files.createDirectories(drmPath);

            String fileExt = getFileExtension(file);
            String originalFileName = UUID.randomUUID() + "." + fileExt;
            String drmFileName = UUID.randomUUID() + "." + fileExt + ".drm";

            // File tạm
            File tempFile = File.createTempFile("upload_", "." + fileExt);
            file.transferTo(tempFile);
            tempFile.deleteOnExit();

            // Lưu file gốc vào thư mục "original" mà không mã hóa
            File originalFile = originalPath.resolve(originalFileName).toFile();
            Files.copy(tempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Tạo file DRM
            byte[] drmContent = drmService.createDrmPackage(entity.getUploadId(), Files.readAllBytes(tempFile.toPath()));
            File drmFile = drmPath.resolve(drmFileName).toFile();
            Files.write(drmFile.toPath(), drmContent);

            // Cập nhật entity với đường dẫn file gốc và file DRM
            entity.setFilePath(drmFile.getPath());  // Đường dẫn của file DRM
            entity.setOriginalFilePath(originalFile.getPath());  // Đường dẫn của file gốc

            return entity;
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED, "Error uploading file: " + file.getOriginalFilename());
        }
    }

    @Override
    public void deleteFiles(Set<UploadEntity> uploads) {
        if (uploads == null || uploads.isEmpty()) return;
        for (UploadEntity upload : uploads) {
            try {
                Files.deleteIfExists(Paths.get(upload.getFilePath()));
                log.info("Deleted: {}", upload.getFilePath());
            } catch (IOException e) {
                log.error("Delete failed: {}", upload.getFilePath(), e);
            }
        }
    }

    public byte[] getDecryptedDocument(Long uploadId, String userId) throws Exception {
        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Upload not found"));

        EncryptionKeyEntity keyEntity = encryptionKeyRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Encryption key not found"));

        byte[] decryptedData = FileEncryptionUtil.decryptFile(new File(upload.getFilePath()), keyEntity.getEncryptionKey());

        if ("application/pdf".equals(upload.getFileType())) {
            String watermark = "User: " + userId + " | Time: " + LocalDateTime.now();
            decryptedData = processPdfSecurity(decryptedData, watermark);
        }

        return decryptedData;
    }

    private byte[] processPdfSecurity(byte[] pdfData, String watermarkText) throws Exception {
        return securePdf(addWatermark(pdfData, watermarkText));
    }

    private byte[] addWatermark(byte[] pdfData, String watermarkText) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfData)), new PdfWriter(baos));
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(i));
            canvas.beginText()
                    .setFontAndSize(PdfFontFactory.createFont(), 12)
                    .moveText(50, 50)
                    .showText(watermarkText)
                    .endText();
        }
        pdfDoc.close();
        return baos.toByteArray();
    }

    private byte[] securePdf(byte[] pdfData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfData));
        WriterProperties properties = new WriterProperties()
                .setStandardEncryption(null, null,
                        ~(EncryptionConstants.ALLOW_PRINTING | EncryptionConstants.ALLOW_COPY),
                        EncryptionConstants.STANDARD_ENCRYPTION_128);
        PdfDocument pdfDoc = new PdfDocument(reader, new PdfWriter(baos, properties));
        pdfDoc.close();
        return baos.toByteArray();
    }

    private String getFileExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * Lấy file URL hoặc stream tài liệu từ cơ sở dữ liệu.
     *
     * @param uploadId ID của file cần lấy.
     * @return Resource chứa nội dung file.
     */
    public FileStreamResponse getFileAsStream(Long uploadId) throws IOException {
        // Tìm UploadEntity theo uploadId
        Optional<UploadEntity> uploadEntityOpt = uploadRepository.findById(uploadId);
        if (!uploadEntityOpt.isPresent()) {
            throw new IOException("File không tồn tại.");
        }

        UploadEntity uploadEntity = uploadEntityOpt.get();
        String filePath = uploadEntity.getFilePath();

        // Tạo đường dẫn đầy đủ đến file trong hệ thống
        Path file = Paths.get(uploadEntity.getOriginalFilePath()).normalize();

        // Kiểm tra nếu file tồn tại và có thể đọc được
        if (!Files.exists(file)) {
            throw new IOException("Không thể tìm thấy file.");
        }

        // Lấy kiểu MIME của file
        String fileType = Files.probeContentType(file);  // Đoạn này lấy kiểu MIME từ hệ thống

        // Trả về file dưới dạng stream và kiểu MIME
        Resource resource = new UrlResource(file.toUri());

        // Tạo và trả về đối tượng chứa stream và fileType
        return new FileStreamResponse(resource, fileType);
    }
}
