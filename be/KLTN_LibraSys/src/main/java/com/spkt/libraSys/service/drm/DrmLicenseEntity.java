package com.spkt.libraSys.service.drm;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "drm_licenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrmLicenseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "upload_id", nullable = false)
    Long uploadId;
    
    @Column(name = "user_id", nullable = false)
    String userId;
    
    @Column(name = "device_id", nullable = false)
    String deviceId;
    
    @Column(name = "issue_date", nullable = false)
    LocalDateTime issueDate;
    
    @Column(name = "expiry_date", nullable = false)
    LocalDateTime expiryDate;
    
    @Column(name = "encrypted_content_key", nullable = false, length = 1024)
    String encryptedContentKey;
    
    @Column(name = "is_revoked", nullable = false)
    boolean revoked = false;
}