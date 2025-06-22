package com.spkt.libraSys.service.drm;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DrmNotification {
    private String action; // REVOKED, EXPIRED, etc.
    private Long documentId;
    private String sessionToken;
}