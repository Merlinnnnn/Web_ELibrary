package com.spkt.libraSys.service.document.PhysicalDocument;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PhysicalDocumentMapper {

    PhysicalDocumentMapper INSTANCE = Mappers.getMapper(PhysicalDocumentMapper.class);

    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "publishedDate", ignore = true)
    @Mapping(target = "status", constant = "ENABLED")
    @Mapping(target = "documentCategory", constant = "PHYSICAL")
    @Mapping(target = "documentTypes", ignore = true)
    @Mapping(target = "courses", ignore = true)
    @Mapping(target = "imagePublicId", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    DocumentEntity toDocumentEntity(PhysicalRequestDto requestDto);

    @Mapping(source = "physicalDocumentId", target = "physicalDocumentId")
    @Mapping(source = "document.documentName", target = "documentName")
    @Mapping(source = "document.author", target = "author")
    @Mapping(source = "document.publisher", target = "publisher")
    @Mapping(source = "document.description", target = "description")
    @Mapping(source = "document.coverImage", target = "coverImage")
    @Mapping(source = "isbn", target = "isbn")
    @Mapping(source = "quantity", target = "quantity")
    @Mapping(source = "borrowedCount", target = "borrowedCount")
    @Mapping(source = "unavailableCount", target = "unavailableCount")
    @Mapping(expression = "java(entity.getAvailableCopies())", target = "availableCopies")
    PhysicalResponseDto toResponseDto(PhysicalDocumentEntity entity);
}
