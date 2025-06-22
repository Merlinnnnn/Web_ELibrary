package com.spkt.libraSys.service.document.DigitalDocument.AccessRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestResponseDto {
    private Long id;
    private String coverImage;
    private Long digitalId;
    private String requesterId;
    private String ownerId;
    private AccessRequestStatus status;
    private LocalDateTime requestTime;
    private LocalDateTime decisionTime;
    private String reviewerId;
    private LocalDateTime licenseExpiry;
    private String requesterName;
    private String ownerName;
}
