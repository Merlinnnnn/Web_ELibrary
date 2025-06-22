package com.spkt.libraSys.service.document;


import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentMapper;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentMapper;
import com.spkt.libraSys.service.document.course.CourseMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses={CourseMapper.class, DigitalDocumentMapper.class, PhysicalDocumentMapper.class})
public interface DocumentMapper {

    @Mapping(target = "documentTypes", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    DocumentEntity toDocument(DocumentCreateRequest request);

    @Mapping(target = "documentTypes", source = "documentTypes")
    @Mapping(target = "courses", source = "courses")
    @Mapping(target = "isbn",expression = "java(getIsbn(document))")
    @Mapping(target = "documentCategory", source = "documentCategory")  // Ánh xạ documentCategory
    @Mapping(target = "physicalDocument", source = "physicalDocument")  // Ánh xạ PhysicalDocumentEntity
    @Mapping(target = "digitalDocument", source = "digitalDocument")
    @Mapping(source = "approvalStatus", target = "approvalStatus")
    @Mapping(target = "quantity",expression = "java(getQuantity(document))")
    @Mapping(target = "price",expression = "java(getPrice(document))")
    DocumentResponseDto toDocumentResponse(DocumentEntity document);

    default String getIsbn(DocumentEntity document) {
        if (document == null || document.getPhysicalDocument() == null) {
            return null;
        }
        return document.getPhysicalDocument().getIsbn();
    }
    default int getQuantity(DocumentEntity document) {
        if (document == null || document.getPhysicalDocument() == null) {
            return 0;
        }
        return document.getPhysicalDocument().getQuantity();
    }
    default double getPrice(DocumentEntity document) {
        if (document == null || document.getPhysicalDocument() == null) {
            return 0;
        }
        return document.getPhysicalDocument().getPrice();
    }
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    void updateDocument(@MappingTarget DocumentEntity document, DocumentUpdateRequest request);

    /**
     * Chuyển đổi từ danh sách Document entity sang danh sách DocumentResponse DTO.
     *
     * @param documents Danh sách các Document entity.
     * @return Danh sách các DTO DocumentResponse.
     */
//    List<DocumentResponse> toDocumentResponseList(List<Document> documents);
//
//    // Phương thức lấy tên loại tài liệu từ Set<DocumentType>
//    default Set<String> getDocumentTypeIds(Document document) {
//        return document.getDocumentTypes().stream()
//                .map(DocumentType::getTypeName)
//                .collect(Collectors.toSet());
//    }
}
