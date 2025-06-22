package com.spkt.libraSys.service.drm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrmLicenseRepository extends JpaRepository<DrmLicenseEntity, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE DrmLicenseEntity l SET l.revoked = true WHERE l.uploadId= :uploadId")
    void revokeAllLicensesByUploadId(Long uploadId);

    // Thêm phương thức này để sửa lỗi
    List<DrmLicenseEntity> findByUploadIdAndRevoked(Long uploadId, boolean revoked);

    Optional<DrmLicenseEntity> findByUserIdAndUploadIdAndRevokedIsFalse(String userId, Long uploadId);

    @Query("SELECT COUNT(d) FROM DrmLicenseEntity d WHERE d.revoked = false")
    long countActivelicenses();

    @Query("SELECT COUNT(d) FROM DrmLicenseEntity d WHERE d.revoked = true")
    long countRevokedLicenses();

    @Query("SELECT COUNT(d) FROM DrmLicenseEntity d WHERE d.issueDate > :date")
    long countRecentLicenses(@Param("date") LocalDateTime date);
}
