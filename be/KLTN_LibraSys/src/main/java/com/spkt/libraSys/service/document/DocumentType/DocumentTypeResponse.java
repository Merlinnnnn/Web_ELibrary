package com.spkt.libraSys.service.document.DocumentType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentTypeResponse {
    private Long documentTypeId;
    private String typeName;
}