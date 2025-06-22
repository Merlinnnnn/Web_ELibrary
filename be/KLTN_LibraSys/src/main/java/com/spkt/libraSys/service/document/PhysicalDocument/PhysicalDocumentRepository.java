package com.spkt.libraSys.service.document.PhysicalDocument;

import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicalDocumentRepository extends JpaRepository<PhysicalDocumentEntity, Long> {
    // Có thể thêm các phương thức tìm kiếm tùy chỉnh nếu cần.
    boolean existsByIsbn(String isbn);
}
