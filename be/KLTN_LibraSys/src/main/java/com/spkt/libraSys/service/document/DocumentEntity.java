package com.spkt.libraSys.service.document;

import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    Long documentId;

    @Column(name = "document_name", nullable = false)
    String documentName;

    @Column(name = "author", nullable = false)
    String author;

    @Column(name = "publisher")
    String publisher;

    @Column(name = "language")
    String language;

    @Column(name = "published_date")
    LocalDate publishedDate;

    @Column(name = "description", length = 1000)
    String description;
    @Column(name = "summary", length = 1500)
    String summary;

    @Column(name = "cover_image")
    String coverImage;

    @Column(name = "image_public_id")
    String imagePublicId;


    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    @Builder.Default
    ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    @Column(name = "reaseon_approval", nullable = true)
    String reason_approval;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    DocumentStatus status = DocumentStatus.ENABLED;

    // Liên kết với các loại tài liệu (DocumentType)
    @ManyToMany
    @JoinTable(
            name = "document_document_types",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "document_type_id")
    )
    Set<DocumentTypeEntity> documentTypes;
    @ManyToMany
    @JoinTable(
            name = "course_documents",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Builder.Default
    Set<CourseEntity> courses = new HashSet<>();

    @Column(name = "document_category")  // Thêm trường này để phân biệt loại tài liệu
    @Enumerated(EnumType.STRING)
    DocumentCategory documentCategory; // Enum chứa các giá trị: PHYSICAL, DIGITAL, BOTH

    @OneToOne(mappedBy = "document")
    PhysicalDocumentEntity physicalDocument;

    @OneToOne(mappedBy = "document")
    DigitalDocumentEntity digitalDocument;



}






