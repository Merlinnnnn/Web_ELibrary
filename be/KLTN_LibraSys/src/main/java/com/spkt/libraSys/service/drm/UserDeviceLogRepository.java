package com.spkt.libraSys.service.drm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDeviceLogRepository extends JpaRepository<UserDeviceLogEntity, Long> {
    List<UserDeviceLogEntity> findByUserId(String userId);
    boolean existsByUserIdAndDeviceId(String userId, String deviceId);
}
