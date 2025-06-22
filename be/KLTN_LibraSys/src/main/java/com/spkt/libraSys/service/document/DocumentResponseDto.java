package com.spkt.libraSys.service.document;

import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentResponseDto;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalRequestDto;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalResponseDto;
import com.spkt.libraSys.service.document.course.CourseResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {
    private Long documentId;
    private String isbn;
    private String documentName;
    private String author;
    private String publisher;
    private LocalDate publishedDate;
    private String language;
    private double price;
    private int quantity;
    private String description;
    private String coverImage;
    private String documentCategory;
    private String summary;
    private String approvalStatus;
    private Set<DocumentTypeEntity> documentTypes;
    private Set<CourseResponse> courses;
    private PhysicalResponseDto physicalDocument;
    private DigitalDocumentResponseDto digitalDocument;
   
}
