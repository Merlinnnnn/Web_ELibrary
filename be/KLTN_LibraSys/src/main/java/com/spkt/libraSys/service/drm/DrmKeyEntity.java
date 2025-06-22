package com.spkt.libraSys.service.drm;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "drm_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrmKeyEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "upload_id", nullable = false)
    Long uploadId;
    
    @Column(name = "content_key", nullable = false, length = 1024)
    String contentKey;
    
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
    
    @Column(name = "is_active", nullable = false)
    boolean active = true;
}