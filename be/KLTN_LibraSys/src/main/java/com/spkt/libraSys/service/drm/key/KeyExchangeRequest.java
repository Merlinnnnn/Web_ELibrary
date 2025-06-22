package com.spkt.libraSys.service.drm.key;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KeyExchangeRequest {
    
    @NotBlank(message = "Encrypted client key is required")
    private String encryptedClientKey;
    
    @NotNull(message = "Upload ID is required")
    private Long uploadId;  // Đã thay đổi từ contentId sang uploadId
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    

}