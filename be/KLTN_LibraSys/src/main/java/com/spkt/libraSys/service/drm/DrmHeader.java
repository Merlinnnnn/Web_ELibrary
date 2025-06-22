package com.spkt.libraSys.service.drm;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DrmHeader {
    private Long uploadId;
    private LocalDateTime timestamp;
    private String licenseServerUrl;
    
    @Override
    public String toString() {
        return String.format("{\"uploadId\":%d,\"timestamp\":\"%s\",\"licenseServerUrl\":\"%s\"}",
                uploadId, timestamp, licenseServerUrl);
    }
}