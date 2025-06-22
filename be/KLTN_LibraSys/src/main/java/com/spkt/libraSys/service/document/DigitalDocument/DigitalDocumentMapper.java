package com.spkt.libraSys.service.document.DigitalDocument;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DigitalDocumentMapper {


   
    @Mapping(target = "documentId", ignore = true) 
    @Mapping(target = "publishedDate", ignore = true) 
    @Mapping(target = "status", constant = "ENABLED") 
    @Mapping(target = "documentCategory", constant = "DIGITAL") 
    @Mapping(target = "documentTypes", ignore = true) 
    @Mapping(target = "courses", ignore = true) 
    @Mapping(target = "imagePublicId", ignore = true)
    @Mapping(target = "coverImage", ignore = true) 
    DocumentEntity toDocumentEntity(DigitalDocumentRequestDto requestDto);

    // Mapping từ DigitalDocumentEntity sang DigitalDocumentResponseDto
    @Mapping(source = "digitalDocumentId", target = "digitalDocumentId")
    @Mapping(source = "document.documentName", target = "documentName")
    @Mapping(source = "document.author", target = "author")
    @Mapping(source = "document.publisher", target = "publisher")
    @Mapping(source = "document.description", target = "description")
    @Mapping(source = "document.coverImage", target = "coverImage")
    @Mapping(source = "uploads", target = "uploads", qualifiedByName = "mapUploadsToUploadInfo")
    @Mapping(source = "visibilityStatus", target = "visibilityStatus")
    @Mapping(source ="document.approvalStatus",target="approvalStatus")
    DigitalDocumentResponseDto toResponseDto(DigitalDocumentEntity entity);

    // Mapping từ Set<UploadEntity> sang List<UploadInfo>
    @Named("mapUploadsToUploadInfo")
    default List<DigitalDocumentResponseDto.UploadInfo> mapUploadsToUploadInfo(Set<UploadEntity> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            return null;
        }
        return uploads.stream()
                .map(this::toUploadInfo)
                .collect(Collectors.toList());
    }

    // Mapping từ UploadEntity sang UploadInfo
    @Mapping(source = "uploadId", target = "uploadId")
    @Mapping(source = "fileName", target = "fileName")
    @Mapping(source = "fileType", target = "fileType")
    @Mapping(source = "filePath", target = "filePath")
    @Mapping(source = "uploadedAt", target = "uploadedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    DigitalDocumentResponseDto.UploadInfo toUploadInfo(UploadEntity uploadEntity);
}