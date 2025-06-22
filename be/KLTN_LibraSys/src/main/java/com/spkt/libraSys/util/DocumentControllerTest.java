package com.spkt.libraSys.util;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRepository;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.upload.UploadRepository;
import com.spkt.libraSys.service.document.upload.UploadService;
import com.spkt.libraSys.service.drm.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Log4j2
public class DocumentControllerTest {

    private final UploadService uploadService;
    private final DigitalDocumentRepository digitalDocumentRepository;
    private final UploadRepository uploadRepository;

    private final DrmService drmService;
    private final DrmEncryptionUtil drmEncryptionUtil;
    private final EncryptionKeyRepository encryptionKeyRepository;
    private final DrmKeyRepository drmKeyRepository;

    @GetMapping("/{uploadId}/content")
    public ResponseEntity<Resource> getDocumentContent(
            @PathVariable Long uploadId,
            @RequestHeader("Device-ID") String deviceId,
            Authentication authentication) {

        try {
//            String userId = authentication.getName();
//
//            // 1. Xin cấp license DRM trước khi đọc nội dung
//            DrmLicense license = drmService.issueLicense(uploadId, userId, deviceId);
//            if (license == null) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//            }

            // 2. Lấy thông tin upload
            UploadEntity upload = uploadRepository.findById(uploadId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Document not found"));

            // 3. Đọc file DRM đã được mã hóa trực tiếp
            File drmFile = new File(upload.getFilePath());
            if (!drmFile.exists()) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "DRM file not found");
            }

            // 4. Cấu hình response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename(upload.getFileName())
                            .build());

            // 5. Thêm DRM license vào header để client sử dụng
//            headers.add("X-DRM-License-Id", license.getLicenseId().toString());
//            headers.add("X-DRM-Session-Token", license.getSessionToken());

            // 6. Trả về nội dung file DRM

//            byte[] decryptedData =  Files.readAllBytes(drmFile.toPath());
//            byte[] res =  drmEncryptionUtil.decryptContent(decryptedData, contentKey);
//            System.out.println("res" +  res);

            ByteArrayResource resource = new ByteArrayResource(
                    Files.readAllBytes(drmFile.toPath())
            );
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(drmFile.length())
                    .body(resource);

        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error retrieving document content");
        }
    }

    @GetMapping("/stream/{uploadId}")
    public ResponseEntity<StreamingResponseBody> streamDocument(
            @PathVariable Long uploadId,
            Authentication authentication) {
        // Kiểm tra quyền truy cập
        String userId = authentication.getName();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt for digitalDocumentId");
            throw new AppException(ErrorCode.UNAUTHORIZED, "Authentication required");
        }
        System.out.println(authentication.getName());


//        boolean hasAccess = digitalDocumentRepository.findById(digitalDocumentId)
//            .map(doc -> {
//                if (doc.getVisibilityStatus() == VisibilityStatus.PUBLIC) {
//                    return true;
//                }
//                return doc.getUser() != null && doc.getUser().getUserId().equals(userId);
//            })
//            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Document not found"));
//
//        if (!hasAccess) {
//            return ResponseEntity.status(403).build();
//        }

        // Stream tài liệu
        StreamingResponseBody stream = outputStream -> {
            try {
                byte[] decryptedData = uploadService.getDecryptedDocument(uploadId, userId);
                outputStream.write(decryptedData);
            } catch (Exception e) {
                throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Error streaming document");
            }
        };

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF) // Hoặc động dựa trên fileType
            .body(stream);
    }
    //method lay all tai lieu da giai ma bang contentKey
    @GetMapping("/{uploadId}/decrypted")
    public ResponseEntity<Resource> getDecryptedFile (
            @PathVariable Long uploadId,
            Authentication authentication){

        try {
            UploadEntity upload = uploadRepository.findById(uploadId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                            "File not found"));
            DrmKeyEntity drmKeyEntity = drmKeyRepository.findByUploadIdAndActive(uploadId,true)
                    .orElseThrow(()->new AppException(ErrorCode.RESOURCE_NOT_FOUND,"File not found"));

            byte[] encryptedData = Files.readAllBytes(new File(upload.getFilePath()).toPath());

            String contentKey  = drmEncryptionUtil.decryptKey(drmKeyEntity.getContentKey());
            System.out.println("encryptedData" + encryptedData.length);
            System.out.println("contentKey" + contentKey);
          //  byte[] encryptedContent = drmService.extractDrmContent(encryptedData);
            //thuc hien tach header cua encrytedData

            byte[] decryptedData = drmEncryptionUtil.decryptContent(encryptedData, contentKey);
            System.out.println("decryptedData" + decryptedData.length);

            ByteArrayResource resource = new ByteArrayResource(decryptedData);


            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(upload.getFileType())) // Đặt đúng mimeType nếu có
                    .contentLength(decryptedData.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + upload.getFileName() + "\"")
                    .body(resource);


        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error decrypting file content");
        }
    }

    /**
     * API: Stream một content chunk đã mã hóa (encrypted chunk) tới client
     *
     * Endpoint: GET /api/documents/{uploadId}/content-chunk?offset=...&length=...
     *
     *   - Trả về 1 đoạn dữ liệu đã mã hóa của file số hóa (mã hóa bằng contentKey)
     *   - Client sẽ giải mã bằng contentKey ở phía client
     *   - offset, length: tính theo byte
     *   - Có thể gửi kèm IV cho từng chunk nếu thuật toán cần
     */
    @GetMapping("/{uploadId}/content-chunk")
    public ResponseEntity<Resource> streamEncryptedChunk(
            @PathVariable Long uploadId,
            @RequestParam("offset") long offset,
            @RequestParam("length") int length,
            Authentication authentication
    ) throws IOException {


        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));

        // --- GIẢI MÃ chunk bằng contentKey ---
        DrmKeyEntity drmKeyEntity = drmKeyRepository.findByUploadIdAndActive(uploadId,true)
                .orElseThrow(()->new AppException(ErrorCode.RESOURCE_NOT_FOUND,"File not found"));
        String contentKey  = drmEncryptionUtil.decryptKey(drmKeyEntity.getContentKey());
        byte[] encryptedData = Files.readAllBytes(new File(upload.getFilePath()).toPath());
        byte[] decrypted = drmEncryptionUtil.decryptContent(encryptedData, contentKey);



        System.out.println("decryptedData" + decrypted.length);


        File drmFile = new File(upload.getFilePath());
        if (!drmFile.exists()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "DRM file not found");
        }
        if (offset < 0 || length <= 0 || offset + length > drmFile.length()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Invalid range");
        }

        try (var raf = new java.io.RandomAccessFile(drmFile, "r")) {
            raf.seek(offset);
            byte[] buffer = new byte[length];
            int read = raf.read(buffer, 0, length);

            // Cần tạo mảng đúng số bytes đã đọc
            byte[] toSend;
            if (read == length) {
                toSend = buffer;
            } else if (read > 0) {
                toSend = java.util.Arrays.copyOf(buffer, read);
            } else {
                toSend = new byte[0];
            }
            byte[] sliced;
            if (decrypted.length >= offset + length) {
                sliced = Arrays.copyOfRange(decrypted, (int) offset, (int)offset + length);
                // Bây giờ sliced chứa 65555 byte đầu tiên của decrypted
            } else {
                throw new IllegalArgumentException("Mảng không đủ dữ liệu để cắt");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF); // Trả về PDF chunk
            headers.set("Content-Range", String.format("bytes %d-%d/%d", offset, offset + read - 1, drmFile.length()));
            headers.setContentLength(sliced.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(upload.getFileType())) // Đặt đúng mimeType nếu có
                    .contentLength(sliced.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + upload.getFileName() + "\"")
                    .body(new ByteArrayResource(sliced));


        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error streaming chunk");
        }
    }
}