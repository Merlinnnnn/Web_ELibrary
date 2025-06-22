package com.spkt.libraSys.service.drm;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "drm_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrmSessionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "license_id", nullable = false)
    Long licenseId;
    
    @Column(name = "session_token", nullable = false)
    String sessionToken;

    @Column(name = "device_id", nullable = true)
    String deviceId;
    @Column(name = "start_time", nullable = false)
    LocalDateTime startTime;
    
    @Column(name = "last_heartbeat", nullable = false)
    LocalDateTime lastHeartbeat;
    
    @Column(name = "is_active", nullable = false)
    boolean active = true;
}