package com.spkt.libraSys.service.drm;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Builder
@Data
public class DrmLicense {
    private Long licenseId;
    private String sessionToken;
    private LocalDateTime expiryDate;
    private String encryptedContentKey;
}