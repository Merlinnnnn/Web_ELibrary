package com.spkt.libraSys.service.drm.key;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.drm.DrmEncryptionUtil;
import com.spkt.libraSys.service.drm.DrmKeyEntity;
import com.spkt.libraSys.service.drm.DrmKeyRepository;
import com.spkt.libraSys.service.drm.key.PublicKeyResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/drm")
public class DrmKeyController {

    private final DrmKeyService drmKeyService;
    private final DrmEncryptionUtil drmEncryptionUtil;
    private final DrmKeyRepository drmKeyRepository;

    @Autowired
    public DrmKeyController(DrmKeyService drmKeyService, DrmEncryptionUtil drmEncryptionUtil, DrmKeyRepository drmKeyRepository) {
        this.drmKeyService = drmKeyService;
        this.drmEncryptionUtil = drmEncryptionUtil;
        this.drmKeyRepository = drmKeyRepository;
    }

    @GetMapping("/publickey")
    public ResponseEntity<ApiResponse<PublicKeyResponseDTO>> getPublicKey() {
        // Lấy thông tin public key từ service
        PublicKeyResponseDTO keyData = drmKeyService.getCurrentPublicKey();

        // Tạo đối tượng ApiResponse để trả về
        ApiResponse<PublicKeyResponseDTO> response = ApiResponse.<PublicKeyResponseDTO>builder()
                .code(1000)
                .success(true)
                .message("Public key retrieved successfully")
                .data(keyData)
                .build();

        return ResponseEntity.ok(response);
    }
    @GetMapping("/privatekey")
    public ResponseEntity<ApiResponse<String>> getPrivateKey() {
        // Lấy thông tin public key từ service
        String keyData = drmKeyService.getPrivatekey();

        // Tạo đối tượng ApiResponse để trả về
        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(1000)
                .success(true)
                .message("Private key retrieved successfully")
                .data(keyData)
                .build();

        return ResponseEntity.ok(response);
    }
    @PostMapping("/decrypted")
    public ResponseEntity<ApiResponse<String>> getDecryptedPrivateKey(@RequestBody Map<String, String> body) throws Exception {
        // Lấy thông tin public key từ service
        String encryptedContentKey = body.get("encryptedContentKey");
        String keyData = drmKeyService.getDecrypted(encryptedContentKey);

        // Tạo đối tượng ApiResponse để trả về
        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(1000)
                .success(true)
                .message("Private key retrieved successfully")
                .data(keyData)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/contentkey/{uploadId}")
    public ResponseEntity<ApiResponse<String>> getContentKey(@PathVariable Long uploadId) throws Exception {
        // Lấy thông tin public key từ service

        DrmKeyEntity drmKeyEntity = drmKeyRepository.findByUploadIdAndActive(uploadId,true).
                orElseThrow(()->new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        String keyData = drmEncryptionUtil.decryptKey(drmKeyEntity.getContentKey());


        // Tạo đối tượng ApiResponse để trả về
        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(1000)
                .success(true)
                .message("Content key retrieved successfully")
                .data(keyData)
                .build();

        return ResponseEntity.ok(response);
    }


}