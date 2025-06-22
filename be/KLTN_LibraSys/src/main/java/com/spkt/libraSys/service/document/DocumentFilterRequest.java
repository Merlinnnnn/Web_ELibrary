package com.spkt.libraSys.service.document;

import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

@Data
public class DocumentFilterRequest {
    private String documentName;
    private String author;
    private String publisher;
    private String language;
    private Set<Long> courseIds;
    private Set<Long> documentTypeIds;
    private DocumentCategory documentCategory;
    private DocumentStatus status;
    private ApprovalStatus approvalStatus;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishedDateFrom;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishedDateTo;
} 