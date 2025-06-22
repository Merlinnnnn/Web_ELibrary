package com.spkt.libraSys.service.document.PhysicalDocument;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "physical_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhysicalDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "physical_document_id")
    Long physicalDocumentId;

    @OneToOne
    @JoinColumn(name = "document_id", referencedColumnName = "document_id")
    DocumentEntity document;  // Liên kết với tài liệu chung

    @Column(name = "isbn", unique = true, nullable = false)
    String isbn;

    @Column(name = "quantity", nullable = false)
    int quantity;
    @Column(name = "price", nullable = false)
    double price;

    @Column(name = "borrowed_count", nullable = false)
    @Builder.Default
    int borrowedCount = 0;

    @Column(name = "unavailable_count", nullable = false)
    @Builder.Default
    int unavailableCount = 0;


    public int getAvailableCopies() {
        return Math.max(quantity - (borrowedCount + unavailableCount), 0);
    }
}

