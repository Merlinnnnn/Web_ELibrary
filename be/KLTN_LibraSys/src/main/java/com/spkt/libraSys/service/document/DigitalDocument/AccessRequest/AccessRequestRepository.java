package com.spkt.libraSys.service.document.DigitalDocument.AccessRequest;

import io.github.resilience4j.core.lang.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.user.UserEntity;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequestEntity, Long> {
    List<AccessRequestEntity> findByOwnerIdAndStatus(String ownerId, AccessRequestStatus status);

    List<AccessRequestEntity> findByDigitalIdAndRequesterId(Long digitalId, String requesterId);
    Optional<AccessRequestEntity> findByDigitalIdAndRequesterIdAndStatus(Long digitalId, String requesterId, AccessRequestStatus status);

    Page<AccessRequestEntity> findByOwnerId(String userId, Pageable pageable);

    Page<AccessRequestEntity> findByStatus(AccessRequestStatus status, Pageable pageable);

    Page<AccessRequestEntity> findByStatusAndOwnerId(AccessRequestStatus status, String ownerId, Pageable pageable);

    Page<AccessRequestEntity> findByRequesterId(String requesterId, Pageable pageable);

    Page<AccessRequestEntity> findByDigitalId(Long   digitalId, Pageable pageable);

    List<AccessRequestEntity> findByRequesterId(String requesterId);
}
