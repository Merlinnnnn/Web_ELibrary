package com.spkt.libraSys.util;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EncryptionKeyRepository extends JpaRepository<EncryptionKeyEntity, Long> {
    Optional<EncryptionKeyEntity> findByUploadId(Long uploadId);
}