package com.spkt.libraSys.service.drm;

import lombok.Data;

@Data
public class DrmLicenseRequest {
    private Long uploadId;
    private String deviceId;
    private String publicKey;
}