package com.spkt.libraSys.service.drm.key;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KeyExchangeResponse {

    private String sessionToken;
    private String encryptedContentKey;
    private String contentUrl;
    private List<String> rights;
    private LocalDateTime expiryTime;
}
