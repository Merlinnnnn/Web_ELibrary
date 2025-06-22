package com.spkt.libraSys.service.drm;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.drm.key.KeyExchangeRequest;
import com.spkt.libraSys.service.drm.key.KeyExchangeResponse;
import com.spkt.libraSys.service.drm.key.KeyPairManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drm")
@RequiredArgsConstructor
@Log4j2
public class DrmController {

    private final DrmService drmService;


    @PostMapping("/license")
    public ResponseEntity<ApiResponse<DrmLicense>> requestLicense(
            @Valid @RequestBody DrmLicenseRequest request) throws Exception {
        DrmLicense license = drmService.issueLicense(
                request.getUploadId(),
                request.getDeviceId(),
                request.getPublicKey()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<DrmLicense>builder()
                        .message("Cấp phép thành công")
                        .data(license)
                        .build());
    }
    @PostMapping("/license/android")
    public ResponseEntity<ApiResponse<DrmLicense>> requestLicenseForAndroid(
            @Valid @RequestBody DrmLicenseRequest request) throws Exception {
        DrmLicense license = drmService.issueLicenseForAndroid(
                request.getUploadId(),
                request.getDeviceId(),
                request.getPublicKey()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<DrmLicense>builder()
                        .message("Cấp phép thành công")
                        .data(license)
                        .build());
    }


    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> updateHeartbeat(
            @Valid @RequestBody Map<String, String> request) {

        try {
            String sessionToken = request.get("sessionToken");
            drmService.updateHeartbeat(sessionToken);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .message("Cập nhật trạng thái thành công")
                    .build());
        } catch (LicenseRevokedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<Void>builder()
                            .message("Giấy phép đã bị thu hồi")
                            .build());
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .message("Đã xảy ra lỗi hệ thống")
                            .build());
        }
    }

    @PostMapping("/revoke/{uploadId}")
    public ResponseEntity<ApiResponse<Void>> revokeAccess(
            @PathVariable Long uploadId) {
        drmService.revokeAccess(uploadId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã thu hồi quyền truy cập thành công")
                .build());
    }

    @PostMapping("/renew-key/{uploadId}")
    public ResponseEntity<ApiResponse<Void>> renewKey(@PathVariable Long uploadId) {
        try {
            drmService.createNewKeyAfterRevoke(uploadId);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .message("Đã tạo khóa mới và mã hóa lại nội dung thành công")
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi tạo khóa mới", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .message("Không thể tạo khóa mới: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/content/{uploadId}")
    public ResponseEntity<Resource> getDrmProtectedContent(@PathVariable Long uploadId) {
        Map<String, Object> result = drmService.getDrmProtectedContent(uploadId);
        String fileType = (String) result.get("fileType");
        Resource resource = (Resource) result.get("resource");

        String mimeType = switch (fileType.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "mp4" -> "video/mp4";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            // Thêm các loại MIME khác nếu cần
            default -> "application/octet-stream"; // Dự phòng cho các loại tệp không xác định
        }; // Mặc định nếu không tìm thấy kiểu MIME hợp lệ

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file." + fileType + "\"")
                .body(resource);
    }

    @GetMapping("/{digitalDocId}/uploads")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentUploadsWithKeys(
            @PathVariable Long digitalDocId) {
        
                try {
            Map<String, Object> result = drmService.getDocumentUploadsWithKeys(digitalDocId);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Lấy thông tin upload và key thành công")
                    .data(result)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin upload và key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Không thể lấy thông tin upload và key: " + e.getMessage())
                            .build());
        }
    }

}
