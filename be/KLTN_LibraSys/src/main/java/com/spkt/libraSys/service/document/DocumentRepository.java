package com.spkt.libraSys.service.document;

import com.spkt.libraSys.service.document.course.CourseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> , JpaSpecificationExecutor<DocumentEntity> {
   // boolean existsByIsbn(String isbn);
   // List<DocumentEntity> findByApprovalStatus(DocumentApprovalStatus approvalStatus);
   Page<DocumentEntity> findByCoursesIn(Collection<CourseEntity> courses, Pageable pageable);

   @Query("SELECT COUNT(d) FROM DocumentEntity d WHERE d.status = :status")
   long countByStatus(@Param("status") DocumentStatus status);

   List<DocumentEntity> findByDocumentNameContainingIgnoreCase(String documentName);

   @Query("SELECT DISTINCT d FROM DocumentEntity d LEFT JOIN FETCH d.documentTypes")
   List<DocumentEntity> findAllWithDocumentTypes();

   @Query("SELECT DISTINCT d FROM DocumentEntity d LEFT JOIN FETCH d.documentTypes WHERE d.documentId = :id")
   Optional<DocumentEntity> findByIdWithDocumentTypes(@Param("id") Long id);

   @Query("SELECT DISTINCT d FROM DocumentEntity d LEFT JOIN FETCH d.documentTypes WHERE d.documentName LIKE %:name%")
   List<DocumentEntity> findByDocumentNameContainingIgnoreCaseWithDocumentTypes(@Param("name") String documentName);

   @Query("SELECT DISTINCT d FROM DocumentEntity d LEFT JOIN FETCH d.documentTypes WHERE d IN :documents")
   List<DocumentEntity> findAllWithDocumentTypesByIds(@Param("documents") Collection<DocumentEntity> documents);
}
