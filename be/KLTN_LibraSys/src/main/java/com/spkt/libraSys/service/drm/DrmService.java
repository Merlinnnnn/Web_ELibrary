package com.spkt.libraSys.service.drm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestEntity;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestRepository;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestStatus;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DigitalDocument.VisibilityStatus;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.upload.UploadRepository;
import com.spkt.libraSys.service.drm.key.KeyExchangeResponse;
import com.spkt.libraSys.service.user.UserEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRepository;

@Service
@RequiredArgsConstructor
@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrmService {
    
    DrmKeyRepository drmKeyRepository;
    DrmSessionRepository drmSessionRepository;
    DrmLicenseRepository drmLicenseRepository;
    DrmEncryptionUtil encryptionUtil;
    DrmCallbackService callbackService;
    private final DrmEncryptionUtil drmEncryptionUtil;
    private final AccessRequestRepository accessRequestRepository;
    private final UploadRepository uploadRepository;
    private final AuthService authService;
    private final DigitalDocumentRepository digitalDocumentRepository;

    private static final int MAX_ACTIVE_DEVICES = 2;
    private final UserDeviceLogRepository userDeviceLogRepository;

    /**
     * Tạo gói DRM cho tài liệu
     * @param uploadId ID của tài liệu
     * @param rawContent Nội dung gốc của tài liệu
     * @return Nội dung được bảo vệ DRM
     */
    public byte[] createDrmPackage(Long uploadId, byte[] rawContent) {
        // 1. Tạo khóa mã hóa cho tài liệu
        String contentKey = generateRandomKey();
        
        // 2. Lưu khóa vào cơ sở dữ liệu
        DrmKeyEntity keyEntity = new DrmKeyEntity();
        keyEntity.setUploadId(uploadId);
        keyEntity.setContentKey(encryptionUtil.encryptKey(contentKey));
        keyEntity.setCreatedAt(LocalDateTime.now());
        keyEntity.setActive(true);
        drmKeyRepository.save(keyEntity);
        String fk =  encryptionUtil.encryptKey(contentKey);
        String ck = encryptionUtil.decryptKey(fk);
        System.out.println("contentKey"+contentKey);
        System.out.println("ck"+ck);
        System.out.println("fk"+fk);
        // 3. Mã hóa nội dung với khóa này
        byte[] encryptedContent = encryptionUtil.encryptContent(rawContent, contentKey);
        //test decryted
        byte[] decryptedContent = encryptionUtil.decryptContent(encryptedContent, contentKey);
        System.out.println(contentKey);
        System.out.println("decryptedContent"+decryptedContent.length);
        // 4. Tạo header DRM chứa metadata
        DrmHeader header = new DrmHeader();
        header.setUploadId(uploadId);
        header.setTimestamp(LocalDateTime.now());
        header.setLicenseServerUrl("/api/drm/license");
        
        // 5. Đóng gói header và nội dung đã mã hóa
        //return packagingDrmContent(header, encryptedContent);
        return encryptedContent;
    }



    /**
     * Tạo mới hoặc cập nhật phiên DRM
     *
     * @param licenseId ID của license
     * @param deviceId ID của thiết bị
     * @return Thông tin phiên được tạo/cập nhật
     */
    private DrmSessionEntity createOrUpdateSession(Long licenseId, String deviceId) {
        try {
            Optional<DrmSessionEntity> existingSession = drmSessionRepository
                    .findByLicenseIdAndDeviceIdAndActiveIsTrue(licenseId, deviceId);

            DrmSessionEntity session;
            if (existingSession.isPresent()) {
                // Cập nhật phiên hiện có
                session = existingSession.get();
                session.setLastHeartbeat(LocalDateTime.now());
            } else {
                // Tạo phiên mới
                session = new DrmSessionEntity();
                session.setLicenseId(licenseId);
                //session.setDeviceId(deviceId);
                session.setSessionToken(UUID.randomUUID().toString());
                session.setStartTime(LocalDateTime.now());
                session.setLastHeartbeat(LocalDateTime.now());
                session.setActive(true);
            }

            return drmSessionRepository.save(session);
        } catch (Exception e) {
            log.error("Lỗi khi tạo/cập nhật phiên", e);
            throw new AppException(ErrorCode.SESSION_CREATION_ERROR,
                    "Không thể tạo phiên DRM cho thiết bị này");
        }
    }


    /**
     * Kiểm tra quyền truy cập và thời gian hết hạn của AccessRequest
     * @param uploadId ID tài liệu
     * @param userId ID người dùng yêu cầu
     * @return Trạng thái quyền truy cập của người dùng
     */
    private boolean hasValidAccess(Long uploadId, String userId) throws Exception {
        //VisibilityStatus = PUBLIC -> true
        UploadEntity uploadEntity = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Document not found"));
        DigitalDocumentEntity digitalDocumentEntity = uploadEntity.getDigitalDocument();
        if(digitalDocumentEntity.getVisibilityStatus().equals(VisibilityStatus.PUBLIC))
            return true;
        // Tìm yêu cầu truy cập của người dùng đối với tài liệu
        AccessRequestEntity request = accessRequestRepository
                .findByDigitalIdAndRequesterIdAndStatus(digitalDocumentEntity.getDigitalDocumentId(), userId, AccessRequestStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED, "You have not been granted access to this document"));

        // Kiểm tra thời gian hết hạn của giấy phép truy cập
        return request.getLicenseExpiry() == null || !request.getLicenseExpiry().isBefore(LocalDateTime.now());// Người dùng có quyền truy cập hợp lệ
    }

    /**
     * Cấp giấy phép DRM cho người dùng
     * @param uploadId ID tài liệu
     * @param deviceId ID thiết bị
     * @param publicKey Khóa công khai của thiết bị
     * @return Giấy phép DRM
     */
    public DrmLicense issueLicense(Long uploadId, String deviceId, String publicKey) throws Exception {
        return issueLicenseInternal(uploadId, deviceId, publicKey, false);
    }

    /**
     * Cấp giấy phép DRM cho Android
     * @param uploadId ID tài liệu
     * @param deviceId ID thiết bị Android
     * @param publicKey Khóa công khai Android
     * @return Giấy phép DRM
     */
    public DrmLicense issueLicenseForAndroid(Long uploadId, String deviceId, String publicKey) throws Exception {
        return issueLicenseInternal(uploadId, deviceId, publicKey, true);
    }

    /**
     * Hàm nội bộ thực hiện cấp giấy phép DRM
     */
    private DrmLicense issueLicenseInternal(Long uploadId, String deviceId, String publicKey, boolean isAndroid) throws Exception {
        String userId = authService.getCurrentUser().getUserId();

        if (!hasValidAccess(uploadId, userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "User has no access rights to this document");
        }

        validateAndLogDevice(userId, deviceId);

        DrmKeyEntity keyEntity = drmKeyRepository.findByUploadIdAndActive(uploadId, true)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "DRM key not found or revoked"));

        String decryptedContentKey = encryptionUtil.decryptKey(keyEntity.getContentKey());
        String deviceEncryptedKey = isAndroid
                ? encryptionUtil.encryptContentKeyForAndroid(decryptedContentKey, publicKey)
                : encryptionUtil.encryptForPublicKey(decryptedContentKey, publicKey);

        DrmLicenseEntity licenseEntity = DrmLicenseEntity.builder()
                .uploadId(uploadId)
                .userId(userId)
                .deviceId(deviceId)
                .issueDate(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusHours(24))
                .revoked(false)
                .encryptedContentKey(deviceEncryptedKey)
                .build();

        licenseEntity = drmLicenseRepository.save(licenseEntity);

        DrmSessionEntity session = DrmSessionEntity.builder()
                .licenseId(licenseEntity.getId())
                .startTime(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .active(true)
                .sessionToken(UUID.randomUUID().toString())
                .build();

        drmSessionRepository.save(session);

        return DrmLicense.builder()
                .licenseId(licenseEntity.getId())
                .sessionToken(session.getSessionToken())
                .expiryDate(licenseEntity.getExpiryDate())
                .encryptedContentKey(licenseEntity.getEncryptedContentKey())
                .build();
    }

    /**
     * Thu hồi quyền truy cập tài liệu
     * @param uploadId ID tài liệu
     */
    public void revokeAccess(Long uploadId) {
        // 1. Đánh dấu tất cả license của tài liệu là đã thu hồi
        drmLicenseRepository.revokeAllLicensesByUploadId(uploadId);
        
        // 2. Vô hiệu hóa khóa hiện tại
        drmKeyRepository.deactivateKeysByUploadId(uploadId);
        
        // 3. Đánh dấu tất cả session đang hoạt động là không hợp lệ
        drmSessionRepository.deactivateSessionsByUploadId(uploadId);
        
        // 4. Gửi thông báo đến các client đang mở tài liệu
        callbackService.notifyRevocation(uploadId);
        
        log.info("Revoked access to document: {}", uploadId);
    }
    
    /**
     * Cập nhật heartbeat từ client để biết tài liệu còn đang mở
     * @param sessionToken Token phiên
     */
    public void updateHeartbeat(String sessionToken) {
        DrmSessionEntity session = drmSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() ->   new AppException(ErrorCode.RESOURCE_NOT_FOUND,"Session not found"));
        
        // Kiểm tra xem license có bị thu hồi không
        DrmLicenseEntity license = drmLicenseRepository.findById(session.getLicenseId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,"License not found"));
        
        if (license.isRevoked()) {
            session.setActive(false);
            drmSessionRepository.save(session);
            throw new AppException(ErrorCode.FORBIDDEN,"License has been revoked");
        }
        
        // Cập nhật thời gian heartbeat
        session.setLastHeartbeat(LocalDateTime.now());
        drmSessionRepository.save(session);
    }
    
    // Các phương thức phụ trợ
    private String generateRandomKey() {
        return UUID.randomUUID().toString();
    }
    
    private boolean verifyAccessRights(Long documentId, String userId) {
        // Thực hiện kiểm tra quyền truy cập từ DB
        return true; // Giả định
    }
    
    private byte[] packagingDrmContent(DrmHeader header, byte[] encryptedContent) {
        // Logic đóng gói header và nội dung
        // Ví dụ: Chuyển header thành JSON, ghép với encryptedContent
        byte[] headerBytes = serializeHeader(header);
        byte[] result = new byte[4 + headerBytes.length + encryptedContent.length];
        
        // Ghi độ dài header (4 bytes)
        int headerLength = headerBytes.length;
        result[0] = (byte) (headerLength >> 24);
        result[1] = (byte) (headerLength >> 16);
        result[2] = (byte) (headerLength >> 8);
        result[3] = (byte) headerLength;
        
        // Sao chép header
        System.arraycopy(headerBytes, 0, result, 4, headerBytes.length);
        
        // Sao chép nội dung đã mã hóa
        System.arraycopy(encryptedContent, 0, result, 4 + headerBytes.length, encryptedContent.length);
//        byte[] res =  extractDrmContent(result);
//        //so sanh res == encryptedContent
//        System.out.println(res.length + "======== " + encryptedContent.length);
        return result;
    }
    
    private byte[] serializeHeader(DrmHeader header) {
        // Chuyển đổi header thành JSON hoặc format phù hợp
        // Ví dụ sử dụng Jackson, Gson, v.v.
        return header.toString().getBytes();
    }
    /**
     * Extracts the DRM header and encrypted content from a packaged DRM file
     *
     * @param packagedData The raw data from the DRM-protected file
     * @return A pair containing the extracted DRM header and the encrypted content
     */
    public byte[] extractDrmContent(byte[] packagedData) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(packagedData);

            // 1. Đọc độ dài header (4 bytes đầu)
            int headerLength = ((buffer.get() & 0xFF) << 24)
                    | ((buffer.get() & 0xFF) << 16)
                    | ((buffer.get() & 0xFF) << 8)
                    | (buffer.get() & 0xFF);

            // 2. Đọc phần header
            byte[] headerBytes = new byte[headerLength];
            buffer.get(headerBytes);

            // 3. Đọc phần nội dung mã hóa
            byte[] encryptedContent = new byte[buffer.remaining()];
            buffer.get(encryptedContent);


            // 4. Parse header (JSON → DrmHeader)
            ObjectMapper mapper = new ObjectMapper();
            DrmHeader header = mapper.readValue(headerBytes, DrmHeader.class);



            // 6. Giải mã nội dung bằng contentKey
            return encryptedContent;

        } catch (Exception e) {
            throw new RuntimeException("Unpack DRM content failed", e);
        }
    }

    /**
     * Trả về tài liệu DRM đã mã hóa dưới dạng Resource cho một tài liệu.
     *
     * @param uploadId ID tài liệu
     * @return Tài liệu đã mã hóa dưới dạng Map với thông tin tài liệu và Resource
     */
    public Map<String, Object> getDrmProtectedContent(Long uploadId) {
        // 1. Lấy thông tin người dùng hiện tại
        UserEntity currentUser = authService.getCurrentUser();
        String userId = currentUser.getUserId();

        // 2. Kiểm tra quyền truy cập tài liệu
        boolean hasAccess = false;
        try {
            hasAccess = hasValidAccess(uploadId, userId);
        } catch (Exception e) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Không thể kiểm tra quyền truy cập");
        }

        if (!hasAccess) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Người dùng không có quyền truy cập tài liệu này");
        }

        // 3. Tìm tài liệu
        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tài liệu không tồn tại"));

        String fileType = upload.getFileType();
        Path path = Paths.get(upload.getFilePath());

        try {
            // Kiểm tra tài liệu có tồn tại và có thể đọc được không
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Trả về thông tin tài liệu và Resource
                return Map.of(
                        "fileType", fileType,
                        "resource", resource
                );
            } else {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không thể truy cập tài liệu");
            }
        } catch (IOException e) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài liệu đã mã hóa");
        }
    }

    /**
     * Tạo key mới cho tài liệu sau khi bị revoke
     * @param uploadId ID của tài liệu
     */
    public void createNewKeyAfterRevoke(Long uploadId) {
        // 1. Kiểm tra xem tài liệu có bị revoke không
        Optional<DrmKeyEntity> existingKey = drmKeyRepository.findByUploadIdAndActive(uploadId, true);
        if (existingKey.isPresent()) {
            throw new AppException(ErrorCode.ACCESS_DENIED, 
                "Không thể tạo khóa mới: Tài liệu chưa bị thu hồi. Vui lòng thu hồi quyền truy cập trước.");
        }

        // 2. Lấy thông tin tài liệu
        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài liệu"));
        
        // 3. Đọc nội dung gốc từ file đã mã hóa
        Path originalFilePath = Paths.get(upload.getOriginalFilePath());
        byte[] rawContent;
        try {
            rawContent = Files.readAllBytes(originalFilePath);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể đọc nội dung tài liệu");
        }
        
        // 4. Tạo khóa mã hóa mới cho tài liệu
        String newContentKey = generateRandomKey();
        
        // 5. Lưu khóa mới vào cơ sở dữ liệu
        DrmKeyEntity keyEntity = new DrmKeyEntity();
        keyEntity.setUploadId(uploadId);
        keyEntity.setContentKey(encryptionUtil.encryptKey(newContentKey));
        keyEntity.setCreatedAt(LocalDateTime.now());
        keyEntity.setActive(true);
        drmKeyRepository.save(keyEntity);
        
        // 6. Mã hóa nội dung với khóa mới
        byte[] encryptedContent = encryptionUtil.encryptContent(rawContent, newContentKey);
        
        // 7. Tạo file DRM mới
        Path drmPath = Paths.get("uploads/documents/drm");
        try {
            Files.createDirectories(drmPath);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể tạo thư mục DRM");
        }
        String drmFileName = UUID.randomUUID() + "." + upload.getFileType() + ".drm";
        Path drmFilePath = drmPath.resolve(drmFileName);
        
        try {
            // Lưu file DRM mới
            Files.write(drmFilePath, encryptedContent);
            
            // Xóa file DRM cũ nếu tồn tại
            Path oldDrmPath = Paths.get(upload.getFilePath());
            if (Files.exists(oldDrmPath)) {
                Files.deleteIfExists(oldDrmPath);
            }
            
            // Cập nhật thông tin trong UploadEntity
            upload.setFilePath(drmFilePath.toString());
            uploadRepository.save(upload);
            
        } catch (IOException e) {
            // Nếu có lỗi, xóa key mới đã tạo
            drmKeyRepository.delete(keyEntity);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể tạo file DRM mới");
        }
    }

    public Map<String, Object> getDocumentUploadsWithKeys(Long digitalDocId) {
        // 1. Tìm digital document
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalDocId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Digital document not found"));

        // 2. Lấy danh sách upload
        Set<UploadEntity> uploads = digitalDocument.getUploads();
        if (uploads.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No uploads found for this document");
        }

        // 3. Lấy key cho từng upload (nếu có)
        List<Map<String, Object>> uploadsWithKeys = uploads.stream()
                .map(upload -> {
                    DrmKeyEntity key = drmKeyRepository.findByUploadIdAndActive(upload.getUploadId(), true)
                            .orElse(null);

                    Map<String, Object> uploadMap = new HashMap<>();
                    uploadMap.put("uploadId", upload.getUploadId());
                    uploadMap.put("fileName", upload.getFileName());
                    uploadMap.put("fileType", upload.getFileType());
                    uploadMap.put("filePath", upload.getFilePath());
                    uploadMap.put("uploadedAt", upload.getUploadedAt());

                    if (key != null) {
                        uploadMap.put("key", Map.of(
                                "id", key.getId(),
                                "contentKey", key.getContentKey(),
                                "createdAt", key.getCreatedAt(),
                                "active", key.isActive()
                        ));
                    } else {
                        uploadMap.put("key", null); // Không ném lỗi, chỉ gán null
                    }

                    return uploadMap;
                })
                .collect(Collectors.toList());

        // 4. Trả về kết quả
        return Map.of(
                "digitalDocumentId", digitalDocId,
                "documentName", digitalDocument.getDocument().getDocumentName(),
                "uploads", uploadsWithKeys
        );
    }

    private void validateAndLogDevice(String userId, String deviceId) {
//        List<UserDeviceLogEntity> devices = userDeviceLogRepository.findByUserId(userId);
//
//        boolean deviceExists = devices.stream()
//                .anyMatch(d -> d.getDeviceId().equals(deviceId));
//
//        if (!deviceExists) {
//            if (devices.size() >= MAX_ACTIVE_DEVICES) {
//                throw new AppException(ErrorCode.DEVICE_LIMIT_EXCEEDED, "Vượt quá số thiết bị được phép");
//            }
//
//            UserDeviceLogEntity log = new UserDeviceLogEntity();
//            log.setUserId(userId);
//            log.setDeviceId(deviceId);
//            log.setLastUsed(LocalDateTime.now());
//            userDeviceLogRepository.save(log);
//        } else {
//            devices.stream()
//                    .filter(d -> d.getDeviceId().equals(deviceId))
//                    .findFirst()
//                    .ifPresent(log -> {
//                        log.setLastUsed(LocalDateTime.now());
//                        userDeviceLogRepository.save(log);
//                    });
//        }
    }



}