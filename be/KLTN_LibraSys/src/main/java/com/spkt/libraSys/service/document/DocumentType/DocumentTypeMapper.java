package com.spkt.libraSys.service.document.DocumentType;



import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentTypeMapper {
    DocumentTypeEntity toDocumentType(DocumentTypeRequestDto request);
    DocumentTypeResponse toDocumentTypeResponse(DocumentTypeEntity documentType);
}