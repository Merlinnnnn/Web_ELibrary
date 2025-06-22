package com.spkt.libraSys.service.drm;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_log", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "deviceId"}))
@Data
public class UserDeviceLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String deviceId;
    private LocalDateTime lastUsed;

    private LocalDateTime createdAt = LocalDateTime.now();
}
