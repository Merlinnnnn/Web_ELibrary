package com.spkt.libraSys.service.drm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface DrmKeyRepository extends JpaRepository<DrmKeyEntity, Long> {
    
    Optional<DrmKeyEntity> findByUploadIdAndActive(Long documentId, boolean active);
    
    @Modifying
    @Transactional
    @Query("UPDATE DrmKeyEntity k SET k.active = false WHERE k.uploadId = :documentId")
    void deactivateKeysByUploadId(Long documentId);
}