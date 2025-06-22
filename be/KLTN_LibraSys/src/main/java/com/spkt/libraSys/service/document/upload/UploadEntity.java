package com.spkt.libraSys.service.document.upload;

import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "uploads")
public class UploadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_id")
    Long uploadId;

    @Column(name = "file_name", nullable = false)
    String fileName;

    @Column(name = "file_type", nullable = false)
    String fileType;  // pdf, mp4, mp3, jpg,...

    @Column(name = "file_path", nullable = true)
    String filePath;

    @Column(name = "original_file_path", nullable = true)
    String originalFilePath;

    @Column(name="upload_at")
    LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "digital_document_id", referencedColumnName = "digital_document_id")
    DigitalDocumentEntity digitalDocument;

}
