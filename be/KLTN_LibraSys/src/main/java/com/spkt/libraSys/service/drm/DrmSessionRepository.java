package com.spkt.libraSys.service.drm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrmSessionRepository extends JpaRepository<DrmSessionEntity, Long> {

    Optional<DrmSessionEntity> findBySessionToken(String sessionToken);

    @Modifying
    @Transactional
    @Query("UPDATE DrmSessionEntity s SET s.active = false WHERE s.licenseId IN (SELECT l.id FROM DrmLicenseEntity l WHERE l.uploadId = :documentId)")
    void deactivateSessionsByUploadId(Long documentId);

    // Thêm phương thức này để sửa lỗi
    List<DrmSessionEntity> findByLicenseId(Long licenseId);

    int countByLicenseIdAndActiveIsTrueAndLastHeartbeatAfter(Long licenseId, LocalDateTime timestamp);
    Optional<DrmSessionEntity> findByLicenseIdAndDeviceIdAndActiveIsTrue(Long licenseId, String deviceId);

    List<DrmSessionEntity> findByActive(boolean active);
}