package com.spkt.libraSys.service.drm.key;

import com.spkt.libraSys.service.drm.DrmEncryptionUtil;
import com.spkt.libraSys.service.drm.key.PublicKeyResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class DrmKeyService {

    private final KeyPairManagementService keyPairManagementService;
    private final DrmEncryptionUtil drmEncryptionUtil;

    @Value("${drm.key.alias:drm-server-key}")
    private String keyAlias;
    
    @Value("${drm.key.rotation.days:90}")
    private int keyRotationDays;

    @Autowired
    public DrmKeyService(KeyPairManagementService keyPairManagementService, DrmEncryptionUtil drmEncryptionUtil) {
        this.keyPairManagementService = keyPairManagementService;
        this.drmEncryptionUtil = drmEncryptionUtil;
    }

    /**
     * Lấy thông tin public key hiện tại của hệ thống DRM
     * @return Đối tượng PublicKeyResponseDTO chứa thông tin public key
     */
    public PublicKeyResponseDTO getCurrentPublicKey() {
        String publicKeyBase64 = keyPairManagementService.getEncodedPublicKey();
        LocalDateTime creationDate = keyPairManagementService.getKeyCreationDate();
        LocalDateTime expirationDate = creationDate.plusDays(keyRotationDays);
        
        return new PublicKeyResponseDTO(
                publicKeyBase64,
                keyAlias,  // Sử dụng keyAlias làm keyId
                creationDate,
                expirationDate,
                "RSA",
                2048  // KeyPairManagementService sử dụng RSA 2048 bit
        );
    }
    public String getPrivatekey() {
        return keyPairManagementService.getEncodedPublicKey();
    }
    public String getDecrypted(String decryptedData) throws Exception {
        byte[] encryptedClientKeyBytes = Base64.getDecoder().decode(decryptedData);
        byte[] contentKey=  drmEncryptionUtil.decryptWithPrivateKey(encryptedClientKeyBytes);

        return  Base64.getEncoder().encodeToString(contentKey);
    }
}