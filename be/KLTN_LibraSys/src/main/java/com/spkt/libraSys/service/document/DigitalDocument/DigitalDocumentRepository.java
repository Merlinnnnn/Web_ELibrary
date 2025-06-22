package com.spkt.libraSys.service.document.DigitalDocument;

import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DigitalDocumentRepository extends JpaRepository<DigitalDocumentEntity,Long> {
    Page<DigitalDocumentEntity> findByUser(UserEntity user, Pageable pageable);

    Page<DigitalDocumentEntity> findAll(Specification<DigitalDocumentEntity> specification, Pageable sortedPageable);


    @Query(value = """
    SELECT d.*, ar.request_time FROM digital_documents d
    JOIN access_requests ar ON d.digital_document_id = ar.digital_id
    WHERE ar.requester_id = :requesterId
      AND ar.status = 'APPROVED'
      AND ar.license_expiry > NOW()
    ORDER BY ar.request_time DESC
    """,
            countQuery = """
    SELECT COUNT(*) FROM access_requests ar
    WHERE ar.requester_id = :requesterId
      AND ar.status = 'APPROVED'
      AND ar.license_expiry > NOW()
    """,
            nativeQuery = true)
    Page<DigitalDocumentEntity> findAccessibleDocumentsByUser(
            @Param("requesterId") String requesterId,
            Pageable pageable
    );




    @Query("""
    SELECT d FROM DigitalDocumentEntity d
    WHERE d.document.approvalStatus IN :statuses
    """)
    Page<DigitalDocumentEntity> findByApprovalStatuses(@Param("statuses") Collection<ApprovalStatus> statuses, Pageable pageable);


}
