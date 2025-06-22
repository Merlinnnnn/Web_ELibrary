package com.spkt.libraSys.service.document.DigitalDocument;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "digital_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DigitalDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "digital_document_id")
    Long digitalDocumentId;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = true)
    UserEntity user;

    @OneToOne
    @JoinColumn(name = "document_id", referencedColumnName = "document_id")
    DocumentEntity document;  
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", nullable = false)
    @Builder.Default
    VisibilityStatus visibilityStatus = VisibilityStatus.RESTRICTED_VIEW;

    @OneToMany(mappedBy = "digitalDocument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Set<UploadEntity> uploads = new HashSet<>();

    @Column(name = "upload_time")
    LocalDateTime uploadTime;

    @PrePersist
    void createAt(){
        uploadTime = LocalDateTime.now();
    }

}


